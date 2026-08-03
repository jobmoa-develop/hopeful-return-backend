package com.jobmoa.hopefulreturn.sms;

import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Naver Cloud SENS SMS API v2 실연동 구현. sens.enabled=true 일 때 활성.
 * 공식 문서: https://api.ncloud-docs.com/docs/sens-sms-send (서명은 common-ncpapi 참조).
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "sens", name = "enabled", havingValue = "true")
public class SensSmsService implements SmsService {

    private static final String BASE_URL = "https://sens.apigw.ntruss.com";
    private static final int MAX_IMAGE_BYTES = 300 * 1024; // 300 KB
    private static final int MAX_IMAGE_WIDTH = 1500;
    private static final int MAX_IMAGE_HEIGHT = 1440;

    private final RestClient restClient = RestClient.create();
    private final String accessKey;
    private final String secretKey;
    private final String serviceId;
    private final String from;

    public SensSmsService(
            @Value("${sens.access-key}") String accessKey,
            @Value("${sens.secret-key}") String secretKey,
            @Value("${sens.service-id}") String serviceId,
            @Value("${sens.from}") String from) {
        // 값에 섞인 개행(CR)·공백은 서명/헤더를 깨뜨려 NCP 401 을 유발하므로 방어적으로 trim.
        this.accessKey = trim(accessKey);
        this.secretKey = trim(secretKey);
        this.serviceId = trim(serviceId);
        this.from = trim(from);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    @Override
    public void sendVerificationCode(String phoneNumber, String code) {
        String body = "[인증코드] " + code;
        send(new SmsSendCommand(
                "SMS", null, body,
                List.of(new SmsSendCommand.Recipient(phoneNumber, body)),
                null, null, null));
    }

    @Override
    public SmsSendResult send(SmsSendCommand command) {
        List<Map<String, String>> files = uploadImages(command);
        Map<String, Object> body = buildBody(command, files);
        String path = "/sms/v2/services/" + serviceId + "/messages";

        Map<String, Object> response = post(path, body);
        String statusCode = stringValue(response, "statusCode");
        String statusName = stringValue(response, "statusName");
        String requestId = stringValue(response, "requestId");
        if (!"202".equals(statusCode)) {
            log.warn("[SENS] unexpected send response: {}", response);
            throw new BusinessException(ErrorCode.SMS_SEND_FAILED);
        }
        List<String> fileIds = files.stream().map(file -> file.get("fileId")).toList();
        return new SmsSendResult(true, statusCode, statusName, requestId, fileIds);
    }

    @Override
    public void cancelReservation(String reserveId) {
        if (!StringUtils.hasText(reserveId)) {
            throw new BusinessException(ErrorCode.SMS_SEND_FAILED);
        }
        delete("/sms/v2/services/" + serviceId + "/reservations/" + reserveId);
    }

    private static final DateTimeFormatter COMPLETE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public List<SmsMessageResult> lookupResults(String requestId) {
        if (!StringUtils.hasText(requestId)) {
            return List.of();
        }
        try {
            // 1) requestId 로 수신 건별 messageId·to 목록 조회(발송 응답은 requestId 만 주므로 여기서 확보).
            String listPath = "/sms/v2/services/" + serviceId + "/messages?requestId="
                    + URLEncoder.encode(requestId, StandardCharsets.UTF_8);
            List<Map<String, Object>> messages = asMessages(get(listPath));
            List<SmsMessageResult> results = new ArrayList<>();
            for (Map<String, Object> m : messages) {
                String messageId = stringValue(m, "messageId");
                if (!StringUtils.hasText(messageId)) {
                    continue;
                }
                // 2) messageId 로 실제 전달 결과 조회.
                results.add(lookupOne(messageId, stringValue(m, "to")));
            }
            return results;
        } catch (RuntimeException e) {
            // 조회 실패는 예외 대신 빈 목록 — 폴러가 다음 주기에 재시도한다.
            log.warn("[SENS] result lookup failed: requestId={}", requestId, e);
            return List.of();
        }
    }

    private SmsMessageResult lookupOne(String messageId, String fallbackTo) {
        try {
            String path = "/sms/v2/services/" + serviceId + "/messages/" + messageId;
            List<Map<String, Object>> messages = asMessages(get(path));
            Map<String, Object> msg = messages.isEmpty() ? Map.of() : messages.get(0);
            String to = stringValue(msg, "to");
            String statusCode = stringValue(msg, "statusCode");
            String statusName = stringValue(msg, "statusName");
            String statusMessage = stringValue(msg, "statusMessage");
            return new SmsMessageResult(
                    messageId,
                    StringUtils.hasText(to) ? to : fallbackTo,
                    mapState(stringValue(msg, "status"), statusName),
                    statusCode,
                    StringUtils.hasText(statusMessage) ? statusMessage : statusName,
                    parseCompleteTime(stringValue(msg, "completeTime")));
        } catch (RuntimeException e) {
            log.warn("[SENS] message result lookup failed: messageId={}", messageId, e);
            return new SmsMessageResult(messageId, fallbackTo, SmsDeliveryState.PENDING, null, null, null);
        }
    }

    /** status COMPLETED 만 종료 상태. success/fail 로 SUCCESS/FAIL, READY·PROCESSING 은 PENDING(재조회). */
    private SmsDeliveryState mapState(String status, String statusName) {
        if (!"COMPLETED".equalsIgnoreCase(status)) {
            return SmsDeliveryState.PENDING;
        }
        return "success".equalsIgnoreCase(statusName) ? SmsDeliveryState.SUCCESS : SmsDeliveryState.FAIL;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asMessages(Map<String, Object> response) {
        if (response == null) {
            return List.of();
        }
        Object messages = response.get("messages");
        if (messages instanceof List<?> list) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    result.add((Map<String, Object>) map);
                }
            }
            return result;
        }
        return List.of();
    }

    private LocalDateTime parseCompleteTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim(), COMPLETE_TIME_FORMAT);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private Map<String, Object> buildBody(SmsSendCommand command, List<Map<String, String>> files) {
        List<Map<String, String>> messages = new ArrayList<>();
        for (SmsSendCommand.Recipient recipient : command.recipients()) {
            Map<String, String> message = new LinkedHashMap<>();
            message.put("to", recipient.to());
            message.put("content", recipient.content());
            messages.add(message);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", command.type());
        body.put("contentType", "COMM");
        body.put("countryCode", "82");
        body.put("from", from);
        if (StringUtils.hasText(command.subject())) {
            body.put("subject", command.subject());
        }
        body.put("content", command.content());
        body.put("messages", messages);
        if (!files.isEmpty()) {
            body.put("files", files);
        }
        // 예약 발송: reserveTime("yyyy-MM-dd HH:mm") 이 있으면 SENS 가 해당 시각에 발송한다.
        if (StringUtils.hasText(command.reserveTime())) {
            body.put("reserveTime", command.reserveTime());
            body.put("reserveTimeZone",
                    StringUtils.hasText(command.reserveTimeZone()) ? command.reserveTimeZone() : "Asia/Seoul");
        }
        return body;
    }

    private List<Map<String, String>> uploadImages(SmsSendCommand command) {
        List<Map<String, String>> files = new ArrayList<>();
        if (command.imagesBase64() == null || command.imagesBase64().isEmpty()) {
            return files;
        }
        int index = 0;
        for (String raw : command.imagesBase64()) {
            String fileId = uploadImage(raw, index++);
            Map<String, String> file = new LinkedHashMap<>();
            file.put("fileId", fileId);
            files.add(file);
        }
        return files;
    }

    private String uploadImage(String rawBase64, int index) {
        String base64 = stripDataUriPrefix(rawBase64);
        validateImage(base64);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("fileName", "mms_" + index + ".jpg");
        body.put("fileBody", base64);

        String path = "/sms/v2/services/" + serviceId + "/files";
        Map<String, Object> response = post(path, body);
        String fileId = stringValue(response, "fileId");
        if (!StringUtils.hasText(fileId) || "null".equals(fileId)) {
            log.warn("[SENS] file upload returned no fileId: {}", response);
            throw new BusinessException(ErrorCode.SMS_SEND_FAILED);
        }
        return fileId;
    }

    private void validateImage(String base64) {
        byte[] data;
        try {
            data = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.SMS_IMAGE_INVALID);
        }
        if (data.length == 0 || data.length > MAX_IMAGE_BYTES) {
            throw new BusinessException(ErrorCode.SMS_IMAGE_INVALID);
        }
        // JPEG 매직 넘버(FF D8 FF)만 허용 — jpg/jpeg 이외 거부
        if (data.length < 3
                || (data[0] & 0xFF) != 0xFF
                || (data[1] & 0xFF) != 0xD8
                || (data[2] & 0xFF) != 0xFF) {
            throw new BusinessException(ErrorCode.SMS_IMAGE_INVALID);
        }
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
            if (image == null
                    || image.getWidth() > MAX_IMAGE_WIDTH
                    || image.getHeight() > MAX_IMAGE_HEIGHT) {
                throw new BusinessException(ErrorCode.SMS_IMAGE_INVALID);
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SMS_IMAGE_INVALID);
        }
    }

    private String stripDataUriPrefix(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        int comma = trimmed.indexOf(',');
        if (trimmed.startsWith("data:") && comma > 0) {
            return trimmed.substring(comma + 1).trim();
        }
        return trimmed;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> post(String path, Object body) {
        try {
            return restClient.post()
                    .uri(BASE_URL + path)
                    .headers(headers -> applySignatureHeaders(headers, "POST", path))
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientResponseException e) {
            // NCP 응답 본문({"error":{errorCode,message,details}})을 명확히 남겨 원인 식별을 돕는다.
            log.warn("[SENS] request failed: {} status={} body={}",
                    path, e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.SMS_SEND_FAILED);
        } catch (RestClientException e) {
            log.error("[SENS] request failed: {}", path, e);
            throw new BusinessException(ErrorCode.SMS_SEND_FAILED);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> get(String path) {
        try {
            // 서명(path)과 실제 요청 URI 가 정확히 일치해야 한다(쿼리스트링 포함) → URI.create 로 템플릿 인코딩 우회.
            return restClient.get()
                    .uri(URI.create(BASE_URL + path))
                    .headers(headers -> applySignatureHeaders(headers, "GET", path))
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientResponseException e) {
            log.warn("[SENS] request failed: {} status={} body={}",
                    path, e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.SMS_SEND_FAILED);
        } catch (RestClientException e) {
            log.error("[SENS] request failed: {}", path, e);
            throw new BusinessException(ErrorCode.SMS_SEND_FAILED);
        }
    }

    private void delete(String path) {
        try {
            // 서명(path)과 실제 요청 URI 가 정확히 일치해야 한다 → URI.create 로 템플릿 인코딩 우회.
            restClient.delete()
                    .uri(URI.create(BASE_URL + path))
                    .headers(headers -> applySignatureHeaders(headers, "DELETE", path))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            log.warn("[SENS] request failed: {} status={} body={}",
                    path, e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.SMS_SEND_FAILED);
        } catch (RestClientException e) {
            log.error("[SENS] request failed: {}", path, e);
            throw new BusinessException(ErrorCode.SMS_SEND_FAILED);
        }
    }

    private void applySignatureHeaders(HttpHeaders headers, String method, String path) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        headers.set("x-ncp-apigw-timestamp", timestamp);
        headers.set("x-ncp-iam-access-key", accessKey);
        headers.set("x-ncp-apigw-signature-v2", makeSignature(method, path, timestamp));
    }

    /**
     * StringToSign = "{METHOD} {URL}\n{timestamp}\n{accessKey}" → HMAC-SHA256(secretKey) → Base64.
     */
    private String makeSignature(String method, String path, String timestamp) {
        String message = method + " " + path + "\n" + timestamp + "\n" + accessKey;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(raw);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new BusinessException(ErrorCode.SMS_SEND_FAILED);
        }
    }

    private String stringValue(Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }
}
