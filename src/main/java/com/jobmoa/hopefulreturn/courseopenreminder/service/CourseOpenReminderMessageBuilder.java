package com.jobmoa.hopefulreturn.courseopenreminder.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 개강 하루전 안내 문자 본문 빌더(무상태 순수 로직 — 단위 테스트 대상).
 *
 * <p>사용자 지정 양식:
 * <pre>
 * 개강 회차 하루전 안내
 * {지역}({지역회차})_{전체회차} 개강 {하루/N일} 전입니다.
 * 아래 방문일자에 지장이 없도록 방문 부탁드립니다.
 * {배정인력명}
 * 일정: {근무일 나열}
 * 주소: {교육장 주소}
 * </pre>
 * 한 사람(userId)당 한 건을 발송하므로, 오전/오후 강사가 서로 다른 사람이면 자연히 별도 발송된다.
 * 같은 사람이 오전·오후를 겸하면 일정에 (오전)/(오후) 를 붙여 한 건으로 안내한다.
 */
@Component
public class CourseOpenReminderMessageBuilder {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("M/d");

    /** 근무일 + 시간대(AM/PM/FULL). 세션이 오전/오후면 일정 표기에 접미사를 붙인다. */
    public record SessionDate(LocalDate date, String sessionType) {
    }

    public String build(String regionName, Integer localCourseNumber, Integer courseNumber,
            String staffName, List<SessionDate> schedule, String location, int daysBefore) {
        String beforeText = daysBefore <= 1 ? "하루" : (daysBefore + "일");
        // 제목의 "하루전" 도 days_before 로 파라미터화(1이면 기존과 동일, 그 외 "N일전").
        String titleBefore = daysBefore <= 1 ? "하루전" : (daysBefore + "일전");
        return "개강 회차 " + titleBefore + " 안내\n"
                + nullToEmpty(regionName) + "(" + nullToEmpty(localCourseNumber) + ")_"
                + nullToEmpty(courseNumber) + " 개강 " + beforeText + " 전입니다.\n"
                + "아래 방문일자에 지장이 없도록 방문 부탁드립니다.\n"
                + nullToEmpty(staffName) + "\n"
                + "일정: " + formatSchedule(schedule) + "\n"
                + "주소: " + nullToEmpty(location);
    }

    /** 일정을 날짜·세션 순으로 정렬·중복제거 후 "6/23(오전), 6/24" 형태로 이어붙인다. */
    String formatSchedule(List<SessionDate> schedule) {
        if (schedule == null || schedule.isEmpty()) {
            return "";
        }
        return schedule.stream()
                .filter(sd -> sd.date() != null)
                .sorted(Comparator.comparing(SessionDate::date)
                        .thenComparing(sd -> sessionRank(sd.sessionType())))
                .map(this::formatOne)
                .distinct()
                .collect(Collectors.joining(", "));
    }

    private String formatOne(SessionDate sd) {
        return sd.date().format(DATE_FMT) + sessionSuffix(sd.sessionType());
    }

    private String sessionSuffix(String sessionType) {
        if ("AM".equals(sessionType)) {
            return "(오전)";
        }
        if ("PM".equals(sessionType)) {
            return "(오후)";
        }
        return "";
    }

    private int sessionRank(String sessionType) {
        if ("AM".equals(sessionType)) {
            return 0;
        }
        if ("PM".equals(sessionType)) {
            return 1;
        }
        return 2;
    }

    private String nullToEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
