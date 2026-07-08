# 스태프 스케줄(staff_schedule) — 스키마 & API 설계

> 상태: **설계 단계** (2026-07-08). 테이블/역할은 반영 완료, REST API는 이후 회차에서 구현 예정.
> 원본 스키마: `src/main/resources/db/migration/V1__init.sql` (`staff_schedule`).

## 1. 배경

강사를 포함한 **모든 스태프 역할**(PM·PL·강사·진행자·상담사·행정)이 "강의 회차에 참여 가능한
날짜·시간대"를 프론트 **캘린더**에서 등록/수정/삭제한다. 강사는 **오전/오후**를 나눠 등록하거나
**둘 다** 가능으로 등록할 수 있다.

- `staff_schedule`은 **사용자(users) 기준의 일반 가용 일정 풀**이며 특정 강좌(course)에 종속되지 않는다.
- 실제 강좌 회차 배정은 기존 `course_staff`(course_id·user_id·staff_role·session_type)가 담당한다.

## 2. 역할(role) 변경 — PM·PL 신설

`role` 마스터(V3)에 2종을 추가하고 관련 enum을 확장했다.

| role_name | 설명 | 비고 |
|-----------|------|------|
| `PROJECT_MANAGER` | 프로젝트 매니저(PM) | 신규 |
| `PROJECT_LEADER` | 프로젝트 리더(PL) | 신규 |

- `role/entity/RoleName.java` : `PROJECT_MANAGER`, `PROJECT_LEADER` 추가.
- `coursestaff/entity/StaffRole.java` : 동일 2종 추가(강좌 스태프로도 배정 가능). `course_staff.staff_role`은 NVARCHAR라 DB 스키마 변경 없음.
- 기존 매핑: 강사=`LECTURER`, 진행자=`STAFF`(진행요원), 상담사=`COUNSELOR`, 행정=`OPERATOR`(행정허브).

## 3. 테이블 스키마 — `staff_schedule`

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `staff_schedule_id` | BIGINT IDENTITY | PK | |
| `user_id` | BIGINT | NOT NULL, FK→`users.user_id` | 등록 주체(스태프) |
| `schedule_date` | DATE | NOT NULL | 참여 가능 날짜 |
| `session_type` | NVARCHAR(50) | NOT NULL | 시간대: `AM`(오전)/`PM`(오후)/`FULL`(종일=오전+오후) |
| `is_available` | BIT | NOT NULL | 가용 여부(1=가능). 향후 불가/휴가 확장 대비 |
| `memo` | NVARCHAR(255) | nullable | 비고(사유 등) |
| `created_at` | DATETIME2 | nullable | |
| `updated_at` | DATETIME2 | nullable | |

- **UNIQUE** `UQ_STAFF_SCHEDULE_USER_DATE_SESSION (user_id, schedule_date, session_type)` — 동일인·동일 날짜·동일 시간대 중복 방지.
- **FK** `FK_STAFF_SCHEDULE_USERS (user_id) → users(user_id)`.
- `session_type`은 기존 `coursestaff/entity/SessionType`(`FULL/AM/PM`)을 재사용한다.
- "오전+오후 모두" = `session_type=FULL` 한 행(권장). `AM`·`PM` 별도 등록도 UNIQUE가 허용한다.

## 4. REST API 설계 (`/api/staff-schedules`)

공통: 응답 봉투 `ApiResponse<T>`(`success/data/error`), 인증 JWT Bearer, 권한 `@PreAuthorize`,
Swagger `@Tag("StaffSchedule")`. 목록은 `page/size/totalElements/totalPages` 페이지네이션 포맷.

