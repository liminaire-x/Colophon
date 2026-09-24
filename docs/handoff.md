# 인수인계 (2026-09-23~24 세션 → 다음 세션)

> 새 세션이 처음 읽는 문서다. 다른 문서에 있는 사실은 링크만 건다. 다음 세션이 내용을 흡수하면
> 지운다(같은 사실은 한 곳에만). 지난번 인수인계도 그렇게 지웠다.

## 1. 읽는 순서

1. `CLAUDE.md`(자동 로드): 설계 약속, 스택, 코드 구조, 워크플로우 요약
2. **이 문서**
3. [map.md](map.md): 개념 표와 두 이야기(밀 배달, 대장장이의 칼)
4. [roadmap.md](roadmap.md): 진행 상태, 작가 도구, 후보 목록
5. [workflow.md](workflow.md): 조각 흐름, 완료 정의, **게임 실행 환경**(두 클라이언트), 에셋 작업
6. [decisions/](decisions/): 0001~0007. 0001~0006 머리의 "이름 변경" 한 줄 참고

## 2. 이 세션에서 일어난 일

| 순서 | 한 일 | 결과 |
|---|---|---|
| 1 | **id 규칙** 재정의: `<종류>_<무작위 8자>`, 에디터만 부여·아무도 고치지 않음. 기록 키도 `종류_…`로 통일 | [0004](decisions/0004-ids-and-record-keys.md) |
| 2 | 첫 이야기 조각 4: 퀘스트 문서(`quests.json`), 공개, `J` 퀘스트 화면 | [0005](decisions/0005-quests.md) |
| 3 | 개발 환경: `runClient1`(Dev1)·`runClient2`(Dev2), 모두 `run/` 아래, 리소스팩 공유. `repo/`·maven-publish 제거 | [workflow.md](workflow.md) "게임 실행 환경" |
| 4 | 첫 이야기 조각 5: `Complete Quest`, 나갈 때 기록 저장 → **첫 이야기 완성** | 0005 "완료", 0001 |
| 5 | 두 번째 이야기 "대장장이의 칼": 보상 아이템 `/give` 문법(Gobber 모드 컴포넌트 포함), 처치형 목표, 사냥 보상(코드 없이 에디터만) → **완성** | [0006](decisions/0006-item-syntax.md), 0005 "처치형 목표" |
| 6 | 작가 도구: 에디터 ✋ 버튼(손에 든 아이템 → `/give` 한 줄), 제출형 목표를 `/clear` 조건으로(숨은 표식 `custom_data`로 위조 방지) | 0006 |
| 7 | Gradle 작업 `reset`(데이터를 백업 폴더로) | workflow.md |
| 8 | **이름 변경 Colophon → Lorebench**: 표시 이름, mod_id, 패키지·클래스, 명령어, 폴더, GitHub 저장소, 로컬 폴더 | [0007](decisions/0007-rename-lorebench.md) |

- 마지막 코드 커밋: `e9dbd9b`(이름 변경). CI 그린, **테스트 60개**, 산출물 `lorebench-1.0-SNAPSHOT.jar`.
- 이름 변경 후 게임 확인 완료(2026-09-24): 모드 목록, `/lorebench npc spawn`, 촌장 모델, `J` 화면, `run/config/lorebench/` 생성.
- 노드 9개: 트리거 `On Player Join`·`On NPC Interact` / 조건 `Has Flag`·`Quest State` / 행동 `Set Flag`·`Send Message`·`Reveal Quest`·`Complete Quest`·`Play NPC Animation`.

## 3. 사용자와의 약속과 일하는 방식 (가장 중요)

- `CLAUDE.md`의 설계 약속이 전부 유효하다. 특히 **선택지 + 추천 + 고치는 비용 → 결정은 사용자**.
- **사용자의 아이디어가 더 나은 경우가 많았다.** 이번 세션: id 접두사, 노드 id까지 접두사, id 편집 금지, 기록 키에서 `:` 제거, 목표의 엄격함 질문(→ 숨은 표식), 작업대 탭, 이름 변경. 선택지를 닫지 말 것.
- **설명이 안 통하면 비유와 구체적 장면으로 다시 쓴다.** "제출 가능이 아닌 상황"은 두 번 설명이 막혔고, **가게 계산대 비유**로 통했다. 다이어그램을 요청하기도 한다(`show_widget`).
- 개발 단계 원칙: 형식이 바뀌면 **변환 코드 대신 로컬 데이터 초기화**(메모리 `dev-reset-over-migration`). 첫 공개 뒤에는 형식 번호 + 변환.
- 사용자는 한국어, 커밋은 영어 conventional commits + `Co-Authored-By: Claude …` 줄.

## 4. 다음 작업: 작업대(workbench)로 에디터 나누기

### 배경 (사용자와 합의한 방향)

