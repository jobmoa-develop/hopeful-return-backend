package com.jobmoa.hopefulreturn.common;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * 영업일 계산 유틸. 주말(토/일)만 제외하며 공휴일은 고려하지 않는다.
 * 공휴일은 API 가 필요하므로 추후에 고려하는것으로 한다.
 */
public final class BusinessDayCalculator {

    private BusinessDayCalculator() {
    }

    /**
     * 기준일로부터 영업일 N일 후 날짜를 계산한다(주말만 제외).
     */
    public static LocalDate addBusinessDays(LocalDate start, int businessDays) {
        if (start == null) {
            return null;
        }
        LocalDate result = start;
        int added = 0;
        while (added < businessDays) {
            result = result.plusDays(1);
            if (result.getDayOfWeek() != DayOfWeek.SATURDAY && result.getDayOfWeek() != DayOfWeek.SUNDAY) {
                added++;
            }
        }
        return result;
    }
}