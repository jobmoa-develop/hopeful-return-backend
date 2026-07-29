# 참여자 목록 회차 필터 확장 API

> 브랜치 `feature-be-round-filter` · 이슈 [#65](https://github.com/jobmoa-develop/hopeful-return-backend/issues/65)
> 참여자관리 후속 작업 3건 중 **2번**. DB 마이그레이션 없음.

## 참여자 목록 조회 — `GET /api/participants` (필터 확장)

기존 `name`/`phone` 서버 필터에 **회차(지역+회차번호)** 필터를 추가했다(모두 optional, 하위호환).

- 권한: `ADMIN, HEAD_OFFICE, REGIONAL_MANAGER, PROJECT_MANAGER, PROJECT_LEADER, OPERATOR, COUNSELOR, STAFF`

**쿼리 파라미터**

| 이름 | 타입 | 설명 |
|------|------|------|
| `name` | String | 참여자명 |
| `phone` | String | 전화번호 |
| `regionId` | Long | **(신규)** 지역 ID — 참여자 **최신 수강건** 기준 |
| `courseNumber` | Integer | **(신규)** 회차(course_number) — 최신 수강건 기준 |
| `page`, `size` | Integer | 페이지네이션 |

**필터 의미**
- 회차 필터는 참여자의 **최신 수강건(latest enrollment = courseParticipantId 최댓값)**의 지역/회차번호로 매칭한다
  (목록이 표시하는 값과 동일 기준). 최신 수강건이 없는 참여자는 회차 필터에 매칭되지 않는다.
- `regionId`/`courseNumber`가 모두 없으면 기존 빠른 경로(DB 페이지네이션 후 페이지 보강)로 동작한다.
- 회차 필터가 있으면 최신 수강건을 전체 계산 → 필터 → in-memory 페이지네이션하며,
  상담사·출결 요약은 **현재 페이지 참여자에 대해서만** 배치 조회해 N+1을 막는다.

## 테스트
- 서비스 단위: `ParticipantServiceImplTest` — 회차 신규 2건(regionId 매칭 · 매칭 없음) + 기존 회귀. 전체 그린.

## 변경 이력 (2026-07-29)
- **상위 지역 필터 `parentRegionId` 추가.** 상위 지역(서울/METROPOLITAN) 선택 시 산하 하위 지역 전체 회차를,
  하위 지역(양천/OPERATION)은 `regionId`로 해당 지역만 조회. 둘 다 오면 `regionId` 우선.
- 공용 헬퍼 `region.support.RegionResolver.resolveRegionIds(regionId, parentRegionId)`로 ID 목록을 해석해
  최신 수강건 회차 필터를 단일 비교 → 목록 `contains` 비교로 확장. (동일 헬퍼를 문자 발송 내역도 재사용.)
- 완전 삭제 안전장치: `DELETE /api/participants/{id}`(ADMIN)는 회차 등록 이력이 있으면
  `409 PARTICIPANT_HAS_ENROLLMENTS`로 차단(먼저 회차 등록 취소 필요).
