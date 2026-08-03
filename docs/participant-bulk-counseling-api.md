# 참여자 관리 개선 — 일괄 수료 · 상담 스코프 · 검색 · 상담사 지정 API

> 브랜치 `feature-be-participant-bulk-counseling` · 이슈 [#53](https://github.com/jobmoa-develop/hopeful-return-backend/issues/53)
> 참여자 관리 화면 개편(기획)의 **BackEnd** 범위. FE(체크박스·모달·GNB·검색 UI·KST 시계)는 이 PR 머지 후 별도 이슈.
> DB 마이그레이션 없음 — 기존 테이블/컬럼(`course_participant`, `course_participant_counselor`, `course_staff`) 재사용.

---

## 1. 일괄 수료 처리 — `PATCH /api/course-participants/completion/bulk`

선택한 수강건들에 **동일한** 수료/미수료 상태·수료일·미수료 사유를 한 번에 적용한다. 단건 `PATCH /{id}/completion` 로직을 재사용한다.

- 권한: `ADMIN, HEAD_OFFICE, REGIONAL_MANAGER, PROJECT_MANAGER, PROJECT_LEADER, OPERATOR`
- 처리 중 존재하지 않는 id를 만나면 `COURSE_PARTICIPANT_NOT_FOUND` 예외로 **전체 롤백**(부분 반영 방지).
- `status` 는 `COMPLETED` / `INCOMPLETE` 만 허용 — 그 외 값은 조회 전에 `400 INVALID_STATUS`.

**Request**
```json
{
  "courseParticipantIds": [1, 2, 3],
  "status": "COMPLETED",
  "completionDate": "2026-08-24",
  "incompleteReason": null
}
```
- 미수료 시: `"status": "INCOMPLETE"`, `"incompleteReason": "출석 기준 미달"` (수료일은 무시 가능).

**Response 200**
```json
{ "success": true, "data": { "updatedCount": 3, "updatedIds": [1, 2, 3] }, "error": null }
```

---

## 2. 목록 조회 확장 — `GET /api/course-participants`

기존 목록 엔드포인트에 **검색 필터**와 **상담사 스코프**를 추가했다(하위호환 — 모든 파라미터 optional).

- 권한: `ADMIN, HEAD_OFFICE, REGIONAL_MANAGER, PROJECT_MANAGER, PROJECT_LEADER, OPERATOR, COUNSELOR, STAFF`

**쿼리 파라미터**

| 이름 | 타입 | 설명 |
|------|------|------|
| `courseId` | Long | 강좌 ID |
| `regionId` | Long | **(신규)** 지역 ID |
| `courseNumber` | Integer | **(신규)** 회차(course_number) |
| `status` | String | 수강 상태 |
| `keyword` | String | 참여자명/전화번호 검색 |
| `page`, `size` | Integer | 페이지네이션 |

**상담사 스코프(서버측 강제)**
- 인증 주체가 **COUNSELOR 롤만** 가진 경우, 본인이 배정된(3슬롯 PRE/POST1/POST2 중 하나) 수강건만 반환한다. FE에서 우회 불가.
- 관리자 롤(ADMIN·본부장·지역담당·PM·PL·OPERATOR)을 함께 가지면 스코프 제한 없음(전체 조회).
- 판정: 요청 사용자 id는 JWT 필터가 심은 `userId` 요청 속성, 롤은 `Authentication` authority(`ROLE_COUNSELOR` 유일 여부).

---

## 3. 배정 가능 상담사 조회 — `GET /api/course-participants/{courseParticipantId}/assignable-counselors`

해당 수강건의 **회차(course)에 인력 배치된 상담사** 목록을 반환한다(상담사 지정 드롭다운용).

- 권한: `ADMIN, HEAD_OFFICE, REGIONAL_MANAGER, PROJECT_MANAGER, PROJECT_LEADER, OPERATOR, COUNSELOR`
- 소스: `course_staff` 중 `staff_role = COUNSELOR` (`findByCourseIdAndStaffRole`).

**Response 200**
```json
{
  "success": true,
  "data": { "counselors": [ { "counselorId": 7, "name": "상담사1" } ] },
  "error": null
}
```

---

## 4. 상담 슬롯 상담사 지정 — `PATCH /api/course-participants/{courseParticipantId}/counselors/{counselingType}/counselor`

특정 상담 구분(`PRE_SESSION` / `POST_SESSION_1` / `POST_SESSION_2`)의 상담사를 지정·변경한다. **사전/사후 상담사가 이후 상담사를 지정**하는 흐름을 지원한다.

- 권한: `ADMIN, HEAD_OFFICE, REGIONAL_MANAGER, PROJECT_MANAGER, PROJECT_LEADER, OPERATOR, COUNSELOR`
- 검증
  1. **요청자 게이트**: COUNSELOR 롤만 가진 사용자는 **본인이 해당 참여자에 배정된 경우에만** 지정 가능 → 아니면 `403 FORBIDDEN_COUNSELOR_ASSIGN`. (관리자 롤은 게이트 통과)
  2. **대상 검증**: 지정 대상 `counselorId` 는 해당 회차에 인력 배치된(course_staff COUNSELOR) 상담사여야 함 → 아니면 `400 COUNSELOR_NOT_ASSIGNABLE`.
- 슬롯이 이미 있으면 상담사를 교체하고 **세션 기록(시작/종료/메모)을 초기화**한다(새 상담사 = 새 세션). 없으면 신규 배정.

**Request**
```json
{ "counselorId": 7 }
```

**Response 200** — 변경 후 슬롯 요약(`CounselorChangedResponse`, 기존 상담사 변경 API와 동일 형식)
```json
{
  "success": true,
  "data": {
    "courseParticipantId": 21,
    "counselors": [
      { "counselorId": 7, "counselorName": "상담사1", "status": "PRE_SESSION", "completed": false },
      { "counselorId": 7, "counselorName": "상담사1", "status": "POST_SESSION_1", "completed": false }
    ]
  },
  "error": null
}
```

---

## 5. 에러 코드

| 코드 | HTTP | 메시지 |
|------|------|--------|
| `INVALID_STATUS` | 400 | 유효하지 않은 상태값입니다. |
| `COURSE_PARTICIPANT_NOT_FOUND` | 404 | 수강 정보를 찾을 수 없습니다. |
| `FORBIDDEN_COUNSELOR_ASSIGN` | 403 | 해당 참여자에 배정된 상담사만 이후 상담사를 지정할 수 있습니다. |
| `COUNSELOR_NOT_ASSIGNABLE` | 400 | 해당 회차에 인력 배치된 상담사만 지정할 수 있습니다. |

## 6. 테스트

- 서비스 단위: `CourseParticipantServiceImplTest` (일괄 수료 5·배정가능 1·슬롯 지정 4 = 신규 9건 포함, 35건 그린).
- DB게이트 통합: `CourseParticipantApiIntegrationTest` (일괄 수료·스코프·배정가능·슬롯 지정 신규 8건, `DB_PASSWORD` 활성 시 실행).
- Postman 실호출은 dev DB 기동 후 별도 대조.
