package com.jobmoa.hopefulreturn.courseparticipant.service;

import com.jobmoa.hopefulreturn.courseparticipant.model.dto.BulkImportCommitRequest;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.BulkImportPreviewResponse;
import com.jobmoa.hopefulreturn.courseparticipant.model.dto.BulkImportResultResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 참여자 XLSX 일괄 등록. 업로드 파일을 파싱해 교육과정명별로 미리보기를 제공하고(쓰기 없음),
 * 운영자가 확인·수정한 행 목록을 받아 지정된 회차에 등록한다(미매핑·중복·오류 행은 스킵).
 */
public interface ParticipantBulkImportService {

    /** 업로드 파일을 파싱해 교육과정명별 그룹·행을 미리보기로 반환한다(DB 쓰기 없음). */
    BulkImportPreviewResponse preview(MultipartFile file);

    /** 편집된 행 목록을 받아 참여자를 등록한다. 미매핑·중복·오류 행은 스킵하고 결과를 리포트한다. */
    BulkImportResultResponse commit(BulkImportCommitRequest request);
}
