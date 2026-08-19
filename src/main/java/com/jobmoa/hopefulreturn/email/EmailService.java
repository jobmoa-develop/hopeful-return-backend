package com.jobmoa.hopefulreturn.email;

import com.jobmoa.hopefulreturn.email.dto.StaffUnavailableMail;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class EmailService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String NL = System.lineSeparator();

    private final JavaMailSender mailSender;
    // 발신자 주소. 미설정 시 JavaMail 이 OS사용자@호스트명(예: no1fc@JOBMOA)으로 보내 SMTP 서버가 거부(553)하므로,
    // 인증 계정(MAIL_USERNAME) 또는 MAIL_FROM 을 명시적으로 세팅한다.
    private final String fromAddress;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${app.mail.from:}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void sendVerificationCode(String to, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            applyFrom(message);
            message.setTo(to);
            message.setSubject("[hopeful-return] 이메일 인증 코드");
            message.setText("인증 코드: " + code + System.lineSeparator() + "5분 이내에 입력해 주세요.");
            mailSender.send(message);
        } catch (Exception e) {
            log.error("이메일 발송 실패: to={}", to, e);
            throw e;
        }
    }

    /**
     * 인력이 배정된 회차의 날짜를 근무불가로 전환했음을 배정 관리자에게 알린다.
     * 발송 실패는 상위(리스너)에서 개별 처리할 수 있도록 예외를 그대로 전파한다.
     */
    public void sendStaffUnavailableNotice(String to, StaffUnavailableMail data) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            applyFrom(message);
            message.setTo(to);
            message.setSubject(buildSubject(data));
            message.setText(buildBody(data));
            mailSender.send(message);
        } catch (Exception e) {
            log.error("근무불가 알림 메일 발송 실패: to={}", to, e);
            throw e;
        }
    }

    // 발신자 주소가 설정된 경우에만 From 을 지정한다(미설정 시 기존 동작 유지).
    private void applyFrom(SimpleMailMessage message) {
        if (StringUtils.hasText(fromAddress)) {
            message.setFrom(fromAddress);
        }
    }

    private String buildSubject(StaffUnavailableMail data) {
        String region = orDash(data.regionName());
        String round = data.round() == null ? "-" : String.valueOf(data.round());
        String date = data.date() == null ? "-" : data.date().format(DATE_FORMAT);
        return "[hopeful-return] 인력 근무불가 알림 — " + region + " " + round + "회차 " + date;
    }

    private String buildBody(StaffUnavailableMail data) {
        String date = data.date() == null ? "-" : data.date().format(DATE_FORMAT);
        return "배정된 인력이 근무 불가로 변경되었습니다. 재배정이 필요할 수 있습니다." + NL + NL
                + "· 인력: " + orDash(data.staffName()) + NL
                + "· 지역: " + orDash(data.regionName()) + NL
                + "· 회차: " + (data.round() == null ? "-" : data.round() + "회차") + NL
                + "· 날짜: " + date + NL
                + "· 시간대: " + orDash(data.sessionLabel()) + NL
                + "· 사유: " + orDash(data.reason()) + NL;
    }

    private String orDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
