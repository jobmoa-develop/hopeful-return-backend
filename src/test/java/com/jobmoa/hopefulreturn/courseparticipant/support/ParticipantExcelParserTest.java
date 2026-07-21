package com.jobmoa.hopefulreturn.courseparticipant.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.BulkImportParsedRow;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/**
 * {@link ParticipantExcelParser} 단위 테스트 — 정부식 XLSX 를 POI 로 즉석 생성해 파싱을 검증한다.
 * 헤더명 기준 매핑·전화/생년월일/날짜 정규화·상태 파생(선정/미선정)·오류 행 격리·필수 헤더 누락 예외.
 */
class ParticipantExcelParserTest {

    private static final String[] HEADERS = {
        "교육과정명", "교육기관소재지_시도", "교육기관소재지_시군구", "교육생명",
        "생년월일", "휴대폰번호", "신청일시", "접수진행상태", "선정여부", "선정일시"
    };

    private final ParticipantExcelParser parser = new ParticipantExcelParser();

    @Test
    @DisplayName("정상 행: 헤더명 매핑·하이픈 전화 정규화·선정→CONFIRMED")
    void parsesValidRow() {
        MockMultipartFile file = xlsx(new String[][] {
            {"[현장] (서울)리본(Re:Born)커리어_16회차", "서울특별시", "서울특별시 양천구", "홍길동",
                "19860313", "010-1234-5678", "2026-07-09 10:45:46.0", "접수완료", "선정", "2026-07-09"}
        });

        List<BulkImportParsedRow> rows = parser.parse(file);

        assertThat(rows).hasSize(1);
        BulkImportParsedRow row = rows.get(0);
        assertThat(row.name()).isEqualTo("홍길동");
        assertThat(row.phone()).isEqualTo("01012345678"); // 하이픈 제거
        assertThat(row.birthYear()).isEqualTo(1986);
        assertThat(row.applyDate()).isEqualTo(LocalDate.of(2026, 7, 9));
        assertThat(row.receptionDate()).isEqualTo(LocalDate.of(2026, 7, 9));
        assertThat(row.status()).isEqualTo("CONFIRMED");
        assertThat(row.sourceCourseName()).isEqualTo("[현장] (서울)리본(Re:Born)커리어_16회차");
        assertThat(row.error()).isNull();
    }

    @Test
    @DisplayName("전화 정규화: 선행 0이 빠진 10자리는 0을 붙여 11자리로")
    void normalizesTenDigitPhone() {
        MockMultipartFile file = xlsx(new String[][] {
            {"(서울)_1회차", "서울특별시", "서울특별시 양천구", "김철수",
                "19780101", "1012345678", "2026-07-01", "접수완료", "선정", "2026-07-01"}
        });

        List<BulkImportParsedRow> rows = parser.parse(file);

        assertThat(rows.get(0).phone()).isEqualTo("01012345678");
        assertThat(rows.get(0).error()).isNull();
    }

    @Test
    @DisplayName("미선정 행 → CANCELED (선정여부=미선정)")
    void mapsUnselectedToCanceled() {
        MockMultipartFile file = xlsx(new String[][] {
            {"(서울)_1회차", "서울특별시", "서울특별시 양천구", "이영희",
                "19900101", "01055556666", "2026-07-01", "접수완료", "미선정", ""}
        });

        List<BulkImportParsedRow> rows = parser.parse(file);

        assertThat(rows.get(0).status()).isEqualTo("CANCELED");
        assertThat(rows.get(0).error()).isNull();
    }

    @Test
    @DisplayName("휴대폰번호 없는 행: 전체 실패가 아니라 해당 행에 error 를 채운다")
    void marksRowWithMissingPhone() {
        MockMultipartFile file = xlsx(new String[][] {
            {"(서울)_1회차", "서울특별시", "서울특별시 양천구", "김이름", "19900101", "", "2026-07-01", "접수완료", "선정", ""}
        });

        List<BulkImportParsedRow> rows = parser.parse(file);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).error()).contains("휴대폰번호");
    }

    @Test
    @DisplayName("교육생명·휴대폰번호가 모두 빈 행은 건너뛴다")
    void skipsBlankRows() {
        MockMultipartFile file = xlsx(new String[][] {
            {"(서울)_1회차", "서울특별시", "서울특별시 양천구", "홍길동", "19860313", "01011112222", "2026-07-09", "접수완료", "선정", "2026-07-09"},
            {"", "", "", "", "", "", "", "", "", ""}
        });

        List<BulkImportParsedRow> rows = parser.parse(file);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).rowNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("필수 헤더(교육생명) 누락 시 BULK_IMPORT_INVALID_FILE 예외")
    void throwsWhenRequiredHeaderMissing() {
        MockMultipartFile file = xlsxWithHeaders(
                new String[] {"교육과정명", "휴대폰번호"},
                new String[][] {{"(서울)_1회차", "01011112222"}});

        assertThatThrownBy(() -> parser.parse(file)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("빈 파일은 BULK_IMPORT_INVALID_FILE 예외")
    void throwsOnEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile(
                "file", "empty.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[0]);

        assertThatThrownBy(() -> parser.parse(empty)).isInstanceOf(BusinessException.class);
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────

    private MockMultipartFile xlsx(String[][] dataRows) {
        return xlsxWithHeaders(HEADERS, dataRows);
    }

    private MockMultipartFile xlsxWithHeaders(String[] headers, String[][] dataRows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("data");
            Row headerRow = sheet.createRow(0);
            for (int c = 0; c < headers.length; c++) {
                headerRow.createCell(c).setCellValue(headers[c]);
            }
            for (int r = 0; r < dataRows.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < dataRows[r].length; c++) {
                    row.createCell(c).setCellValue(dataRows[r][c]);
                }
            }
            workbook.write(out);
            return new MockMultipartFile(
                    "file", "participants.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("테스트 xlsx 생성 실패", e);
        }
    }
}
