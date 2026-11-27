# YSM 테스트 실행과 모델 경로

## 이번 로그의 원인과 확인 범위

`run/logs/latest.log`의 2026-11-21 00:31 실행은 Microsoft Java 17.0.15를 정상 사용했지만, YSM 2.6.5의 내부 라이브러리를 초기화하는 `System.load`에서 `RuntimeException: err: 54`가 발생했습니다. 따라서 Java 선택이 적용되지 않은 문제나 모델 폴더 누락으로 설명할 수 없습니다. 모델 읽기보다 앞에서 실패합니다.

오류에 표시된 파일은 개발용으로 변환한 `ysm-forge-1.20.1-2.6.5_mapped_official_1.20.1.jar`입니다. 현재 증거상 개발용 변환 환경과 이 배포본의 내부 라이브러리 사이의 호환 문제로 판단합니다. `err: 54`만으로 YSM 내부의 정확한 실패 조건까지 단정할 수는 없습니다. Java 변경만으로 호환성이 해결됐다는 이전 안내는 정정합니다.

YSM 원본을 사용하는 일반 Forge 실행을 추가했습니다. YSM 내부 코드나 DLL은 수정하지 않았습니다. 기존 `runClient`에서는 이 오류가 반복되지 않도록 YSM 자동 로딩을 해제했습니다. 실험용 개발 로딩은 `-Pysm_userdev=true`로만 활성화됩니다.

## YSM과 Minecraft EFT를 함께 테스트

PowerShell에서 `D:\Codex\MinecraftModding\MinecraftEFT`로 이동한 뒤 실행합니다.

```powershell
.\gradlew17.bat --offline runYsmClient
```

현재 코드를 배포용 JAR로 빌드한 뒤, YSM·TACZ 원본 JAR과 함께 `run-ysm/mods`에 배치하고 일반 Forge 클라이언트를 실행합니다. 매 실행마다 최신 Minecraft EFT 빌드를 복사합니다. PC에 설치된 **Forge 47.4.10 / Minecraft 1.20.1** 프로필과 **Microsoft Java 17**을 사용하며, 프로젝트의 개발·컴파일 버전인 Forge 47.4.28은 유지합니다.

이 테스트는 새 게임 폴더 `D:\Codex\MinecraftModding\MinecraftEFT\run-ysm`을 사용하므로 기존 개발 월드는 수정하지 않습니다. 로그는 `run-ysm/logs/latest.log`에 생성됩니다.

게임을 열지 않고 빌드·파일 배치만 할 때는 다음 명령을 사용합니다.

```powershell
.\gradlew17.bat --offline prepareYsmClient
```

준비 작업과 Java의 `--dry-run`을 검증했습니다. 이는 게임이나 YSM 내부 라이브러리를 실제 실행한 검증이 아닙니다. `err: 54`가 일반 Forge 실행에서 해소되는지와 모델 표시 여부는 게임 실행 후 확인해야 합니다.

## 모델 위치

- 계속 사용할 모델 폴더: `D:\Codex\MinecraftModding\MinecraftEFT\run\config\yes_steve_model`
- 추가 모델: 위 폴더의 `custom` 하위 폴더에 `.ysm`, 모델 배포용 `.zip` 또는 폴더형 모델을 넣습니다.
- `run-ysm/config/yes_steve_model`은 위 폴더를 가리키는 Windows 폴더 연결이므로 두 경로에서 같은 모델을 사용합니다.
- 첨부 ZIP의 `builtin`과 `custom`은 그대로 보관했으며, 다른 환경의 캐시는 복사하지 않았습니다. 사용자 모델을 배포 JAR 안에는 포함하지 않습니다.

월드에 들어가 모델 로딩을 기다린 뒤 `Alt + Y`로 모델 선택 창을 엽니다. 실행 중 추가한 모델은 권한이 있는 상태에서 `/ysm model reload`로 다시 읽습니다.

## 기존 개발 실행

```powershell
.\gradlew17.bat --offline runClient
```

이 명령은 기존 ForgeGradle 개발 환경을 실행하며 **YSM을 로딩하지 않습니다**. 일반 개발과 디버깅에는 이 명령을, YSM과의 조합 확인에는 `runYsmClient`를 사용합니다.

참고: [YSM 설치·Java 지원 안내](https://yesstevemodel.github.io/en/wiki/install/), [모델 선택·추가 안내](https://yesstevemodel.github.io/en/wiki/faq/), [ForgeGradle 모드 의존성 안내](https://docs.minecraftforge.net/en/fg-6.x/dependencies/).
