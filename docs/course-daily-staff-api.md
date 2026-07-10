# 회차 날짜별 인력 배정 API (course-daily-staff)

> 회차(day1~day5)의 **날짜 × 역할 × 시간대(오전/오후/종일)** 단위로 배정 인력을 저장/조회한다.
> **`course_staff`(회차·역할·직원) + `staff_schedule`(날짜) 를 연결**해 처리한다 — `staff_schedule.course_staff_id` 추가.
> 관련: BE 이슈 **#36**, FE 이슈 **#13**. 마이그레이션 **V8**.

## 저장소 — `staff_schedule.course_staff_id` (V8)

`staff_schedule` 에 `course_staff_id BIGINT NULL`(FK→`course_staff`, `ON DELETE CASCADE`) 을 추가한다.
`is_available` 은 기존 의미(근무 가능 여부)를 유지한다.

| course_staff_id | is_available | 의미 |
| --- | --- | --- |
| NOT NULL | true | **배정** — 연결된 `course_staff`(회차·역할·session·직원)가 `schedule_date`에 근무 |
| NULL | false | 근무 불가일(사용자/실습생 캘린더) |
| NULL | true | 일반 가용 |

- 역할·session·회차는 연결된 `course_staff`가, 날짜·직원은 `staff_schedule`가 보유 → 조회 시 join으로 그리드 복원.
- `staff_schedule` UNIQUE(user_id, schedule_date, session_type) 유지 → 저장은 upsert(기존 행에 `course_staff_id` 부착).
- 배정 역할 enum `StaffRole`에 **`ADMIN_STAFF`(행정인력)** 추가. 행정인력은 user role `OPERATOR`(행정허브) 인력을 대상으로 한다.
- 강사 오전/오후는 `staff_role=LECTURER` + `session_type=AM|PM`로 구분. 상담사는 다중.
- `course_staff` 스키마 변경 없음. 기존 `/api/course-staffs`(코스 단위 담당자) API 동작 그대로.

## 엔드포인트 (`/api/course-daily-staffs`)

### 1) 날짜별 배정 조회 — `GET /api/course-daily-staffs?courseId={id}`
- 권한: 로그인 사용자(`isAuthenticated`)
- 응답:
```json
{
  "success": true,
  "data": {
    "courseId": 15,
    "assignments": [
      { "courseDailyStaffId": 42, "scheduleDate": "2026-06-23", "staffRole": "LECTURER",
        "sessionType": "AM", "userId": 6, "name": "이강사" }
    ]
  }
}
```

### 2) 그리드 저장 — `PUT /api/course-daily-staffs/bulk`
- 권한: `ADMIN, OPERATOR, REGIONAL_MANAGER`
- 동작: 해당 회차의 기존 배정을 **전량 삭제 후 재삽입**(그리드 단위 upsert). 중복 항목은 서버에서 제거.
- 요청:
```json
{
  "courseId": 15,
  "entries": [
    { "scheduleDate": "2026-06-23", "staffRole": "LECTURER", "sessionType": "AM", "userId": 6 },
    { "scheduleDate": "2026-06-23", "staffRole": "COUNSELOR", "sessionType": "FULL", "userId": 4 }
  ]
}
```
- 응답: `{ "success": true, "data": { "courseId": 15, "saved": 2 } }`
- 검증: 회차 미존재 → `COURSE_NOT_FOUND`, 배정 인력 미존재 → `USER_NOT_FOUND`, 잘못된 역할/시간대 → `INVALID_INPUT`.

### 3) 가용 후보 직원 조회 — `GET /api/course-daily-staffs/candidates?courseId={id}`
- 권한: `ADMIN, OPERATOR, REGIONAL_MANAGER`
- 동작: **배정 가능 역할(`user_role`→StaffRole 매핑, `OPERATOR`→`ADMIN_STAFF`)** 자격자와, 각자의 교육일 가용
  날짜를 반환한다. 해당 날짜에 **근무 불가일 행(`course_staff_id` NULL·`is_available=false`)** 이 있는 사람은 그 날짜에서 제외.
  (`availability.sessionType`은 날짜 가용 표시용 `FULL` 고정 — 셀 배치 세션은 역할이 결정.)
- 응답:
```json
{
  "success": true,
  "data": {
    "courseId": 15,
    "dates": ["2026-06-23", "2026-06-24"],
    "candidates": [
      { "userId": 6, "name": "이강사", "staffRoles": ["LECTURER"],
        "availability": [ { "scheduleDate": "2026-06-23", "sessionType": "FULL" } ] }
    ]
  }
}
```

## 연계 — 강좌 API (년도/회차/교육일 소스)

인력 배정 화면의 **년도 select·회차 select·날짜 열**은 강좌 목록/상세 API에서 조달한다. 이를 위해 `/api/courses` 응답에 다음을 추가(#37):
- `CourseListResponse.Item`·`CourseDetailResponse`에 **`year`**(=`day1_date`의 연도, `day1Date.getYear()` 파생·null 안전) + **`day1Date`~`day5Date`** 노출.
- 마이그레이션 불필요(교육일은 `course`에 이미 저장). `CourseServiceImpl.deriveYear(day1Date)` 매핑.
- FE는 `year`로 년도 그룹핑, `day1~day5`로 표 날짜 열 구성.

## 참고
- 코드: `com.jobmoa.hopefulreturn.coursedailystaff` (model.dto/service/controller). 저장은 `course_staff`+`staff_schedule`, 조회는 `staff_schedule.course_staff_id` join.
- 후보는 `user_role`/`role`(역할) + `staff_schedule` 불가일 조인. 캘린더(불가일 등록)는 FE 실습생 담당 — 불가일을 `is_available=false`로 등록.
- 단위 테스트: `CourseDailyStaffServiceImplTest`(7건).
