# 참여자 XLSX 일괄 등록 API

정부식 참여자 엑셀(.xlsx)을 업로드해 참여자를 일괄 등록한다. 엑셀에는 내부 `courseId` 가 없고
회차가 **교육과정명(예: `[현장] (서울)리본(Re:Born)커리어_16회차`) + 지역명**으로만 표현되며,
정부식 광역 단위 회차와 내부 운영지역 단위 회차의 체계가 달라 **자동 매칭이 어렵다.** 따라서
2단계(미리보기 → 확인·수정 → 커밋)로 처리한다.

- 권한: `ADMIN, HEAD_OFFICE, REGIONAL_MANAGER, PROJECT_MANAGER, PROJECT_LEADER` (등록 = 관리 5롤)
- 파싱: 백엔드 Apache POI(`poi-ooxml`)

## 엑셀 컬럼 매핑 (헤더명 기준 — 열 순서 무관)

| 엑셀 헤더 | 사용 | 매핑 |
|---|---|---|
| `교육과정명` | 그룹 키·회차번호 추출 | `_(\d+)회차` → roundNumber |
| `교육기관소재지_시도` / `_시군구` | 지역 표시·추천 | sido / sigungu |
| `교육생명` | 참여자명 (필수) | participant.name |
| `생년월일` (yyyymmdd, 예 `19860313`) | 앞 4자리 | participant.birthYear |
| `휴대폰번호` | **정규화**(아래) | participant.phone |
| `신청일시` (`yyyy-MM-dd HH:mm:ss.S`) | 앞 10자리 | course_participant.applyDate |
| `선정일시` (`yyyy-MM-dd`) | 앞 10자리 | course_participant.receptionDate |
| `접수진행상태` + `선정여부` | 상태 결정 | 아래 규칙 |

**상태 매핑** (`접수진행상태`·`선정여부`): 접수취소 → `CANCELED`, `선정` → `CONFIRMED`,
`미선정` → `CANCELED`, 그 외(접수완료 등) → `APPLIED`.

**휴대폰 정규화** (`PhoneNormalizer`): 숫자만 추출 후 —
11자리(`01012345678`) 유지 · 13자 하이픈(`010-1234-5678`) → 11자리 · 10자리 선행 0 누락(`1012345678`) → `0` 접두.

- **미반영 필드:** 이메일·성별·수료여부/수료일시 (participant 스키마에 컬럼 없음, birthYear 만 사용).
- **필수 헤더 누락**(`교육과정명`·`교육생명`·`휴대폰번호`) 또는 비-xlsx/빈 파일 → `400 BULK_IMPORT_INVALID_FILE`.
- 행 검증 실패(교육생명·휴대폰번호 누락 등)는 **전체 실패가 아니라 해당 행만 오류 표시**.

## 1) 미리보기 — `POST /api/course-participants/bulk-import/preview`

- `multipart/form-data`, part `file`(.xlsx). **DB 쓰기 없음.**
- 응답: 교육과정명별 그룹 + 각 그룹의 회차번호·인원·오류수·추천 courseId(대개 null)·행 목록(`status` 포함).

```json
{ "success": true, "data": {
  "totalRows": 273, "validRows": 270, "invalidRows": 3,
  "groups": [
    { "sourceCourseName": "[현장] (서울)리본(Re:Born)커리어_16회차",
      "sido": "서울특별시", "sigungu": "서울특별시 양천구",
      "roundNumber": 16, "participantCount": 12, "invalidCount": 0, "suggestedCourseId": null,
      "rows": [ { "rowNumber": 1, "name": "홍길동", "phone": "01000000000",
                  "birthYear": 1986, "applyDate": "2026-07-09", "status": "CONFIRMED", "error": null } ] }
  ] } }
```

## 2) 커밋 — `POST /api/course-participants/bulk-import/commit`

- **`application/json`** — 미리보기 후 운영자가 확인·수정한 행 목록을 그대로 전송한다(파일 재전송 없음).
- 서버는 클라이언트 값을 **신뢰하지 않고 행마다 재검증**(전화 정규화·필수값·상태 파싱·회차 존재·중복).

```json
{ "items": [
  { "rowNumber": 1, "sourceCourseName": "[현장] (서울)...16회차", "targetCourseId": 1,
    "name": "홍길동", "phone": "01012345678", "birthYear": 1986,
    "applyDate": "2026-07-09", "receptionDate": "2026-07-09", "status": "CONFIRMED" }
] }
```

**등록 규칙**
- `targetCourseId` 없거나 미존재 → 미매핑 스킵. 이름/전화 누락 → INVALID 스킵.
- 참여자 **find-or-create**: `matchKey`(이니셜_생년_전화뒤4) → 전화 순으로 기존 참여자 재사용, 없으면 신규 생성.
- **중복 등록 스킵**: 같은 회차에 같은 참여자가 이미 있으면 스킵(`existsByCourseIdAndParticipantId`).
- 상태: 요청의 `status`(APPLIED/CONFIRMED/CANCELED), 미지정/오류 시 APPLIED.

```json
{ "success": true, "data": {
  "registeredCount": 250, "skippedDuplicateCount": 5, "skippedUnmappedCount": 15,
  "invalidRowCount": 3, "createdParticipantCount": 240, "reusedParticipantCount": 10,
  "details": [ { "rowNumber": 5, "name": "홍길동", "sourceCourseName": "[현장] (인천)...",
                 "outcome": "SKIPPED_UNMAPPED", "reason": "매핑된 회차가 없습니다." } ] } }
```

## 구현 위치
- 파서: `courseparticipant/support/ParticipantExcelParser.java`
- 전화 정규화: `courseparticipant/support/PhoneNormalizer.java`
- 서비스: `courseparticipant/service/ParticipantBulkImportService(+Impl).java`
- 컨트롤러: `CourseParticipantController#bulkImportPreview/bulkImportCommit`
- DTO: `courseparticipant/model/dto/BulkImport*.java`, `BulkImportCommitRequest.java`
- 리포지토리: `CourseParticipantRepository#existsByCourseIdAndParticipantId`

## 주의 (운영)
정부식 파일은 대개 다중 회차·다중 지역을 포함하며, 내부 DB에 없는 회차(예: 인천)는 미리보기에서
추천이 비고 매핑할 회차가 없으므로 **운영자가 회차를 먼저 생성한 뒤** 매핑해야 등록된다.
