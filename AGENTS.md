# Minecraft EFT 모델과 모션

## GitHub 및 버전 관리

- 저장소는 KimJunseo-ICT/MinecraftEFTCS이며 Git-flow를 따른다. 기본 작업은 develop에서 feature/<주제>를 만들어 진행한다. main/develop에 직접 작업 커밋하거나 강제 푸시하지 않는다.
- 매 업로드 전 CHANGELOG.md에 변경 내용과 확인 상태를 기록하고 작업 브랜치를 push한 뒤 develop 대상 PR로 통합한다. PR은 merge commit으로 병합한다.
- 버전 확정은 release/<버전> → main → develop, 긴급 수정은 main에서 hotfix/<버전> → main → develop을 따른다. gradle.properties의 mod_version, 태그 v<버전>, CHANGELOG의 버전이 일치해야 한다.
- 게임 테스트는 사용자가 직접 진행한다. 요청 없이 게임/GameTest/기능 테스트를 실행하지 않는다. 컴파일과 저장소 규칙 검사를 게임 테스트 완료로 기록하지 않는다.
- 캐시·실행 폴더·계정 정보·외부 모드 JAR은 커밋하지 않는다. .gitignore와 docs/git-flow.md를 따른다. 최초 이전의 작업을 가짜 과거 커밋으로 만들지 않는다.

## 모델과 모션 규칙

플레이어 팔이나 손을 사용하는 기능은 일반 스킨과 YSM 양쪽에서 동작해야 한다. YSM은 선택 의존성으로 두고, 전용 코드는 클라이언트에서 지원 버전과 모델 활성 상태를 확인한 뒤 호출한다.

모델 이름 목록 대신 현재 모델의 좌우 어깨·손 기준점을 읽는다. 모델을 교체하면 기준점을 다시 구하고, 기준점이 없는 팔은 일반 스킨 렌더링으로 대체한다. 주 손과 보조 손, 왼손잡이 설정을 함께 고려한다. 렌더링 방식은 치료 시간이나 효과를 바꾸지 않는다.
