package com.jobmoa.hopefulreturn.courseparticipant.support;

import com.jobmoa.hopefulreturn.common.BusinessException;
import com.jobmoa.hopefulreturn.common.ErrorCode;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.BulkImportParsedRow;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 희망리턴패키지 참여자 일괄 등록용 정부식 XLSX 파서.
 *
 * <p>1행을 헤더로 보고 <b>헤더명 기준</b>으로 열을 찾는다(열 순서 변화에 견고). 각 데이터 행을
 * {@link BulkImportParsedRow} 로 변환하며, 필수값(교육생명·휴대폰번호) 누락 등 검증 실패는
 * 전체 실패가 아니라 해당 행의 {@code error} 로 표시한다(부분 등록 정책).
 */
@Component
public class ParticipantExcelParser {

    private static final String COL_COURSE_NAME = "교육과정명";
    private static final String COL_SIDO = "교육기관소재지_시도";
    private static final String COL_SIGUNGU = "교육기관소재지_시군구";
    private static final String COL_NAME = "교육생명";
    private static final String COL_BIRTH = "생년월일";
    private static final String COL_PHONE = "휴대폰번호";
    private static final String COL_APPLY = "신청일시";
    private static final String COL_RECEPTION_STATUS = "접수진행상태";
    private static final String COL_SELECTED = "선정여부";
    private static final String COL_SELECTED_AT = "선정일시";

    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_CANCELED = "CANCELED";
    private static final String STATUS_APPLIED = "APPLIED";

    private static final int BIRTH_YEAR_LENGTH = 4;
    private static final int ISO_DATE_LENGTH = 10; // yyyy-MM-dd
    private static final int PHONE_MIN_LENGTH = 9;
    private static final int PHONE_MAX_LENGTH = 20;

    private final DataFormatter dataFormatter = new DataFormatter();

    /**
     * 업로드된 XLSX 를 파싱한다. 파일을 열 수 없거나 필수 헤더가 없으면
     * {@link ErrorCode#BULK_IMPORT_INVALID_FILE} 예외를 던진다.
     */
    public List<BulkImportParsedRow> parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BULK_IMPORT_INVALID_FILE);
        }
        try (InputStream in = file.getInputStream(); Workbook workbook = new XSSFWorkbook(in)) {
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            if (sheet == null) {
                throw new BusinessException(ErrorCode.BULK_IMPORT_INVALID_FILE);
            }
            Row header = sheet.getRow(sheet.getFirstRowNum());
            Map<String, Integer> columns = readHeader(header);
            requireColumns(columns, COL_COURSE_NAME, COL_NAME, COL_PHONE);
            return readRows(sheet, columns);
        } catch (BusinessException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            // POI 가 xlsx 가 아닌 파일(예: xls/손상 파일)에 던지는 예외를 사용자용 오류로 변환한다.
            throw new BusinessException(ErrorCode.BULK_IMPORT_INVALID_FILE);
        }
    }

    private Map<String, Integer> readHeader(Row header) {
        if (header == null) {
            throw new BusinessException(ErrorCode.BULK_IMPORT_INVALID_FILE);
        }
        Map<String, Integer> columns = new HashMap<>();
        for (int c = header.getFirstCellNum(); c < header.getLastCellNum(); c++) {
            String name = cellString(header.getCell(c));
            if (StringUtils.hasText(name)) {
                columns.putIfAbsent(name.trim(), c);
            }
        }
        return columns;
    }

    private void requireColumns(Map<String, Integer> columns, String... required) {
        for (String col : required) {
            if (!columns.containsKey(col)) {
                throw new BusinessException(ErrorCode.BULK_IMPORT_INVALID_FILE);
            }
        }
    }

    private List<BulkImportParsedRow> readRows(Sheet sheet, Map<String, Integer> columns) {
        List<BulkImportParsedRow> rows = new ArrayList<>();
        int rowNumber = 0;
        for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (isBlankRow(row, columns)) {
                continue;
            }
            rowNumber++;
            rows.add(toParsedRow(rowNumber, row, columns));
        }
        return rows;
    }

    private BulkImportParsedRow toParsedRow(int rowNumber, Row row, Map<String, Integer> columns) {
        String sourceCourseName = value(row, columns, COL_COURSE_NAME);
        String sido = value(row, columns, COL_SIDO);
        String sigungu = value(row, columns, COL_SIGUNGU);
        String name = value(row, columns, COL_NAME);
        String phone = PhoneNormalizer.normalize(value(row, columns, COL_PHONE));
        Integer birthYear = birthYear(value(row, columns, COL_BIRTH));
        LocalDate applyDate = date(value(row, columns, COL_APPLY));
        LocalDate receptionDate = date(value(row, columns, COL_SELECTED_AT));
        String status = resolveStatus(
                value(row, columns, COL_SELECTED), value(row, columns, COL_RECEPTION_STATUS));

        String error = validate(name, phone);
        return new BulkImportParsedRow(
                rowNumber, sourceCourseName, sido, sigungu, name, phone,
                birthYear, applyDate, receptionDate, status, error);
    }

    /**
     * 접수진행상태·선정여부로 등록 상태를 정한다.
     * 접수취소 → CANCELED, 선정 → CONFIRMED, 미선정 → CANCELED, 그 외(접수완료 등) → APPLIED.
     */
    private String resolveStatus(String selection, String receptionStatus) {
        if (receptionStatus != null && receptionStatus.contains("취소")) {
            return STATUS_CANCELED;
        }
        if ("선정".equals(selection)) {
            return STATUS_CONFIRMED;
        }
        if ("미선정".equals(selection)) {
            return STATUS_CANCELED;
        }
        // 접수완료 등 선정 판정 전인 행은 접수(APPLIED) 상태로 등록한다.
        return STATUS_APPLIED;
    }

    private String validate(String name, String phone) {
        if (!StringUtils.hasText(name)) {
            return "교육생명이 없습니다.";
        }
        if (!StringUtils.hasText(phone) || phone.length() < PHONE_MIN_LENGTH || phone.length() > PHONE_MAX_LENGTH) {
            return "휴대폰번호가 올바르지 않습니다.";
        }
        return null;
    }

    private boolean isBlankRow(Row row, Map<String, Integer> columns) {
        if (row == null) {
            return true;
        }
        // 교육생명·휴대폰번호가 모두 비어있으면 실질적으로 빈 행으로 간주한다.
        return !StringUtils.hasText(value(row, columns, COL_NAME))
                && !StringUtils.hasText(value(row, columns, COL_PHONE));
    }

    private String value(Row row, Map<String, Integer> columns, String columnName) {
        Integer idx = columns.get(columnName);
        if (idx == null) {
            return null;
        }
        return cellString(row.getCell(idx));
    }

    private String cellString(Cell cell) {
        if (cell == null) {
            return null;
        }
        String text = dataFormatter.formatCellValue(cell);
        return text == null ? null : text.trim();
    }

    private Integer birthYear(String value) {
        if (value == null || value.length() < BIRTH_YEAR_LENGTH) {
            return null;
        }
        try {
            return Integer.valueOf(value.substring(0, BIRTH_YEAR_LENGTH));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * "2026-07-09 10:45:46.0" / "2026-07-09" 등에서 앞 10자리(yyyy-MM-dd)만 취해 날짜로 파싱한다.
     * 파싱 실패 시 null(등록은 되되 날짜는 비움).
     */
    private LocalDate date(String value) {
        if (value == null || value.length() < ISO_DATE_LENGTH) {
            return null;
        }
        try {
            return LocalDate.parse(value.substring(0, ISO_DATE_LENGTH));
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