- 두 이야기를 만들며 **그래프가 커지는 문제**가 드러났다: NPC 하나가 퀘스트 여럿을 다루면 `Quest State` 안에 `Quest State`가 겹친다(map.md 그래프 C·D).
- 사용자 판단: 이건 기능이 아니라 **작가(에디터 사용자)의 UX 문제**이고, 플레이어 이야기만으로는 답이 안 나온다. → **작가 시나리오**를 두 번째 축으로 둔다.
- 작가 = 지금은 **사용자 본인**(Claude와 함께 개발부터 그래프 작성까지).
- 사용자 제안: 헤더에 **그래프 / 퀘스트 / 시네마틱** 같은 탭을 두어 **작업대를 분리**한다. 그래프 = 노드 배치와 연결, 퀘스트 = 퀘스트 흐름 작성, 시네마틱 = 카메라·애니메이션·대사 조합.

### 1단계: 헤더 탭 (사용자에게 제안함, 아직 "진행" 답은 받지 않음)

에디터만 바뀌고 새 기능은 없다(비용 쌈). 제안한 내용:
- 헤더에 **그래프 / 퀘스트 / NPC** 탭. 지금 왼쪽 목록에 섞인 셋을 탭별로 나눈다.
- 그래프 탭: 왼쪽 그래프 목록 + 노드 팔레트, 가운데 캔버스, 오른쪽 노드 설정(지금과 같음).
- 퀘스트 탭: 왼쪽 퀘스트 목록, **가운데 넓은 편집 화면**(지금은 좁은 오른쪽 칸). ✋·목표·보상 그대로.
- NPC 탭: 왼쪽 NPC 목록, 가운데 정의 + 배치 목록.
- Publish는 헤더에 그대로(세 문서 한 번에). 마지막 탭은 브라우저가 기억(localStorage, try/catch).
- 시네마틱 탭은 **이야기가 요구할 때** 만든다(빈 탭을 미리 두지 않음).
- 구현 메모: `editor/src/App.jsx`가 **690줄 한 파일**이다. 탭을 나누면서 탭별 파일로 쪼개는 게 자연스럽다(비용 쌈). 상태(graphs·npcs·quests)는 Publish 때문에 App에 남겨야 한다.
- 확인 방법: `npm run build` + 사용자 게임/브라우저 확인. Java 변경이 없으면 CI는 형식상 확인.

### 2단계: 퀘스트 작업대 설계 (비싼 결정 포함, 선택지로)

제안했던 경계(사용자 반응은 긍정적이었으나 확정 아님):

| 작업대 | 맡는 것 |
|---|---|
| 퀘스트 | 퀘스트의 **흐름**: 누가 주나, 누구에게 제출하나, 선행 퀘스트, 상태별 대사, 목표·보상 |
| 그래프 | 표준 흐름에 없는 **특별한 일**(예: 완료 시 폭죽, 애니메이션) |
| NPC | 정의, 배치, "이 NPC가 나오는 퀘스트·그래프"(쓰이는 곳 찾기) |
| 시네마틱(나중) | 시간 순 연출. 퀘스트나 그래프가 "재생"을 부름 |

- 핵심 아이디어: 그래프를 깊게 만드는 건 "NPC 클릭 → 퀘스트 상태 → 대사 → 공개/완료"라는 **반복되는 표준 흐름**이고, 이걸 퀘스트 작업대가 맡으면 그래프가 얕아진다.
- 앞서 낸 세 방향(참고용): **A** 퀘스트 대사를 콘텐츠로(퀘스트 문서에 제공자·제출처·선행·상태별 대사, NPC 클릭 시 자동 선택) / **B** "위에서부터 처음 맞는 것" 노드(겹침을 한 줄로) / **C** 그래프 재사용 조각. 작업대 방향은 A를 전용 화면까지 갖추는 쪽이다.
- **작가 시나리오 4개로 비교하기로 했다**:
  1. 퀘스트 30개짜리 마을에서 촌장 대사 한 줄을 고친다(어디를 여나?)
  2. "왜 Dev2에게 늑대 사냥이 안 열리지?" 원인을 찾는다
  3. 새 NPC에 퀘스트 3개를 순서대로 단다
  4. 퀘스트 하나를 지우려는데 어디서 쓰이는지 알고 싶다
- 정해야 할 비싼 것(선택지로 드릴 것): 퀘스트 문서에 늘어날 칸(제공 NPC, 제출 NPC, 선행 퀘스트, 상태별 대사)의 형식, NPC 클릭 때 여러 퀘스트 중 **무엇을 고르나**(우선순위 규칙), 그래프와의 연결(예: "퀘스트 완료 시" 트리거 노드), 기존 그래프 C·D를 어떻게 옮기나(개발 단계라 초기화 가능).
- 참고: 퀘스트 모드(FTB Quests 등)는 퀘스트 지도(카드 + 선행 화살표) 방식을 쓴다. 가져올지 여부는 시나리오로 판단.

