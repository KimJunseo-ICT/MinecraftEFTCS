# Git-flow 관리 절차

저장소: `KimJunseo-ICT/MinecraftEFTCS`. 기본 개발 브랜치는 `develop`, 배포 기록 브랜치는 `main`입니다.

| 브랜치 | 시작점 | 병합 대상 | 용도 |
|---|---|---|---|
| `feature/<주제>` | `develop` | `develop` | 기능 추가·수정·일반 오류 수정 |
| `release/<버전>` | `develop` | `main`, 이후 `main → develop` | 버전 확정과 배포 준비 |
| `hotfix/<버전>` | `main` | `main`, 이후 `main → develop` | 배포 버전의 긴급 수정 |

## 작업과 업로드

1. `develop`을 가져온 뒤 기능 브랜치를 만듭니다. `main`·`develop`에 직접 커밋하거나 강제 푸시하지 않습니다.
2. 실제 수정 내용과 확인 여부를 `CHANGELOG.md`의 `Unreleased`에 함께 기록합니다. 게임 테스트는 사용자가 요청하거나 직접 확인한 경우만 완료로 기록합니다.
3. 수정 파일을 선별하여 커밋하고 작업 브랜치를 푸시합니다. 제목은 `feat:`, `fix:`, `docs:`, `chore:` 등으로 변경 목적을 설명합니다.
4. `feature/* → develop` PR에 변경 사항·검증 내용·남은 항목을 적고, `Git-flow policy` 검사를 통과한 뒤 **Create a merge commit**으로 병합합니다. squash/rebase 병합은 사용하지 않습니다.

```powershell
git switch develop
git pull --ff-only origin develop
git switch -c feature/example-change
# 수정 및 CHANGELOG.md 갱신
git add <수정한 파일>
git commit -m "feat: describe the change"
git push -u origin HEAD
```

## 버전 확정

1. `develop`에서 `release/1.0.0`처럼 버전 브랜치를 만듭니다.
2. `gradle.properties`의 `mod_version`을 같은 버전으로 지정합니다. `CHANGELOG.md`의 완료 항목을 `## [1.0.0] - YYYY-MM-DD`로 옮기고 `Unreleased`는 다음 작업용으로 남깁니다.
3. 사용자 게임 확인 결과를 기록합니다. 개발 스냅샷은 `1.0.0-alpha.1` 같은 사전 버전으로 구분합니다. 검증하지 않은 버전을 안정판이라고 기록하지 않습니다.
4. `release/1.0.0 → main` PR을 merge commit으로 병합합니다.
5. `main → develop` 동기화 PR도 병합하여 배포 준비 중 변경을 되돌려 반영합니다. `develop`을 `main`으로 reset하지 않습니다.
6. `main`의 해당 merge commit에 주석 태그를 붙입니다. 예: `git tag -a v1.0.0 <merge-sha> -m "Release 1.0.0"`, `git push origin v1.0.0`.
7. 태그 workflow가 버전 일치와 `main` 포함 여부를 확인하고 해당 변경 기록으로 GitHub Release를 만듭니다. 사전 버전은 prerelease로 표시합니다. 바이너리를 자동 첨부하거나 게임을 자동 실행하지 않습니다.

## 긴급 수정

`main`에서 `hotfix/1.0.1`을 만들고 수정·버전·변경 기록을 갱신합니다. `hotfix/1.0.1 → main`, `main → develop` PR 순서로 병합하고 `v1.0.1`을 같은 방식으로 기록합니다. 진행 중인 release 브랜치가 있으면 해당 수정도 반영합니다.

## 최초 가져오기

초기에는 저장소 운영 파일로 `main`을 만들고 `develop`을 분기합니다. `feature/initial-import`로 기존 프로젝트 전체를 가져와 PR로 `develop`에 통합한 뒤, `release/1.0.0-alpha.1 → main → develop` 순서를 적용합니다. 최초 등록 이전의 개발 대화를 가짜 개별 커밋으로 재구성하지 않습니다.

브랜치 보호는 main·develop의 PR 경유, 저장소 규칙 검사 통과, 강제 푸시·삭제 금지를 적용합니다. 단독 개발 계정이므로 자기 자신에게 불가능한 추가 승인 리뷰는 요구하지 않습니다. 보호 설정의 API 권한이 부족하면 적용 여부를 숨기지 않고 기록합니다.

2026-11-27 최초 등록에서는 GitHub 검사 성공 결과가 필수 검사 조건에 반영되지 않는 문제가 발생했습니다. 초기 가져오기·릴리스·동기화 PR에 한해서 실제 검사 결과와 로컬 검증을 확인한 후 필수 검사 연결 조건만 일시 해제하며, 초기 등록이 끝나면 복원합니다. 이 예외를 이후 업로드의 일반 절차로 사용하지 않습니다.

자동 실행이 시작되지 않으면 Actions의 `Repository policy`를 해당 브랜치에서 수동 실행할 수 있습니다. `Record version release`는 반드시 `v<버전>` 태그를 선택해 수동 실행합니다. 버전과 main 포함 여부가 맞지 않으면 Release를 만들지 않습니다. 검사가 대기 중인 상태를 통과로 기록하지 않습니다.
