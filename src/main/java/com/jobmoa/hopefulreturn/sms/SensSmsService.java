package com.jobmoa.hopefulreturn.sms;

import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
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
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.serviceId = serviceId;
        this.from = from;
    }

    @Override
    public void sendVerificationCode(String phoneNumber, String code) {
        String body = "[인증코드] " + code;
        send(new SmsSendCommand(
                "SMS", null, body,
                List.of(new SmsSendCommand.Recipient(phoneNumber, body)),
                null));
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
