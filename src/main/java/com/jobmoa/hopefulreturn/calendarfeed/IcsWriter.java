package com.jobmoa.hopefulreturn.calendarfeed;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 최소 RFC5545(iCalendar) 텍스트 생성기. VCALENDAR 1개 + VEVENT N개(반복/알림 없음)만 다룬다.
 * CRLF 줄끝, 75옥텟 라인 폴딩, 텍스트 이스케이프, Asia/Seoul VTIMEZONE 을 처리한다.
 */
public final class IcsWriter {

    private static final String CRLF = "\r\n";
    private static final String TZID = "Asia/Seoul";
    private static final String PRODID = "-//jobmoa//hopeful-return//KR";
    private static final int MAX_OCTETS = 75;
    private static final DateTimeFormatter LOCAL = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final DateTimeFormatter UTC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    private IcsWriter() {
    }

    /** 하나의 캘린더 이벤트. 시각은 Asia/Seoul 로컬시각(KST, DST 없음)으로 해석된다. */
    public record IcsEvent(
            String uid,
            LocalDateTime start,
            LocalDateTime end,
            String summary,
            String location,
            String description) {
    }

    public static String write(String calendarName, LocalDateTime dtStampUtc, List<IcsEvent> events) {
        StringBuilder sb = new StringBuilder();
        line(sb, "BEGIN:VCALENDAR");
        line(sb, "VERSION:2.0");
        line(sb, "PRODID:" + PRODID);
        line(sb, "CALSCALE:GREGORIAN");
        line(sb, "METHOD:PUBLISH");
        line(sb, "X-WR-CALNAME:" + escape(calendarName));
        line(sb, "X-WR-TIMEZONE:" + TZID);

        // VTIMEZONE: 한국 표준시(+09:00), 서머타임 없음
        line(sb, "BEGIN:VTIMEZONE");
        line(sb, "TZID:" + TZID);
        line(sb, "BEGIN:STANDARD");
        line(sb, "TZOFFSETFROM:+0900");
        line(sb, "TZOFFSETTO:+0900");
        line(sb, "TZNAME:KST");
        line(sb, "DTSTART:19700101T000000");
        line(sb, "END:STANDARD");
        line(sb, "END:VTIMEZONE");

        String stamp = UTC.format(dtStampUtc);
        for (IcsEvent e : events) {
            line(sb, "BEGIN:VEVENT");
            line(sb, "UID:" + escape(e.uid()));
            line(sb, "DTSTAMP:" + stamp);
            line(sb, "DTSTART;TZID=" + TZID + ":" + LOCAL.format(e.start()));
            line(sb, "DTEND;TZID=" + TZID + ":" + LOCAL.format(e.end()));
            line(sb, "SUMMARY:" + escape(e.summary()));
            if (e.location() != null && !e.location().isBlank()) {
                line(sb, "LOCATION:" + escape(e.location()));
            }
            if (e.description() != null && !e.description().isBlank()) {
                line(sb, "DESCRIPTION:" + escape(e.description()));
            }
            line(sb, "END:VEVENT");
        }

        line(sb, "END:VCALENDAR");
        return sb.toString();
    }

    private static void line(StringBuilder sb, String content) {
        sb.append(fold(content)).append(CRLF);
    }

    /** RFC5545 텍스트 이스케이프: 역슬래시·세미콜론·콤마·개행. */
    private static String escape(String v) {
        if (v == null) {
            return "";
        }
        return v.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n")
                .replace("\r", "\\n");
    }

    /**
     * 75옥텟(UTF-8) 초과 전에 CRLF + 선행 공백을 삽입해 접는다. 멀티바이트 문자를 분할하지 않도록
     * 코드포인트 단위로 진행한다. 이어지는 물리 줄은 선행 공백 1옥텟을 포함해 계산한다.
     */
    private static String fold(String logicalLine) {
        if (logicalLine.getBytes(StandardCharsets.UTF_8).length <= MAX_OCTETS) {
            return logicalLine;
        }
        StringBuilder out = new StringBuilder();
        int octets = 0;
        for (int i = 0; i < logicalLine.length(); ) {
            int cp = logicalLine.codePointAt(i);
            int charLen = Character.charCount(cp);
            String s = logicalLine.substring(i, i + charLen);
            int b = s.getBytes(StandardCharsets.UTF_8).length;
            if (octets + b > MAX_OCTETS) {
                out.append(CRLF).append(' ');
                octets = 1; // 선행 공백
            }
            out.append(s);
            octets += b;
            i += charLen;
        }
        return out.toString();
    }
}
