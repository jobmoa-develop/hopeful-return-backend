package com.jobmoa.hopefulreturn.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendVerificationCode(String to, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("[hopeful-return] 이메일 인증 코드");
            message.setText("인증 코드: " + code + System.lineSeparator() + "5분 이내에 입력해 주세요.");
            mailSender.send(message);
        } catch (Exception e) {
            log.error("이메일 발송 실패: to={}", to, e);
            throw e;
        }
    }
}
