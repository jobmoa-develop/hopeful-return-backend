# 참여자 관리 확장 — 상담 슬롯 3분화(V10) · 통합 등록 · 상세/목록 확장

> 브랜치 `feature-be-counseling-slots-v10` · 이슈 [#47](https://github.com/jobmoa-develop/hopeful-return-backend/issues/47)
> FE 참여자 메인·상세 개편에 필요한 BE 확장. FE 실연동은 별도 후속 작업.

---

## 1. V10 마이그레이션 (`V10__counseling_slots.sql`)

`course_participant_counselor` 테이블 변경:

| 항목 | 변경 전 | 변경 후 |
|------|---------|---------|
| `status` 값 | `PRE` / `POST` | `PRE_SESSION`(사전상담) / `POST_SESSION_1`(사후상담1) / `POST_SESSION_2`(사후상담2) |
| 유니크 제약 | `UQ_CPC_PARTICIPANT_COUNSELOR_STATUS` (수강건, 상담사, 슬롯) | `UQ_CPC_PARTICIPANT_STATUS` **(수강건, 슬롯)** — 슬롯당 상담사 1명 |
| 신규 컬럼 | — | `counseling_started_at` DATETIME2, `counseling_ended_at` DATETIME2, `counseling_memo` NVARCHAR(1000) |

- 기존 값 이관: `PRE→PRE_SESSION`, `POST→POST_SESSION_1` (같은 슬롯 중복은 최소 id만 유지).
- **상담 완료 판정 = `counseling_ended_at IS NOT NULL`** (별도 플래그 없음).
- 같은 상담사가 여러 슬롯(사전+사후1 등)을 맡는 것은 허용.

⚠️ **enum rename과 V10은 한 몸** — 구 바이너리 + 신 DB 조합 금지(기동 시 Flyway 자동 적용이라 단일 인스턴스에선 안전).
⚠️ FE가 구 값 `PRE`/`POST` 전송 시 400 `INVALID_STATUS` (별칭 없음, lockstep 배포).

## 2. 상담사 배정 (기존 API, 값 3분화)

`PATCH /api/course-participants/{id}/counselor` — HEAD_OFFICE, REGIONAL_MANAGER, OPERATOR

```json
{ "counselors": [
    { "counselorId": 7,  "status": "PRE_SESSION" },
    { "counselorId": 26, "status": "POST_SESSION_1" },
    { "counselorId": 7,  "status": "POST_SESSION_2" }
] }
```

- 전체 교체(하드 삭제 후 재삽입) 방식 유지.
- **같은 슬롯 중복 배정 → 400 `COUNSELING_SLOT_DUPLICATED`** (기존: 조용히 skip → 명시 오류로 변경).
- 참여자관리 메인의 상담사 편집도 이 API 재사용 (courseParticipantId는 목록의 `latestEnrollment`에서).

## 3. 상담 세션 기록 (신규)

`PATCH /api/course-participants/{courseParticipantId}/counselors/{counselingType}`
— HEAD_OFFICE, REGIONAL_MANAGER, OPERATOR, **COUNSELOR**

```json
// 요청 — null 필드는 기존값 유지(부분 수정), memo는 non-null일 때만 덮어씀
{ "startedAt": "2026-08-05T14:00:00", "endedAt": "2026-08-05T15:00:00", "memo": "사전상담 진행 완료." }

// 응답
{ "courseParticipantId": 10056, "counselingType": "PRE_SESSION",
  "counselorId": 7, "counselorName": "상담사1",
  "startedAt": "2026-08-05T14:00:00", "endedAt": "2026-08-05T15:00:00",
  "memo": "사전상담 진행 완료.", "completed": true }
```

| 오류 | 코드 |
|------|------|
| 슬롯에 배정된 상담사 없음 | 404 `COUNSELING_SLOT_NOT_FOUND` |
| 종료 < 시작, 또는 시작 없이 종료만 | 400 `INVALID_COUNSELING_TIME` |
| 잘못된 counselingType | 400 `INVALID_STATUS` |

> 후속 과제(이슈 #47 명기): COUNSELOR 역할의 본인 배정 건 제한(per-record ownership).

## 4. 통합 등록 (POST /api/participants 확장)

지역·회차를 함께 선택하면 participant + course_participant를 **한 트랜잭션**으로 생성.
수강 등록 실패(예: 강좌 없음) 시 참여자 저장도 **롤백**. `enrollment` 생략 시 참여자만 생성
(유입·자격 필드는 enrollment 안에만 존재 → 단독 등록 시 자연히 제외).

```json
// 요청
{ "name": "김철수", "birthYear": 1978, "phone": "010-5678-1234",
  "enrollment": {
    "courseId": 1, "inflowType": "워크넷",
    "applyDate": "2026-08-01", "receptionDate": "2026-08-02", "basicEducation": "Y",
    "counselors": [ { "counselorId": 7, "status": "PRE_SESSION" } ]
  } }

// 응답 (enrollment 없으면 courseParticipantId 미포함)
{ "participantId": 10070, "matchKey": "KCS_1978_1234", "courseParticipantId": 10056 }
```

- 통합 등록 시 course_participant **진행상태 = `CONFIRMED`(선정) 고정** (기존 단독 수강등록 API는 APPLIED 유지).

## 5. 상세 조회 확장 (GET /api/course-participants/{id})

참여자 상세 페이지의 기본 데이터. **조회 키는 courseParticipantId** — `matchKey`는 표시용 참여자ID.
추가된 필드: `matchKey`, `birthYear`, `phone`, `regionName`, `courseNumber`, `localCourseNumber`,
`inflowType`, `applyDate`, `receptionDate`, `completionDate`.
`counselors[]`(CounselorSummary)에 `startedAt`, `endedAt`, `memo`, `completed` 추가 — FE 여정(사전상담/사후1/사후2 완료) 파생용.

출결현황·조퇴/외출·메모는 기존 `/api/attendances`, `/api/attendance-leaves`, `/api/participant-memos`를 courseParticipantId로 호출.

## 6. 참여자 목록 확장 (GET /api/participants)

항목에 `matchKey`(표시용)와 `latestEnrollment`(최신 수강건 = 참여자별 max courseParticipantId, 없으면 null) 추가:

```json
{ "participantId": 10070, "name": "김철수", "birthYear": 1978, "phone": "010-5678-1234",
  "matchKey": "KCS_1978_1234",
  "latestEnrollment": {
    "courseParticipantId": 10056, "courseId": 1, "courseName": "양천 1기 희망리턴",
    "regionName": "양천", "courseNumber": 1, "localCourseNumber": 1,
    "status": "CONFIRMED", "completionDate": null,
    "counselors": [ { "counselorId": 7, "status": "PRE_SESSION", "completed": true, "...": "..." } ],
    "preCounselingCompleted": true, "attendedDays": 0, "totalCourseDays": 5 } }
```

- 배치 조회로 페이지당 총 4쿼리(페이지 → 수강건 fetch-join → 상담사 IN → 출결 grouped count) — N+1 없음.
- `attendedDays` = **ATTEND + LATE**(지각도 출석으로 집계), `totalCourseDays` = course의 day1~day5 중 지정된 날짜 수. 출결율은 FE가 계산.
- `preCounselingCompleted` = PRE_SESSION 슬롯의 종료일시 존재 여부(메인의 사전상담 칩용).

## 7. 검증 결과 (2026-07-15)

- 무DB `./gradlew build` BUILD SUCCESSFUL (유닛 전체 통과, IT는 DB 게이트 스킵).
- `DB_PASSWORD=... ./gradlew cleanTest test` — **146 tests / 0 failures / 1 skipped(기존 #15 @Disabled)**.
  기존 V7 회귀(IT seedCourse의 local_course_number 미세팅) 3개 파일도 이 브랜치에서 수정 → IT 전체 정상화.
- dev DB(58.151.241.130:14330) Flyway V10 적용 확인: `flyway_schema_history` v10 success, 신규 3컬럼, `UQ_CPC_PARTICIPANT_STATUS`.
- 실서버 스모크(bootRun local:3434, test_oper01): 통합 등록 → 상세(지역/matchKey) → 세션 기록(completed=true) → 목록(latestEnrollment·preCounselingCompleted=true) 모두 명세와 일치. 검증 데이터 정리 완료(participant/cp 0건).
