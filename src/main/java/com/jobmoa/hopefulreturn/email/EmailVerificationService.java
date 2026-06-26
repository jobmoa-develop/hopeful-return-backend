package com.jobmoa.hopefulreturn.email;

import java.security.SecureRandom;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final String KEY_PREFIX = "email:verify:";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;

    @Value("${app.mail.verification-ttl-seconds:300}")
    private long ttlSeconds;

    public void sendCode(String email) {
        String code = generateCode();
        redisTemplate.opsForValue().set(KEY_PREFIX + email, code, Duration.ofSeconds(ttlSeconds));
        emailService.sendVerificationCode(email, code);
    }

    public boolean verify(String email, String code) {
        String stored = redisTemplate.opsForValue().get(KEY_PREFIX + email);
        if (stored != null && stored.equals(code)) {
            redisTemplate.delete(KEY_PREFIX + email);
            return true;
        }
        return false;
    }

    private String generateCode() {
        return String.format("%06d", RANDOM.nextInt(1000000));
    }
}
