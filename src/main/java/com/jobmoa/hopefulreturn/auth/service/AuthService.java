package com.jobmoa.hopefulreturn.auth.service;

import com.jobmoa.hopefulreturn.auth.dto.EmailSendRequest;
import com.jobmoa.hopefulreturn.auth.dto.EmailVerifyRequest;
import com.jobmoa.hopefulreturn.auth.dto.LoginRequest;
import com.jobmoa.hopefulreturn.auth.dto.MemberResponse;
import com.jobmoa.hopefulreturn.auth.dto.RefreshRequest;
import com.jobmoa.hopefulreturn.auth.dto.SignupRequest;
import com.jobmoa.hopefulreturn.auth.dto.TokenResponse;
import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.email.EmailVerificationService;
import com.jobmoa.hopefulreturn.member.entity.Member;
import com.jobmoa.hopefulreturn.member.entity.MemberRole;
import com.jobmoa.hopefulreturn.member.repository.MemberRepository;
import com.jobmoa.hopefulreturn.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailVerificationService emailVerificationService;

    @Transactional
    public MemberResponse signup(SignupRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        Member member = Member.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .role(MemberRole.USER)
                .emailVerified(false)
                .build();
        Member saved = memberRepository.save(member);
        return new MemberResponse(saved.getId(), saved.getEmail(), saved.getName());
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        return issueTokens(member);
    }

    @Transactional(readOnly = true)
    public TokenResponse refresh(RefreshRequest request) {
        if (!jwtTokenProvider.validate(request.refreshToken())) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        String email = jwtTokenProvider.getEmail(request.refreshToken());
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        return issueTokens(member);
    }

    public void sendEmailVerification(EmailSendRequest request) {
        emailVerificationService.sendCode(request.email());
    }

    @Transactional
    public void verifyEmail(EmailVerifyRequest request) {
        if (!emailVerificationService.verify(request.email(), request.code())) {
            throw new BusinessException(ErrorCode.VERIFICATION_CODE_MISMATCH);
        }
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        member.verifyEmail();
    }

    private TokenResponse issueTokens(Member member) {
        String access = jwtTokenProvider.createAccessToken(member.getEmail(), member.getRole().name());
        String refresh = jwtTokenProvider.createRefreshToken(member.getEmail());
        return TokenResponse.bearer(access, refresh);
    }
}
