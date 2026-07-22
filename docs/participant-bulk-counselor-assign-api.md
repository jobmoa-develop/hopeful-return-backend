# 상담 슬롯 상담사 일괄 배정 API

> 브랜치 `feature-be-bulk-assign-counselor` · 이슈 [#64](https://github.com/jobmoa-develop/hopeful-return-backend/issues/64)
> 참여자관리 후속 작업 3건 중 **1번**. 단건 슬롯 지정
> (`PATCH /api/course-participants/{id}/counselors/{type}/counselor`)의 일괄 버전.
> DB 마이그레이션 없음 — 기존 `course_participant_counselor` 재사용.

---

## 상담 슬롯 상담사 일괄 배정 — `PATCH /api/course-participants/counselors/bulk`

참여자관리에서 다건 선택한 수강건들에 **동일 상담 구분(슬롯)의 동일 상담사**를 한 번에 지정한다.
단건 슬롯 지정의 슬롯 upsert 로직(`upsertSlotCounselor`)을 재사용하며, 일괄 수료
(`PATCH /completion/bulk`)와 동일한 all-or-nothing 트랜잭션 패턴을 따른다.

- 권한: `ADMIN, HEAD_OFFICE, REGIONAL_MANAGER, PROJECT_MANAGER, PROJECT_LEADER, OPERATOR`
  (관리 롤 전용 — COUNSELOR 제외. 단건 지정의 COUNSELOR 체인 검증은 적용하지 않는다.)
- 대상 검증: 지정 상담사(`counselorId`)는 **각 수강건 회차에 인력 배치된**(course_staff COUNSELOR)
  상담사여야 한다. 한 건이라도 불충족 시 `400 COUNSELOR_NOT_ASSIGNABLE` 로 **전체 롤백**.
- 없는 수강건을 만나면 `404 COURSE_PARTICIPANT_NOT_FOUND` 로 전체 롤백(부분 반영 방지).
- 슬롯이 이미 있으면 상담사를 교체하고 **세션 기록(시작/종료/메모)을 초기화**한다(새 상담사 = 새 세션).
- `counselingType` 은 `PRE_SESSION` / `POST_SESSION_1` / `POST_SESSION_2` 만 허용 —
  그 외 값은 조회 전에 `400 INVALID_STATUS`.

**Request**
```json
{
  "courseParticipantIds": [1, 2, 3],
  "counselingType": "PRE_SESSION",
  "counselorId": 12
}
```

**Response 200**
```json
{ "success": true, "data": { "updatedCount": 3, "updatedIds": [1, 2, 3] }, "error": null }
```

---

## 에러 코드

| 코드 | HTTP | 메시지 |
|------|------|--------|
| `INVALID_STATUS` | 400 | 유효하지 않은 상태값입니다. |
| `COUNSELOR_NOT_ASSIGNABLE` | 400 | 해당 회차에 인력 배치된 상담사만 지정할 수 있습니다. |
| `COURSE_PARTICIPANT_NOT_FOUND` | 404 | 수강 정보를 찾을 수 없습니다. |

## 테스트

- 서비스 단위: `CourseParticipantServiceImplTest` — 일괄 배정 신규 4건
  (정상 일괄 반영 · 회차 미배치 상담사 롤백 · 없는 수강건 롤백 · 잘못된 슬롯값). 전체 그린.
- HTTP 통합: `CourseParticipantApiIntegrationTest` — 일괄 배정 신규 2건
  (정상 200 · 미배치 상담사 400), `DB_PASSWORD` 활성 시 실행.
