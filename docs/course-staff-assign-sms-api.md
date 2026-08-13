# 인력 배정 안내 문자 발송 API — 백엔드

> 브랜치 `feature/be-course-staff-assign-sms` · 이슈 #147 (선행: notify_type 3종 V20 = #146)
> 인력 배정을 저장·수정할 때 변동된 담당자에게 **배정/변동/제외 안내 문자**를 발송하고, 이력을
> 기존 `course_staff_sms` 테이블에 기록한다. 공급자: **Naver Cloud SENS**(참여자 SMS 인프라 재사용).

## 1. 권한 모델

- 배정 페이지 권한이면 발송 가능: `@PreAuthorize("hasAnyRole('ADMIN','OPERATOR','REGIONAL_MANAGER')")`.
- 배정 저장(`PUT /api/course-daily-staffs/bulk`)과 **동일 권한**. 별도 `can_send_sms` 게이트 없음.

## 2. 알림 종류 (`StaffNotifyType`)

인력배정 3종을 추가(기존 `STATUS_CHANGE`/`SCHEDULE_CHANGE` 유지). DB CHECK 제약은 **V20(#146)** 에서 확장.

| notify_type | 발생 시점 | 기본 템플릿(토큰은 서버 치환) |
|---|---|---|
| `ASSIGN_NEW` | 최초 배정 | `[잡모아]\n{region} {round}회차({startDate}~) {role}으로 배정\n전산에서 확인 부탁드립니다` |
| `ASSIGN_CHANGED` | 배정 수정 - 추가/변동(부분 교체로 남은 이전 담당자 포함) | `[잡모아]\n{region} {round}회차({startDate}~) 인력변동\n전산에서 확인 부탁드립니다` |
| `ASSIGN_REMOVED` | 배정 수정 - 완전 제외 | `[잡모아]\n{region} {round}회차({startDate}~) 인력 제외\n전산에서 확인 부탁드립니다` |

- 토큰: `{region}`=지역명, `{round}`=지역회차(localCourseNumber, 없으면 courseNumber), `{startDate}`=개강일(day1Date) `M/d`(예: 8/18), `{role}`=배정 역할 라벨
  (COUNSELOR→상담사, LECTURER→강사, STAFF→진행자, PROJECT_MANAGER→PM, PROJECT_LEADER→PL, ADMIN_STAFF→행정인력).

## 3. 발송 `POST /api/course-staff-sms/send`

요청 — 알림 종류별 그룹으로 묶어 한 번에 발송:
```json
{
  "courseId": 15,
  "groups": [
    {
      "notifyType": "ASSIGN_CHANGED",
      "content": "[잡모아]\n{region} {round}회차({startDate}~) 인력변동\n전산에서 확인 부탁드립니다",
      "recipients": [ { "userId": 6 }, { "userId": 7, "phoneOverride": "01099998888" } ]
    },
    {
      "notifyType": "ASSIGN_REMOVED",
      "content": "[잡모아]\n{region} {round}회차({startDate}~) 인력 제외\n전산에서 확인 부탁드립니다",
      "recipients": [ { "userId": 9 } ]
    }
  ]
}
```
- `phoneOverride`: 이번 발송에만 적용(users.phone 미변경). 없으면 `users.phone` 사용.
- 전화번호가 없는 수신자(override·users.phone 모두 없음)는 **발송에서 제외**되고 응답 `skipped` 로 반환.
- SMS/LMS: 그룹 단위로 치환 후 최대 EUC-KR 바이트가 90 이하면 SMS, 초과면 LMS(공용 `SmsByteCalculator`). 2000B 초과는 `SMS_CONTENT_TOO_LONG`.
- SENS 한도(100건) → 그룹 내 100건 단위 배치. 발송 실패(`SMS_SEND_FAILED`)는 롤백 없이 해당 배치 `FAIL` 기록.

응답:
```json
{
  "success": true,
  "data": {
    "messageFormat": "SMS",
    "totalCount": 3,
    "successCount": 3,
    "failedCount": 0,
    "skipped": [ { "userId": 12, "name": "홍길동" } ]
  }
}
```

이력은 `course_staff_sms`(course_id, user_id=수신자, sent_by=발송자, notify_type, content=치환본문, send_status=SUCCESS/FAIL, sent_at)에 수신자별 1행 저장. 담당자 SMS는 **SUCCESS/FAIL 2상태**(참여자 SMS 와 달리 결과 폴링 없음).

## 4. 조회 `GET /api/course-staff-sms/history`

기존 유지(필터에 notify_type 배정 3종 포함 가능). 상세는 [participant/전역 발송내역](participant-sms-api.md) 패턴과 동일 페이지.

## 5. 배정 조회/후보 응답에 phone 추가

FE 모달 좌측(성명·전화·수정)·전화번호 없는 인원 경고를 위해 다음 응답에 `phone` 필드를 **추가(가산)**:
- `GET /api/course-daily-staffs` → `assignments[].phone`
- `GET /api/course-daily-staffs/candidates` → `candidates[].phone`

## 6. 재사용/신규 파일

- 재사용: `SmsService.send()`·`SmsSendCommand`(공급자 비의존), `PhoneNormalizer`, `RegionResolver`, `CourseStaffSmsRepository`.
- 신규: `sms/support/SmsByteCalculator`(byte/format 공용, 참여자 SMS도 위임), `SendCourseStaffSms{Request,Response}`, `CourseStaffSmsServiceImpl.send()`.
