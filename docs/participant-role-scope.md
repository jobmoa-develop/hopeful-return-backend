# 참여자관리 역할별 조회 스코프 (Role-based Read Scope)

> 참여자관리(수강생·참여자) 조회에서 로그인 사용자의 역할에 따라 볼 수 있는 데이터를
> **서버측에서 강제**한다(FE 우회 불가). 관련 이슈: BE #77.

## 스코프 규칙

| 역할 | 조회 범위 | 근거 테이블 |
|---|---|---|
| ADMIN / HEAD_OFFICE / REGIONAL_MANAGER / PROJECT_MANAGER / PROJECT_LEADER / **OPERATOR(행정인력)** | **제한 없음**(전체) | — |
| **COUNSELOR(상담사)** | 본인에게 **개별 배정된 상담 건**의 수강생만 | `course_participant_counselor` (counselor_id) |
| **STAFF(진행자)** | 본인이 **배정된 회차(course_staff)** 의 전체 참여자 | `course_staff` (user_id) → `course_participant` |

- 상담사·진행자를 함께 보유하면 **두 스코프의 합집합**.
- 관리자급 역할(OPERATOR 포함)을 하나라도 보유하면 제한이 없다.
- 배정이 없는 제한 사용자는 **빈 스코프**(아무것도 조회되지 않음).

## 적용 엔드포인트

| 메서드 | 경로 | 스코프 동작 |
|---|---|---|
| GET | `/api/course-participants` | 목록 — 스코프 밖 수강건은 결과에서 제외 |
| GET | `/api/course-participants/{id}` | 상세 — 스코프 밖이면 **403 ACCESS_DENIED** |
| GET | `/api/participants` | 목록 — 스코프 밖 참여자는 결과에서 제외 |
| GET | `/api/participants/{id}` | 상세 — 스코프 밖이면 **403 ACCESS_DENIED** |

> 상세 조회는 ID 직접 조회 우회를 막기 위해 목록과 동일 스코프를 강제한다.
> 사후관리(`/api/follow-ups`)는 상담사 전용 스코프를 유지한다(진행자 회차 스코프와 무관).

## 구현 구조

- `security/AuthScopeSupport`
  - `hasUnrestrictedScope(auth)` — 관리자급(OPERATOR 포함) 역할 보유 판정.
  - `hasRole(auth, "ROLE_STAFF")` — 단일 권한 보유 판정.
  - 기존 `isCounselorOnly(auth)` 는 쓰기 게이트(상담 배정/기록)에서 계속 사용.
- `courseparticipant/scope/ParticipantScopeResolver`
  - `resolve(authentication, userId)` → `ParticipantScope(courseParticipantIds, participantIds)`.
  - 두 집합이 모두 `null` 이면 제한 없음(관리자급).
- `courseparticipant/scope/ParticipantScope`
  - `allowsCourseParticipant(id)` / `allowsParticipant(id)` — 상세 조회 가드에 사용.
- 컨트롤러 경계에서 스코프를 계산해 서비스로 전달한다(서비스는 보안 타입을 알지 않음).

## 스코프 산정 쿼리

- STAFF: `courseStaffRepository.findByUserId(userId)` → courseId 집합 →
  `courseParticipantRepository.findByCourseIdIn(courseIds)`.
- COUNSELOR: `courseParticipantCounselorRepository.findByCounselorId(userId)` →
  courseParticipantId 집합 → `courseParticipantRepository.findAllById(...)`(참여자 id 역참조).
