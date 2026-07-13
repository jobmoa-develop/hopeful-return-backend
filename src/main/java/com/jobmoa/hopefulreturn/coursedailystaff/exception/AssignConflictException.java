package com.jobmoa.hopefulreturn.coursedailystaff.exception;

import com.jobmoa.hopefulreturn.coursedailystaff.model.dto.AssignConflict;
import java.util.List;
import lombok.Getter;

/**
 * 배정 저장 시 타 회차와 중복(같은 날짜·겹치는 시간대)이 발견됐고 사용자가 아직 확인하지 않은
 * (confirmConflicts=false) 경우 던진다. 전역 예외 처리기가 409(ASSIGN_CONFLICT)로 충돌 목록과
 * 함께 응답한다. 사용자가 확인(confirmConflicts=true)하면 현재 회차로 이동 저장된다.
 */
@Getter
public class AssignConflictException extends RuntimeException {

    private final transient List<AssignConflict> conflicts;

    public AssignConflictException(List<AssignConflict> conflicts) {
        super("배정 중복 충돌 " + conflicts.size() + "건");
        this.conflicts = conflicts;
    }
}
