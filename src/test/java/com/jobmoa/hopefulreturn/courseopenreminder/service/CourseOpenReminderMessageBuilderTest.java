package com.jobmoa.hopefulreturn.courseopenreminder.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.jobmoa.hopefulreturn.courseopenreminder.service.CourseOpenReminderMessageBuilder.SessionDate;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 개강 하루전 안내 문자 본문 빌더 단위 테스트 — 치환·"하루/N일 전"·오전/오후 표기·일정 정렬 검증.
 */
class CourseOpenReminderMessageBuilderTest {

    private final CourseOpenReminderMessageBuilder builder = new CourseOpenReminderMessageBuilder();

    @Test
    @DisplayName("daysBefore=1 이면 '하루 전', 지역·회차·이름·일정·주소가 양식대로 채워진다")
    void buildDayBefore() {
        String body = builder.build("양천", 22, 58, "이강사",
                List.of(new SessionDate(LocalDate.of(2026, 6, 23), "AM"),
                        new SessionDate(LocalDate.of(2026, 6, 24), "PM")),
                "양천 교육장 A", 1);

        assertThat(body).isEqualTo(
                "개강 회차 하루전 안내\n"
                        + "양천(22)_58 개강 하루 전입니다.\n"
                        + "아래 방문일자에 지장이 없도록 방문 부탁드립니다.\n"
                        + "이강사\n"
                        + "일정: 6/23(오전), 6/24(오후)\n"
                        + "주소: 양천 교육장 A");
    }

    @Test
    @DisplayName("daysBefore=3 이면 '3일 전' 으로 문구가 바뀐다")
    void buildNDaysBefore() {
        String body = builder.build("인천", 3, 12, "김진행",
                List.of(new SessionDate(LocalDate.of(2026, 7, 1), "FULL")), "인천 지부", 3);

        assertThat(body).contains("인천(3)_12 개강 3일 전입니다.");
        assertThat(body).contains("일정: 7/1");
        assertThat(body).doesNotContain("(오전)").doesNotContain("(오후)");
    }

    @Test
    @DisplayName("일정은 날짜·세션 순으로 정렬되고 중복은 제거된다")
    void formatScheduleSortedDistinct() {
        String schedule = builder.formatSchedule(List.of(
                new SessionDate(LocalDate.of(2026, 6, 24), "PM"),
                new SessionDate(LocalDate.of(2026, 6, 23), "PM"),
                new SessionDate(LocalDate.of(2026, 6, 23), "AM"),
                new SessionDate(LocalDate.of(2026, 6, 23), "AM")));

        assertThat(schedule).isEqualTo("6/23(오전), 6/23(오후), 6/24(오후)");
    }

    @Test
    @DisplayName("지역·주소가 null 이면 빈 문자열로 안전하게 채운다")
    void buildWithNulls() {
        String body = builder.build(null, null, 5, "박강사",
                List.of(new SessionDate(LocalDate.of(2026, 8, 10), "AM")), null, 1);

        assertThat(body).contains("()_5 개강 하루 전입니다.");
        assertThat(body).contains("주소: ");
    }
}
