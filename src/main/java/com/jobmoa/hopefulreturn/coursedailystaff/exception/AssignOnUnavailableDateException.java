package com.jobmoa.hopefulreturn.coursedailystaff.exception;

import com.jobmoa.hopefulreturn.coursedailystaff.model.dto.AssignUnavailable;
import java.util.List;
import lombok.Getter;

/**
 * 배정 저장 시 대상 인력이 해당 날짜·시간대에 근무 불가일(course_staff_id NULL·is_available=false)로
 * 등록되어 있어 배정을 거부할 때 던진다. 전역 예외 처리기가 409(ASSIGN_ON_UNAVAILABLE_DATE)로
 * 불가 목록과 함께 응답한다. 중복 충돌(confirmConflicts 로 override 가능)과 달리 override 없는
 * 하드 블록이다.
 */
@Getter
public class AssignOnUnavailableDateException extends RuntimeException {

    private final transient List<AssignUnavailable> unavailable;

    public AssignOnUnavailableDateException(List<AssignUnavailable> unavailable) {
        super("근무 불가일 배정 거부 " + unavailable.size() + "건");
        this.unavailable = unavailable;
    }
}
