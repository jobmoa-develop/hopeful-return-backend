# 참여자관리 문자(SMS) 서비스 API — 백엔드

> 브랜치 `feature-be-participant-sms` · 이슈 #81
> 참여자관리에서 선택 참여자에게 SMS/LMS/MMS를 일괄 발송하고, 개인/공용 템플릿을 관리한다.
> 공급자: **Naver Cloud SENS SMS API v2**(실연동). 문자 기능은 **계정 단위 문자 발송 권한** 보유 계정만 사용.

## 1. 권한 모델 (계정 단위 플래그)

- `users.can_send_sms`(BIT, V14) 플래그로 제어. 페이지권한 방식과 무관.
- 로그인 시 JWT에 `canSendSms` 클레임 → 필터에서 `SMS_SEND` authority로 매핑 → 문자 API는 `@PreAuthorize("hasAuthority('SMS_SEND')")`.
- 로그인/내 정보 응답(`LoginResponse.user.canSendSms`, `MeResponse.canSendSms`)으로 FE가 버튼 노출 판단.

### 권한 부여 (관리자 전용)
`PATCH /api/users/{userId}/sms-permission` — `hasRole('ADMIN')`
```json
{ "canSendSms": true }
```

## 2. 문자 템플릿 CRUD — `/api/sms-templates` (권한: `SMS_SEND`)

- 공개 범위 `scope`: `PERSONAL`(본인 전용) / `SHARED`(공용). 목록은 **공용 전체 + 본인 개인**.
- 개인 템플릿 수정·삭제는 소유 계정만(아니면 403 ACCESS_DENIED).

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/api/sms-templates` | 등록. `{ scope, title, content }` |
| GET | `/api/sms-templates` | 목록(SHARED + 본인 PERSONAL) |
| GET | `/api/sms-templates/{id}` | 상세 |
| PUT | `/api/sms-templates/{id}` | 수정 `{ title, content }` |
| DELETE | `/api/sms-templates/{id}` | 삭제 |

## 3. 문자 발송/이력 — `/api/participant-sms` (권한: `SMS_SEND`)

### 발송 `POST /api/participant-sms`
```json
{
  "courseParticipantIds": [101, 102],
  "title": "수료 안내",
  "content": "{name}님, 수료를 축하합니다.",
  "messageFormat": "LMS",
  "images": ["<base64>"]
}
```
- `{name}` → 수신자 성명 치환(수신자별 개별 본문).
- **형식 자동 판별(서버 방어적 재확정)**: 이미지 있으면 `MMS`, 없으면 치환 후 바이트(EUC-KR)로 `SMS`(≤90) / `LMS`(≤2000).
- **바이트 상한**: 본문 초과(>2000) → 400. 제목(LMS/MMS)은 ≤40바이트.
- **일괄 100건 배치**: SENS `messages` 한도(100건)마다 분할 발송.
- 응답: `{ messageFormat, totalCount, successCount, failedCount, statusName, smsIds }`.

### 이력 조회
| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/participant-sms?courseParticipantId=101` | 수강생별 발송 이력(최신순). 모든 `SMS_SEND` 계정에 전체 노출(발송자 무관) |
| GET | `/api/participant-sms/{smsId}` | 발송 상세(+ imageUrls) |
| GET | `/api/participant-sms/history` | **전역 발송내역(페이지·필터)** — 아래 참조 |

### 전역 발송내역 조회 `GET /api/participant-sms/history`
- 쿼리 파라미터(전부 선택): `keyword`(수신자명/전화), `sendStatus`(SUCCESS/FAIL/PENDING),
  `courseNumber`(회차번호), `regionId`, `sentDateFrom`·`sentDateTo`(YYYY-MM-DD, **종료일 포함**), `page`(0-base), `size`(≤100, 기본 10).
- **역할 스코프(서버 강제)**: `ROLE_ADMIN`·`ROLE_HEAD_OFFICE` → 전체 발송내역.
  그 외 계정 → **본인 발송분만**(`sentBy=로그인 userId`). 클라이언트가 `sentBy`를 지정할 수 없다.
- 정렬: `sentAt` 내림차순. 잘못된 `sendStatus` 값 → 400 INVALID_INPUT.
- 응답:
```json
{
  "content": [{
    "smsId": 501, "courseParticipantId": 101, "participantName": "홍길동", "phone": "01012345678",
    "regionName": "양천", "courseName": "양천5기", "courseNumber": 5,
    "messageFormat": "LMS", "title": "수료 안내", "content": "홍길동님, 수료를 축하합니다.",
    "sendStatus": "SUCCESS", "sentAt": "2026-07-24T15:20:10", "senderName": "관리자"
  }],
  "page": 0, "size": 10, "totalElements": 52, "totalPages": 6
}
```

## 4. SENS 실연동

- 설정(application.yml, 값은 `.env`): `sens.enabled/access-key/secret-key/service-id/from`.
- `sens.enabled=false`(기본) → `NoOpSmsService`(로그만). `true` → `SensSmsService`(실발송).
- 발송: `POST https://sens.apigw.ntruss.com/sms/v2/services/{serviceId}/messages`.
- 서명 `x-ncp-apigw-signature-v2` = `"POST {url}\n{timestamp}\n{accessKey}"` → HMAC-SHA256(secretKey) → Base64.
- **MMS 첨부**: `POST .../files`로 base64 업로드(`fileName`/`fileBody`) → `fileId` → 발송 `files[{fileId}]`.
  - 제약(서버 검증): **jpg/jpeg만**, 파일당 **0~300KB**, 해상도 **최대 1500×1440**, `data:image/...;base64,` 접두어 제거. `fileId`는 SENS 보관 6일.

## 5. 등록일(전산 등록일) 필터

- `GET /api/course-participants`에 `registerDateFrom` / `registerDateTo`(YYYY-MM-DD, 포함) 추가.
- **`GET /api/participants`에도 동일 파라미터 추가**(참여자관리 문자 화면용). 기준 = **최신 수강건의 `course_participant.created_at`**. 최신 수강건이 없는 참여자는 등록일 필터 적용 시 제외.
- 기준 = `course_participant.created_at`(레코드 시스템 등록 시각). 스키마 무변경.

## 6. DB (V11 기존 + V14 신규)

- V11: `sms_template`, `participant_sms.message_format`·`content(2000)`, `participant_sms_image`.
- V14: `users.can_send_sms BIT NOT NULL DEFAULT 0`(ADMIN 기본 부여). `ALTER ADD` 후 `GO` 배치 분리.

## 7. 검증

- `./gradlew test` — 253 tests GREEN(무DB 통합 스킵). 단위 테스트: `{name}` 치환·SMS/LMS/MMS 판별·2000B 초과 거부.
- SENS 실발송(실 Key 필요, 비용 발생)은 별도 승인 후 최소 건수로 확인.
