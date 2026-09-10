package com.jobmoa.hopefulreturn.calendarfeed;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IcsWriterTest {

    private static final LocalDateTime STAMP = LocalDateTime.of(2026, 9, 10, 1, 2, 3);

    @Test
    @DisplayName("VCALENDAR 골격·VTIMEZONE·VEVENT 필드를 CRLF 로 출력한다")
    void writesCalendarSkeleton() {
        String ics = IcsWriter.write("홍길동 일정", STAMP, List.of(new IcsWriter.IcsEvent(
                "ss-1@hopeful-return",
                LocalDateTime.of(2026, 9, 15, 9, 0),
                LocalDateTime.of(2026, 9, 15, 13, 0),
                "근무 가능", null, null)));

        assertThat(ics).contains("BEGIN:VCALENDAR\r\n");
        assertThat(ics).contains("VERSION:2.0");
        assertThat(ics).contains("BEGIN:VTIMEZONE");
        assertThat(ics).contains("TZID:Asia/Seoul");
        assertThat(ics).contains("BEGIN:VEVENT");
        assertThat(ics).contains("UID:ss-1@hopeful-return");
        assertThat(ics).contains("DTSTAMP:20260910T010203Z");
        assertThat(ics).contains("DTSTART;TZID=Asia/Seoul:20260915T090000");
        assertThat(ics).contains("DTEND;TZID=Asia/Seoul:20260915T130000");
        assertThat(ics).contains("SUMMARY:근무 가능");
        assertThat(ics).endsWith("END:VCALENDAR\r\n");
    }

    @Test
    @DisplayName("location/description 이 없으면 해당 라인을 생략한다")
    void omitsBlankOptionalFields() {
        String ics = IcsWriter.write("cal", STAMP, List.of(new IcsWriter.IcsEvent(
                "u", LocalDateTime.of(2026, 9, 15, 9, 0), LocalDateTime.of(2026, 9, 15, 13, 0),
                "제목", "  ", "")));

        assertThat(ics).doesNotContain("LOCATION:");
        assertThat(ics).doesNotContain("DESCRIPTION:");
    }

    @Test
    @DisplayName("특수문자(역슬래시·세미콜론·콤마·개행)를 RFC5545 규칙으로 이스케이프한다")
    void escapesSpecialCharacters() {
        String ics = IcsWriter.write("cal", STAMP, List.of(new IcsWriter.IcsEvent(
                "u", LocalDateTime.of(2026, 9, 15, 9, 0), LocalDateTime.of(2026, 9, 15, 13, 0),
                "a;b,c\\d", null, "1줄\n2줄")));

        assertThat(ics).contains("SUMMARY:a\\;b\\,c\\\\d");
        assertThat(ics).contains("DESCRIPTION:1줄\\n2줄");
    }

    @Test
    @DisplayName("75옥텟을 넘는 긴 라인은 CRLF + 선행 공백으로 접는다")
    void foldsLongLines() {
        String longSummary = "X".repeat(200);
        String ics = IcsWriter.write("cal", STAMP, List.of(new IcsWriter.IcsEvent(
                "u", LocalDateTime.of(2026, 9, 15, 9, 0), LocalDateTime.of(2026, 9, 15, 13, 0),
                longSummary, null, null)));

        // 폴딩 마커(CRLF + space) 가 존재하고, 어떤 물리 줄도 75옥텟을 넘지 않는다.
        assertThat(ics).contains("\r\n ");
        for (String physicalLine : ics.split("\r\n", -1)) {
            assertThat(physicalLine.getBytes(java.nio.charset.StandardCharsets.UTF_8).length)
                    .isLessThanOrEqualTo(75);
        }
    }
}
