# 상담사 일정 캘린더 API — `GET /api/counseling-schedules`

상담 시작일(`counseling_started_at`) 기준으로 전 회차·전 상담사의 상담 세션을 조회한다.
2026-08-13 확장으로 **상담사 본인의 근무 불가일(`unavailabilities`)** 을 함께 반환한다.

- **권한:** `ADMIN`, `OPERATOR`, `COUNSELOR`
- **정렬:** 세션은 `counseling_started_at` 오름차순, 불가는 `schedule_date, 상담사명` 오름차순.

## 쿼리 파라미터

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| `from` | date (YYYY-MM-DD) | 조회 시작일(포함). 미지정 시 당월 1일 |
| `to` | date (YYYY-MM-DD) | 조회 종료일(포함). 미지정 시 `from + 31일` |
| `regionId` | long | 하위 지역(예: 양천) 단일 |
| `parentRegionId` | long | 상위 지역(예: 서울) → 산하 하위 전체로 확장 |
| `courseNumber` | int | 전체회차(기수) — 전체 지역 조회 시 |
| `localCourseNumber` | int | 지역회차 — 지역 선택 조회 시 |
| `counselorName` | string | 상담사명 부분일치(LIKE) |

## 응답

```json
{
  "success": true,
  "data": {
    "schedules": [
      {
        "courseParticipantId": 101,
        "courseParticipantCounselorId": 500,
        "date": "2026-08-10",
        "startedAt": "2026-08-10T14:00:00",
        "endedAt": "2026-08-10T15:00:00",
        "regionName": "양천",
        "courseNumber": 3,
        "participantName": "홍길동",
        "counselorId": 7,
        "counselorName": "상담사1",
        "counselingType": "PRE_SESSION",
        "completed": true
      }
    ],
    "unavailabilities": [
      {
        "counselorId": 47,
        "counselorName": "이빛나라",
        "date": "2026-08-25",
        "sessionType": "FULL",
        "memo": null
      }
    ]
  }
}
```

### `unavailabilities` — 상담사 본인 근무 불가일

- **출처:** `staff_schedule` 테이블에서 `is_available=false` AND `course_staff_id IS NULL` 이고,
  `user_id` 가 **COUNSELOR 역할** 사용자인 행. (배정용 하드블록으로 등록된 근무 불가일을 재사용)
- **매칭:** `counselorId` = `users.user_id` = 세션의 `counselorId` 와 동일 키.
- **`sessionType`:** `AM`(오전) / `PM`(오후) / `FULL`(종일).
- **필터 상호작용(중요):** 불가일은 지역·회차 속성이 없어 해당 필터를 적용할 수 없다. 따라서
  **`regionId`·`parentRegionId`·`courseNumber`·`localCourseNumber` 중 하나라도 있으면 `unavailabilities` 는 빈 배열**로
  반환한다(특정 지역·회차로 좁혀진 뷰에서 전역 불가를 함께 노출하면 오해 소지). "전체 지역" 기본 뷰에서만 채워진다.
  `counselorName`(이름 LIKE)은 세션과 동일하게 불가일에도 적용된다.
- **날짜 경계:** 세션은 `[from 00:00, (to+1일) 00:00)` 시각 범위, 불가는 `schedule_date BETWEEN from AND to`(LocalDate inclusive).

## 구현 참조

- 쿼리: `StaffScheduleRepository.findCounselorUnavailabilities(from, to, counselorPattern, roleName)`
  — `exists` 서브쿼리로 COUNSELOR 역할 제한, `join fetch ss.user` 로 상담사명 N+1 방지.
- 조립: `CounselingScheduleServiceImpl.findSchedules(...)`.
- DTO: `CounselingScheduleResponse.UnavailabilityItem`.
- DB 변경 없음(기존 `staff_schedule` 재사용).
