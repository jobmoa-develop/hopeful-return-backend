# 수강 정보 수정 — 운영 필드 확장

> 브랜치 `feature-be-cp-operational-fields` · 이슈 [#66](https://github.com/jobmoa-develop/hopeful-return-backend/issues/66)
> 참여자관리 후속 작업 3건 중 **3번**(BE). DB 마이그레이션 없음(기존 course_participant 컬럼 사용).

## 수강 정보 수정 — `PUT /api/course-participants/{courseParticipantId}` (필드 확장)

기존 부분 수정(counselors·basicEducation·inflowType)에 **운영 필드**를 추가했다. null 필드는 미변경(기존 시맨틱 유지).

- 권한: `ADMIN, HEAD_OFFICE, REGIONAL_MANAGER, PROJECT_MANAGER, PROJECT_LEADER, OPERATOR`

**Request 필드**

| 필드 | 타입 | 설명 |
|------|------|------|
| `counselors` | List | 상담사 배정(제공 시 전체 교체, null 미변경) |
| `basicEducation` | String | 기초교육 이수 여부 |
| `inflowType` | String | 유입 경로 |
| `applyDate` | LocalDate | **(신규)** 신청일 (null 미변경) |
| `receptionDate` | LocalDate | **(신규)** 접수일 (null 미변경) |
| `contactAttempt` | Integer | **(신규)** 연락 시도 횟수 (null 미변경) |

**Request 예시**
```json
{
  "basicEducation": "N",
  "inflowType": "지인추천",
  "applyDate": "2026-08-10",
  "receptionDate": "2026-08-11",
  "contactAttempt": 5
}
```

**Response 200** — `{ "success": true, "data": { "updated": true }, "error": null }`

> 참여자 기본정보(이름·전화·출생연도) 수정은 기존 `PUT /api/participants/{participantId}` 사용(변경 없음).
> FE 정보 수정 모달은 두 API를 함께 호출한다(참여자 + 수강/운영).

## 테스트
- 서비스 단위: `CourseParticipantServiceImplTest` — 운영 필드 반영 + null(기초교육) 미변경 신규 1건. 전체 그린.
