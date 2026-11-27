# Minecraft EFT Creative System

AI Vibe Coding Minecraft Mod Project: Escape From Tarkov Creative System.

Minecraft **1.20.1 / Forge 47.4.28 / Java 17** 기반 개발 프로젝트입니다. 일반 Forge에서 YSM을 확인할 때는 설치된 Forge **47.4.10** 프로필을 사용합니다.

## 구현 범위

- 지정 무기·장비·군번줄 슬롯, 다중 칸 가방과 보안 컨테이너
- 개인 창고·무기 케이스, 회전 배치와 충돌 방지
- 독립 체력·에너지·수분·스태미나, 상태이상 목록과 HUD
- AI-2·IFAK 치료와 일반 스킨/YSM 팔 모션, 임시 부목·진통제
- 플레이어 시체, 양방향 루팅, 사망 당시 상태와 군번줄 사인 기록
- 탈수증·영양실조·탈진 및 스태미나 회복·재달리기 규칙

현재 버전은 개발 스냅샷입니다. 게임 내 확인은 사용자가 진행하며, 컴파일 완료와 게임 테스트 완료를 구분합니다.

## 개발 환경 복원

1. Java 17을 준비합니다. 이 PC에서는 `gradlew17.bat`가 `%USERPROFILE%\.jdks\temurin-17.0.21`을 사용합니다. 다른 Java 17 경로를 사용하는 PC에서는 `JAVA_HOME`을 설정한 뒤 `gradlew.bat`를 사용합니다.
2. TACZ `1.2.0-hotfix`와 YSM `2.6.5-forge+mc1.20.1` 원본 JAR을 준비합니다. 외부 모드 JAR은 이 저장소에 포함하지 않습니다.
3. 아래 명령으로 로컬 의존성을 배치합니다. 파일이 프로젝트 상위 폴더에 있으면 인수를 생략할 수 있습니다.

```powershell
.\tools\Install-LocalDependencies.ps1 -TaczJar '원본 TACZ.jar' -YsmJar '원본 YSM.jar'
.\gradlew17.bat compileJava
```

처음 복원한 PC는 의존성 다운로드를 위해 온라인으로 실행합니다. 이 PC처럼 캐시가 준비되어 있으면 `--offline`을 사용할 수 있습니다.

```powershell
.\gradlew17.bat --offline compileJava prepareYsmClient
.\gradlew17.bat --offline runYsmClient
```

`runYsmClient`는 이 PC의 설치된 CurseForge/Minecraft Java·Forge 프로필을 사용합니다. YSM 모델은 `run/config/yes_steve_model`에 별도로 준비합니다. 세부 사항은 [YSM 개발 환경](docs/ysm-development.md)을 참고합니다. 일반 개발 실행은 `runClient`입니다.

## 버전 관리

`develop → feature/* → develop → release/* → main → develop` 순서로 관리합니다. 배포 버전의 긴급 수정은 `main → hotfix/* → main + develop`을 사용합니다. PR은 merge commit으로 병합합니다.

- [Git-flow 작업 절차](docs/git-flow.md)
- [전체 변경 기록](CHANGELOG.md)
- [치료 도구](docs/ai2-medikit.md), [인벤토리](docs/custom-inventory.md), [상태이상](docs/status-effects.md)

`gradle.properties`의 `mod_version`, 버전 태그 `v<버전>`, 변경 기록의 버전 제목을 맞춥니다. 버전별 변경 내역은 태그 생성 시 GitHub Release에도 기록됩니다. PR의 저장소 규칙 검사에서는 게임이나 GameTest를 실행하지 않습니다.

## 저장하는 파일

소스·사용 리소스·개발 문서·Gradle wrapper·도구와 로컬 Maven POM을 관리합니다. 실행 인스턴스, 세이브, 계정 정보, YSM 모델 모음, Gradle 캐시, 외부 모드 JAR과 생성된 매핑은 로컬에 둡니다. 제외 파일을 삭제하는 작업은 수행하지 않습니다.

프로젝트 메타데이터의 라이선스는 `All Rights Reserved`입니다. 기존 `LICENSE.txt`·`CREDITS.txt`는 Forge MDK의 고지이며, 외부 모드·참고 자료의 권리를 이 저장소가 변경하지 않습니다.