## 5. 아직 확인하지 않은 것

- **기존 `minecraft:wheat` 같은 순수 id 목표**가 `/clear` 조건으로 바뀐 뒤에도 "밀이면 무엇이든"으로 세지는지(roadmap 작가 도구).
- **`reset` Gradle 작업을 실제로 실행**해 보기(CI는 설정 단계만 확인). configuration cache가 켜져 있어 `doLast` 안에서 `project`를 쓰지 않게 짰다.
- `gradle.properties` 1번째 줄 맨 앞에 **`6`이 들어가 있다**(`6# Sets default memory…`, 커밋 안 됨). 사용자가 실수로 입력한 것으로 보인다. 확인 후 되돌리기.

## 6. 로컬 상태와 함정

- **Java는 에이전트 환경에서 컴파일하지 않는다.** CI(`gh run view <id> --json conclusion` + 테스트 리포트 `test-reports/test/index.html`의 개수)와 사용자 IntelliJ로 확인한다.
- **커밋**: 사용자가 병행 편집한다. 커밋 전 `git diff --cached --stat`, 경로 지정 커밋(`git commit -- 파일`).
- **폴더와 저장소**: 로컬 `C:\Users\gntod\MyProjects\Intellij\LoreBench`, 원격 `https://github.com/liminaire-x/Lorebench.git`. Gradle 프로젝트 이름은 `settings.gradle`에서 `lorebench`로 고정.
- **legacy worktree** `../Colophon-legacy`(태그 `legacy-v2`, `5c3ca3e`): 폴더 이동 때 끊겨서 `git worktree repair`로 다시 이었다. 폴더를 또 옮기면 같은 조치.
- **메모리**: 옛 경로(`…-Intellij-Colophon\memory`)에서 새 경로(`…-Intellij-LoreBench\memory`)로 복사했다. 옛 폴더는 남아 있다.
- **게임 데이터(git 밖)**
  - 서버 `run/`: Lorebench 데이터 `run/config/lorebench/`. 옛 데이터는 `run/config/colophon/`(백업, 새 이름에선 안 읽힘)과 그 안의 `legacy-v2/`, `pre-0004/`, `backups/`.
  - 클라이언트 `run/client1/`, `run/client2/`. 로그는 각 `logs/`.
  - 테스트 리소스팩 `run/resourcepacks/lorebench-test/`(`assets/lorebench/…`, 원본 `source/chief.bbmodel`, `source/chief-texture.mjs`). 세 곳의 `options.txt`에 켜져 있다.
  - 모드 jar: `run/mods`와 `run/client1/mods`, `run/client2/mods`에 **Gobber**, **JEI**(사용자가 추가). 아이템을 더하는 모드는 클라이언트에도 있어야 접속된다.
- **Python**: 최상위 `.venv/Scripts/python.exe`(Bash의 `python`은 PATH에 없음).
- **API 확인 방법**: `build/moddev/artifacts/neoforge-21.1.249-merged.jar`에 마인크래프트 **`.java` 소스가 들어 있다**(`unzip -p … net/minecraft/…/X.java`). NeoForge 소스 jar는 `~/.gradle/caches/…/neoforge-21.1.249-sources.jar`. javap·strings는 없다.
- **셸 함정**: `sed` 치환에 `#`을 구분자로 쓰면 `#minecraft:logs`, `## 제목` 같은 내용과 충돌한다(`|` 구분자나 Edit 도구 사용). 따옴표 heredoc 안에서도 복잡한 치환은 실패하기 쉬우니 Edit 도구가 안전하다.
- **한국어 조사**: Lorebench(로어벤치)는 모음으로 끝난다 → "Lorebench**는/를/가/와**".
- 게임 쪽 문제는 **`run/logs/latest.log`부터**.
- 에디터 포트 8080 API에는 **인증이 없다**(`/api/publish`, `/api/held-item` 등). 외부에 열 때는 인증이나 포트 분리가 필요(roadmap 후보 "리소스팩 자동화"에 적어 둠).

## 7. 문서 규칙에서 어긋난 점 (다음 세션이 정리할 것)

- [README.md](README.md)는 결정 기록을 "한 번 쓰면 고치지 않고, 바꿀 땐 새 기록으로 대체"라고 한다. 그런데 이번 세션에서 0001·0002·0005·0006에 **섹션을 덧붙이거나 일부를 고쳤다**(예: 0005에 "완료", "처치형 목표", 0006에 "`/clear` 문법"). 규칙을 실제 방식(같은 주제의 확장은 덧붙이고, 뒤집는 결정은 새 기록)에 맞게 고칠지, 앞으로 새 기록으로 나눌지 사용자와 정한다.
