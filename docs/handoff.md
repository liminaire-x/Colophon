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
- 개발 단계 원칙(변환 대신 초기화)은 `CLAUDE.md` "설계 약속"에 있다.
- 사용자는 한국어, 커밋은 영어 conventional commits + `Co-Authored-By: Claude …` 줄.

## 4. 다음 작업: 작업대(workbench)로 에디터 나누기

[roadmap.md](roadmap.md) "작업대: 에디터 나누기"로 옮겼다. 사용자 원래 제안은 헤더에 **그래프 / 퀘스트 /
시네마틱** 같은 탭(그래프 = 노드 배치와 연결, 퀘스트 = 퀘스트 흐름 작성, 시네마틱 = 카메라·애니메이션·대사 조합)이었다.

## 5. 아직 확인하지 않은 것

- **기존 `minecraft:wheat` 같은 순수 id 목표**가 `/clear` 조건으로 바뀐 뒤에도 "밀이면 무엇이든"으로 세지는지(roadmap 작가 도구).
- **`reset` Gradle 작업을 실제로 실행**해 보기(CI는 설정 단계만 확인). configuration cache가 켜져 있어 `doLast` 안에서 `project`를 쓰지 않게 짰다.

## 6. 로컬 상태와 함정

- **Java는 에이전트 환경에서 컴파일하지 않는다.** CI(`gh run view <id> --json conclusion` + 테스트 리포트 `test-reports/test/index.html`의 개수)와 사용자 IntelliJ로 확인한다.
- **커밋**: 사용자가 병행 편집한다. 커밋 전 `git diff --cached --stat`, 경로 지정 커밋(`git commit -- 파일`).
- **폴더와 저장소**: 로컬 `C:\Users\gntod\MyProjects\Intellij\LoreBench`, 원격 `https://github.com/liminaire-x/Lorebench.git`. Gradle 프로젝트 이름은 `settings.gradle`에서 `lorebench`로 고정.
- **legacy worktree** `../Colophon-legacy`(태그 `legacy-v2`, `5c3ca3e`): 폴더 이동 때 끊겨서 `git worktree repair`로 다시 이었다. 폴더를 또 옮기면 같은 조치.
- **메모리는 쓰지 않는다**: 2026-09-24 메모리 내용을 `CLAUDE.md`로 합치고 메모리 폴더를 비웠다(목차 한 줄만). 옛 경로 `…-Intellij-Colophon\memory` 폴더는 남아 있다.
- **게임 데이터(git 밖)**
  - 서버 `run/`: Lorebench 데이터 `run/config/lorebench/`. 옛 데이터는 `run/config/colophon/`(백업, 새 이름에선 안 읽힘)과 그 안의 `legacy-v2/`, `pre-0004/`, `backups/`.
  - 클라이언트 `run/client1/`, `run/client2/`. 로그는 각 `logs/`.
  - 테스트 리소스팩 `run/resourcepacks/lorebench-test/`(`assets/lorebench/…`, 원본 `source/chief.bbmodel`, `source/chief-texture.mjs`). 세 곳의 `options.txt`에 켜져 있다.
  - 모드 jar: `run/mods`와 `run/client1/mods`, `run/client2/mods`에 **Gobber**, **JEI**(사용자가 추가). 아이템을 더하는 모드는 클라이언트에도 있어야 접속된다.
- Python, graphify, API 확인 방법, 셸 함정은 `CLAUDE.md` "도구 환경"으로 옮겼다.
- **한국어 조사**: Lorebench(로어벤치)는 모음으로 끝난다 → "Lorebench**는/를/가/와**".
- 게임 쪽 문제는 **`run/logs/latest.log`부터**.
- 에디터 포트 8080 API에는 **인증이 없다**(`/api/publish`, `/api/held-item` 등). 외부에 열 때는 인증이나 포트 분리가 필요(roadmap 후보 "리소스팩 자동화"에 적어 둠).

## 7. 문서 규칙에서 어긋난 점 (다음 세션이 정리할 것)

- [README.md](README.md)는 결정 기록을 "한 번 쓰면 고치지 않고, 바꿀 땐 새 기록으로 대체"라고 한다. 그런데 이번 세션에서 0001·0002·0005·0006에 **섹션을 덧붙이거나 일부를 고쳤다**(예: 0005에 "완료", "처치형 목표", 0006에 "`/clear` 문법"). 규칙을 실제 방식(같은 주제의 확장은 덧붙이고, 뒤집는 결정은 새 기록)에 맞게 고칠지, 앞으로 새 기록으로 나눌지 사용자와 정한다.