| # | 메서드 · 경로 | 설명 | 권한(초안) |
|---|---------------|------|-----------|
| 1 | `POST /api/staff-schedules` | 단건 등록 | 인증 스태프(본인) / 타인 지정은 ADMIN·OPERATOR |
| 2 | `POST /api/staff-schedules/bulk` | 캘린더 다중선택 일괄 등록 | 상동 |
| 3 | `GET /api/staff-schedules` | 목록/월 범위 조회 | ADMIN·OPERATOR·HEAD_OFFICE·REGIONAL_MANAGER |
| 4 | `GET /api/staff-schedules/me` | 내 캘린더 조회 | 인증 스태프(본인) |
| 5 | `GET /api/staff-schedules/{id}` | 상세 조회 | 소유자 / 관리자 |
| 6 | `PUT /api/staff-schedules/{id}` | 수정 | 소유자 / ADMIN·OPERATOR |
| 7 | `DELETE /api/staff-schedules/{id}` | 삭제(하드) | 소유자 / ADMIN |

### 4.1 등록 — `POST /api/staff-schedules`
Request
```json
{
  "userId": 6,             // 생략 시 인증 사용자 본인
  "scheduleDate": "2026-08-18",
  "sessionType": "AM",     // AM | PM | FULL
  "isAvailable": true,      // 생략 시 true
  "memo": "오전만 가능"
}
```
Response
```json
{
  "success": true,
  "data": {
    "staffScheduleId": 12,
    "userId": 6,
    "scheduleDate": "2026-08-18",
    "sessionType": "AM",
    "isAvailable": true,
    "memo": "오전만 가능",
    "createdAt": "2026-07-08T14:20:00"
  },
  "error": null
}
```

### 4.2 일괄 등록 — `POST /api/staff-schedules/bulk`
```json
{
  "userId": 6,
  "entries": [
    { "scheduleDate": "2026-08-18", "sessionType": "FULL" },
    { "scheduleDate": "2026-08-19", "sessionType": "AM", "memo": "오후 회의" }
  ]
}
```
- UNIQUE 충돌 정책은 구현 시 확정(무시 skip vs upsert). 응답에 등록/스킵 건수 포함 권장.

### 4.3 목록/범위 조회 — `GET /api/staff-schedules`
Query: `userId`, `fromDate`, `toDate`, `sessionType`, `page`, `size` (모두 선택).
캘린더 월 단위 조회는 `fromDate`~`toDate`로 범위 지정. 응답은 목록 페이지네이션 포맷.

### 4.4 내 캘린더 — `GET /api/staff-schedules/me`
Query: `fromDate`, `toDate`. 인증 사용자 본인의 일정만 반환.

### 4.5 상세/수정/삭제 — `/{id}`
- `PUT`은 `sessionType`·`isAvailable`·`memo`만 수정(날짜/사용자 변경은 삭제 후 재등록 권장).
- 수정/삭제 시 서비스에서 **소유권 검증**(`userId == 인증사용자` 또는 관리자 역할).

## 5. 예외/에러 코드 (예정)

`common/ErrorCode`에 추가 예정:
- `STAFF_SCHEDULE_NOT_FOUND` (404) — 대상 일정 없음
- `DUPLICATE_STAFF_SCHEDULE` (400/409) — UNIQUE 충돌
- `INVALID_SESSION_TYPE` (400) — 잘못된 session_type (기존 코드 재사용 여지)

## 6. 이후 구현 패키지(예정)

```
com/jobmoa/hopefulreturn/staffschedule/
├── controller/StaffScheduleController.java
├── service/StaffScheduleService.java (+ Impl)
├── repository/StaffScheduleRepository.java
├── entity/StaffScheduleEntity.java
└── model/dto/ (Create/Bulk/Update Request, *Response)
```
- `SessionType`은 `coursestaff`의 것을 재사용하거나 공용 위치로 이동 검토.

## 7. 열린 항목
- bulk 등록 UNIQUE 충돌 정책(무시 vs upsert).
- `is_available`로 '불가/휴가'까지 표현할지(현재는 가용 등록 위주).
- PM/PL 한글 description 문구 최종 확정.
