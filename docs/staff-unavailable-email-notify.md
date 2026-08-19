# 인력 근무불가 전환 메일 알림 (Staff Unavailable Email Notification)

배정된 인력이 **본인 배정된 회차의 날짜를 근무 가능→불가로 변경**하면, 인력배정 권한 관리자에게
이메일로 알린다. 별도 API 엔드포인트는 없다 — 기존 스케줄 수정 흐름에 부수 동작으로 붙는다.

## 트리거

- 엔드포인트: `PUT /api/staff-schedules/{staffScheduleId}` (기존 스케줄 수정)
- 발동 조건(모두 충족): `is_available` **true→false** 전이 **AND** 해당 `staff_schedule.course_staff_id != null`
  - `course_staff_id`는 인력이 특정 회차 날짜에 배정될 때(`CourseDailyStaffServiceImpl.save`) 세팅된다.
  - 순수 근무불가일(course_staff_id=null)은 알림 대상이 아니다.

### 왜 PUT 전이만 트리거인가 (409 정책 포함)

백엔드에서 `is_available=false`가 되는 경로는 3개뿐이다.

| 경로 | course_staff_id | 배정된 날짜? | 알림 |
|---|---|---|---|
| `POST /api/staff-schedules` (create, false) | 항상 null | 아니오 | 미발동 |
| `POST /api/staff-schedules/bulk` (false) | 항상 null | 아니오 | 미발동 |
| **`PUT /api/staff-schedules/{id}` (true→false)** | 유지(≠null 가능) | 예 | **발동** |

- `course_daily_staff` 배정은 `is_available=true`로만 세팅한다(false로 만드는 코드 없음).
- `UNIQUE(user_id, schedule_date, session_type)` 때문에 한 셀은 **"배정+가능" 또는 "미배정+불가"** 중 하나다.
- 따라서 **배정된 날짜에 신규 불가(POST create false)를 등록하면 `409 DUPLICATE_STAFF_SCHEDULE`로 막힌다(설계상 정상).** 배정된 날짜를 불가로 바꾸는 유일한 길은 **기존 행 PUT update(false)** 이며, 이때만 알림이 발동한다.
- 인력 본인도 자기 행이면 PUT 가능(소유권 통과). FE 개인 캘린더에서 배정 행을 불가로 전환하려면 POST가 아니라 **PUT을 호출**해야 한다.

> 참고: `GET /api/staff-schedules`·`/me` 응답 항목에 `courseStaffId`(nullable)가 포함된다. FE는 이 값으로 **배정된 행**을 구분해 "불가 전환(PUT) 시 관리자에게 알림" 안내를 띄우고, 신규 등록(POST)과 전환(PUT)을 올바르게 분기할 수 있다.

## 인력배정 자동 해제 (불가 전환 시)

배정된 날짜(`course_staff_id != null`)를 가능→불가로 전환하면, **삭제(DELETE)와 동일하게 그 날짜의
인력배정에서 빠진다.** `StaffScheduleServiceImpl.update()`가 전이를 감지하면 `course_staff_id`를
**null로 해제**한다(행 자체는 `is_available=false` 불가 표식으로 남긴다).

| 관점 | 결과 |
|---|---|
| 인력배정 목록(`CourseDailyStaffServiceImpl.findAll` = `findByCourseStaffIdIn`) | **빠짐**(course_staff_id=null) |
| 후보 제외(`...IsAvailableFalseAndCourseStaffIdIsNull`) | **반영됨**(그 날짜·시간대 불가로 정상 집계) |
| 관리자 메일 알림 | 정상 발행 — 엔티티는 이미 해제됐으므로 **해제 전 원래 `course_staff_id`를 캡처**해 이벤트에 담는다 |

> 삭제(행 완전 제거)와 달리 **불가 표식을 유지**하므로, 그 날짜가 후보 목록에서 다시 제안되지 않는다.
> `PUT /api/staff-schedules/{id}`(개인 캘린더)·관리자 일정 토글 **양쪽 모두** 같은 update() 경유로 동일 적용된다.

## 발송 대상

- 역할 **ADMIN · OPERATOR · REGIONAL_MANAGER** 전원(전역)
- 미삭제(`deleted != true`) 및 이메일 보유자만, 중복 제거
- REGIONAL_MANAGER 지역 스코프는 현재 스키마 미지원 → 전역 발송

## 메일 내용

| 항목 | 소스 |
|---|---|
| 제목 | `[hopeful-return] 인력 근무불가 알림 — {지역} {회차}회차 {날짜}` |
| 인력명 | `users.name` (전환 인력) |
| 지역 | `course.region.name` |
| 회차 | `localCourseNumber ?? courseNumber` (지역회차 우선) |
| 날짜 | `staff_schedule.schedule_date` |
| 시간대 | `session_type` → 오전/오후/종일 |
| 사유 | `staff_schedule.memo` |

## 처리 방식 (비동기)

1. `StaffScheduleServiceImpl.update()`가 전이를 감지하면 `StaffBecameUnavailableEvent`(불변 record)를 발행.
2. `StaffScheduleChangeNotificationService`가 `@TransactionalEventListener(AFTER_COMMIT)` + `@Async("notificationExecutor")`로 커밋 후 별 스레드에서 수신자별 발송.
3. 발송 실패는 로깅만(원 요청 무영향). 개별 수신자 실패도 나머지 발송을 막지 않는다.

## 발송 이력 (`staff_unavailable_notice`, V21)

수신자 1명당 1행 저장(감사·추적용).

| 컬럼 | 설명 |
|---|---|
| staff_schedule_id | 전환된 스케줄 행 |
| course_staff_id | 배정 연결(판정 근거) |
| staff_user_id | 불가로 전환한 인력 |
| recipient_user_id / recipient_email | 수신 관리자 / 발송 시점 이메일 스냅샷 |
| schedule_date / session_type | 불가로 바뀐 날짜 / 시간대 |
| send_status | SUCCESS / FAIL |
| sent_at / created_at | 발송·생성 시각 |

> 재전환(불가→가능→불가)은 정당한 신규 알림이므로 발송을 억제하지 않는다. 이력 테이블은 조회·감사 목적.

## 환경 변수 (SMTP)

`application.yml`의 `spring.mail`은 이미 배선됨. 발송하려면 `MAIL_USERNAME` / `MAIL_PASSWORD`
(필요 시 `MAIL_HOST` / `MAIL_PORT`) 환경변수 설정 필요. 미설정 시 발송은 실패하지만 스케줄 수정 자체는 정상 동작한다.
