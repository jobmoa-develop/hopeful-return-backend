# QR 기반 참여자 입·퇴실 공개 API

> 브랜치 `feature/be-qr-public-attendance` · 이슈 [#139](https://github.com/jobmoa-develop/hopeful-return-backend/issues/139)
> 참여자가 QR 을 스캔해 **로그인 없이** 입실/조퇴·외출/퇴실을 스스로 기록·조회한다. **DB 마이그레이션 없음**(기존 `attendance`/`attendance_leave` 재사용).

## 개요

- 기존 출결은 진행자(STAFF)가 인증 API 로 수기 입력했다. 이 기능은 참여자용 **공개(비인증) 플로우**를 추가한다.
- QR 링크 = 배포 FE origin + `/qr/{courseId}` (토큰 없음). 본인확인 = **성명 + 전화번호 뒤 4자리**.
- 모든 경로는 `SecurityConfig` 의 `PUBLIC_ENDPOINTS` 에 `"/api/public/qr/**"` 로 등록(permitAll).
- 응답은 공통 `ApiResponse<T>` 엔벨로프. 개인정보가 실리는 요청은 URL·로그 노출을 피해 **모두 POST**(랜딩만 GET).

## 본인확인 (성명 + 전화번호 뒤 4자리)

- `CourseParticipantRepository.findForQrVerify(courseId, name, last4)` 네이티브 쿼리로 DB 에서 매칭:
  `p.name = :name AND RIGHT(REPLACE(p.phone,'-',''),4) = :last4 AND cp.status <> 'CANCELED'`.
- 해당 회차 등록자 중 **정확히 1명**일 때만 통과. 0명(불일치)·2명 이상(동명이인 + 동일 뒷자리)이면
  어느 필드가 틀렸는지·매칭 수를 노출하지 않는 일반화 오류 `400 QR_VERIFICATION_FAILED`.

## 오늘 일차(dayNo) · 상태 머신

- `dayNo` = 오늘 날짜를 `course.day1Date..day5Date` 와 비교(1~5). 교육일이 아니면 `400 QR_NOT_CLASS_DAY`.
- 시간창: `educationStartTime` ~ `educationEndTime`(미설정 시 `COURSE_EDUCATION_*_TIME_NOT_SET`).
- **입실**: 교육 시작 + **10분(grace) 이내**면 `ATTEND`, **초과**면 `LATE` + [교육시작 ~ 실제 입실시각] 구간을
  외출(`attendance_leave`, `leave_time`=교육시작, `return_time`=입실시각, `reason`="지각(자동)")로 **자동 기록**.
- **조퇴/외출**: 입실 후 `attendance_leave.leave_time` 기록 = 조퇴, 이후 `return_time` 채우면 외출.
- **퇴실**: 교육 종료 시각 이후에만 가능(전이면 `400 QR_CHECKOUT_BEFORE_END`).
- 시각 소스는 `Clock` 빈(`ClockConfig`) 주입 — 테스트에서 고정 시계로 대체.

## 엔드포인트 (`/api/public/qr`, 모두 비인증)

| 메서드·경로 | 요청 본문 | 설명 |
|-------------|-----------|------|
| `GET  /courses/{courseId}` | – | 랜딩(비-PII): 지역명·전체/지역 회차번호·오늘 dayNo(교육일 아니면 null)·교육 시작/종료 시각 |
| `POST /courses/{courseId}/verify` | `{name, phoneLast4}` | 본인확인 + 오늘 상태(`QrStatusResponse`) |
| `POST /courses/{courseId}/check-in` | `{name, phoneLast4}` | 입실(10분 grace, 지각 시 자동 외출) |
| `POST /courses/{courseId}/leave` | `{name, phoneLast4, leaveTime}` | 조퇴(외출 시작) |
| `POST /courses/{courseId}/leave/return` | `{name, phoneLast4, attendanceLeaveId?, returnTime}` | 복귀(외출 종료). id 없으면 복귀 미기록 최신 건 |
| `POST /courses/{courseId}/check-out` | `{name, phoneLast4}` | 퇴실(교육 종료 이후) |
| `POST /courses/{courseId}/history` | `{name, phoneLast4}` | 본인 전 일차 내역(읽기전용) |

- `phoneLast4` 는 `@Pattern("\\d{4}")` 검증. `QrStatusResponse` 는 `canCheckIn`/`canLeave`/`canCheckOut`
  액션 플래그와 오늘 조퇴·외출 목록을 함께 반환해 FE 가 상태별 UI 를 렌더한다.

## 응답 예시 (verify / check-in)

```json
{"success":true,"data":{"participantName":"이유경","dayNo":1,
  "checkInTime":"13:16:02","checkOutTime":null,"status":"LATE",
  "leaves":[{"attendanceLeaveId":10001,"leaveTime":"10:00:00","returnTime":"13:16:02","reason":"지각(자동)"}],
  "canCheckIn":false,"canLeave":true,"canCheckOut":false}}
```

## 오류 코드

`QR_VERIFICATION_FAILED`·`QR_NOT_CLASS_DAY`·`QR_ALREADY_CHECKED_IN`·`QR_NOT_CHECKED_IN`·`QR_ALREADY_CHECKED_OUT`·`QR_CHECKOUT_BEFORE_END`(모두 400) — `common/ErrorCode` 에 추가, `GlobalExceptionHandler` 가 `ApiResponse.error` 로 매핑.

## 무결성·가드 (코드 리뷰 반영)

- **퇴실 이후 잠금**: 조퇴·복귀·퇴실은 `requireActiveAttendance`(입실 완료 + 미퇴실)를 통과해야 한다.
  이미 퇴실한 상태면 `QR_ALREADY_CHECKED_OUT`(재퇴실·퇴실 후 조퇴 방지).
- **복귀 덮어쓰기 방지**: `leave/return` 은 `attendanceLeaveId` 를 지정해도 **복귀 미기록(returnTime=null)** 건만 대상.
- **시간대 고정**: `ClockConfig` 는 `Clock.system(Asia/Seoul)` — UTC 컨테이너 배포에서도 dayNo·시각이 KST 기준.
- **입력 검증**: `name` 은 `@Size(max=50)`(participant.name 컬럼 길이), `phoneLast4` 는 `@Pattern("\\d{4}")`.
- **조퇴/복귀 시각은 참여자 입력값**(자기기록 UX) — 서버 시각으로 강제하지 않는다(스펙).

## 후속 과제 (이번 범위 밖)

- **레이트리밋**: 공개 본인확인은 전화 뒤4자리(1만 경우의수) 브루트포스 여지. courseId+정확한 성명+교육일 동시 필요라 저위험이나, 운영 시 IP 단위 레이트리밋(bucket4j 또는 Nginx) 권장.
- **동시 입실 UNIQUE 제약**: `attendance(course_participant_id, day_no)` 유니크 제약이 없어 동시 입실 레이스로 중복 행 가능(마이그레이션 필요). 자기기록·저트래픽이라 우선순위 낮음.
- **CORS/Swagger(prod)**: 앱 전역·기존 설정. 배포 시 FE 도메인 CORS 허용·운영 Swagger 비활성 필요(배포 세션).

## 테스트

- 서비스 단위: `attendance.qr.service.QrAttendanceServiceImplTest` — 비강의일 거부 / 본인확인 0·2명 실패 /
  입실 grace 이내 ATTEND(외출 없음) / grace 초과 LATE + 자동 외출(시작~입실) / 중복 입실 / 조퇴 생성 /
  복귀 return_time / 종료 전 퇴실 거부 / 종료 후 퇴실. 고정 `Clock` 으로 결정적 검증.
- 전체 `./gradlew test` 그린(327), 로컬 docker DB 대상 bootRun 라이브 시퀀스 검증 완료.

## 구성 파일

- 컨트롤러 `attendance.qr.controller.QrAttendanceController`
- 서비스 `attendance.qr.service.QrAttendanceService(Impl)`
- DTO `attendance.qr.model.dto.*`
- 본인확인 쿼리 `courseparticipant.repository.CourseParticipantRepository.findForQrVerify`
- 화이트리스트 `config.SecurityConfig` · 시계 `config.ClockConfig`
