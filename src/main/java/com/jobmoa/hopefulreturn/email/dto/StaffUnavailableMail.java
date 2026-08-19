package com.jobmoa.hopefulreturn.email.dto;

import java.time.LocalDate;

/**
 * 인력 근무불가 알림 메일 본문 조립용 값 객체.
 * 값이 없는 필드(지역·회차·사유 등)는 null 을 허용하고 메일 조립 시 대체 문구로 처리한다.
 */
public record StaffUnavailableMail(
        String staffName,
        String regionName,
        Integer round,
        LocalDate date,
        String sessionLabel,
        String reason) {
}
