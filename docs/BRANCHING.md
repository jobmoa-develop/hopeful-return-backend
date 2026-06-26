# 브랜치 전략

## 기본 브랜치
- `main` : 운영 배포 브랜치 (보호). 직접 push 금지.
- `develop` : 통합 개발 브랜치 (보호). 모든 feature PR 의 기본 타겟.

## 작업 브랜치
- `feature/<area>-<설명>` : 기능 개발. `develop` 에서 분기 → `develop` 으로 PR.
  - 예) `feature/be-auth-jwt`, `feature/be-email-verify`
- `fix/<설명>` : 버그 수정
- `release/<버전>` : 릴리스 준비
- `hotfix/<설명>` : 운영 긴급 수정 (`main` 분기)

## 규칙
1. `main`/`develop` 직접 push 금지 — 반드시 PR + 1인 이상 리뷰 승인 후 머지.
2. 착수 전 **이슈를 먼저 등록하고 본인을 assignee 로 지정**한다 (작업 겹침 방지).
3. 브랜치 접두사의 area(`be-`/`fe-` + 도메인)로 담당 영역을 분리한다.
4. PR 제목/본문에 `Closes #이슈번호` 를 포함한다.

## 협업 분담 (2인)
- 라벨 `area:*` + GitHub assignee 로 담당을 이중 표기한다.
- 같은 파일/도메인을 동시에 건드리지 않도록, 이슈 보드에서 in-progress 를 먼저 확인한다.
