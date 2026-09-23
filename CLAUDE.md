# CLAUDE.md — Colophon 개발 가이드 (AI 어시스턴트용)

**Colophon**: NeoForge 마인크래프트 모드. 웹 에디터에서 NPC·퀘스트(콘텐츠)와 노드 그래프(로직)를
만들고 **publish → 서버 반영**. 클라이언트+서버 모드(GeckoLib 애니메이션, 자체 퀘스트 화면).
방향: 취미로 지속 개발, 점차 규모 있는 프로젝트로 성장. 채택을 성공의 전제로 삼지 않음.

이 파일은 매 세션 로드되는 **짧은 가이드**다. 개념은 [docs/map.md](docs/map.md), 진행 상태는
[docs/roadmap.md](docs/roadmap.md)에만 있다.

## 재시작 (2026-09-23)
v2 설계가 사용자가 소화하는 속도보다 빨리 두꺼워져서, **이야기(구체 시나리오) 중심으로 다시
시작**했다. 옛 코드·문서는 git 태그 `legacy-v2`, 형제 폴더 `../Colophon-legacy`(worktree)에
보관. 옛 코드는 필요해질 때 사용자가 이해·동의한 파일만 다시 가져온다. 옛 문서는 반면교사로 읽되
기준으로 삼지 않는다.

## 코드 구조 (`kr.guinnessgroup.colophon`)
- `graph/` — 저장된 그래프 문서(`GraphDoc`)와 그 읽기·쓰기(`GraphFormat`, format 1). 모양만 검사.
- `runtime/` — 노드 종류(`NodeType`·`Field`·`NodeRegistry`), 문서 → 실행 그래프(`GraphBuilder`), 실행(`Runner`, 즉시·동기), 발행·트리거(`ColophonRuntime`).
- `record/` — 기록(`Owner`·`RecordStore` 캐시·`H2RecordBackend`, schema 1).
- `nodes/` — 빌트인 노드. 노드당 파일 하나, `BuiltinNodes.registerAll`.
- `web/` — 에디터 서버. `Colophon.java` — 부트스트랩·게임 이벤트 연결.
- 저장 형식 결정: [docs/decisions/0001-storage-format.md](docs/decisions/0001-storage-format.md).

## 설계 약속 (꼭 지킬 것)
- **"잠김" 대신 고치는 비용**(비쌈 = 저장 형식·id·공개 API / 중간 / 쌈 = 내부 코드)을 표시한다.
- **사용자가 쉬운 말로 설명할 수 없는 결정은 확정하지 않는다.** 선택지 + 추천을 주고 결정은 사용자.
- **이야기가 요구하는 것만** 설계한다. 쓰는 곳이 없는 타입·기능을 미리 설계하지 않는다.
- 한 번에 하나씩, 문서는 작게. 같은 사실은 한 곳에만.

## 스택 / 사실
- NeoForge **1.21.1** / Java **21**. mod_id `colophon`, group `kr.guinnessgroup`, **MPL-2.0**(파일 단위).
- git author: **liminaire-x <gntodtndls156@gmail.com>**. 커밋 = **conventional commits** (`feat(...)`, `docs:` …). 어트리뷰션 라인 없음(기존 관례).
- 응답/문서 언어: **한국어**. git commit 언어: **영어**.
- 프런트: React + React Flow(@xyflow/react), Vite 단일 index.html. `editor/` 소스 → Gradle buildEditor/packEditor로 패키징.
- 웹 서버: JDK `HttpServer` 8080 (`web/ColophonWebServer.java`).
- **에이전트 환경에서 Java 컴파일 금지**(NeoForge 빌드가 무겁고 Windows Gradle 캐시와 꼬임). 컴파일/실행 확인은 **GitHub Actions CI**(push 시 클린 리눅스 빌드) 또는 **사용자 IntelliJ**(`runClient`/`runServer`).

## 개발 워크플로우
1. **조각 단위로 진행**한다([docs/roadmap.md](docs/roadmap.md)). 각 조각은 게임 안에서 확인된다.
2. 검증:
   - **CI = 컴파일 게이트**. 커밋 → master push → 사후 CI 확인, 실패 시 fix-forward. 성공 판정은 **`gh run view <id> --json conclusion`으로 명시 확인**(`gh run watch` exit 코드만 믿지 말 것).
   - **IntelliJ = 동작 게이트**. 트리거 발화·NPC·애니메이션·화면 같은 MC 통합은 사용자 확인까지 받고 다음 조각으로.
   - **테스트는 비싼 것에 붙인다**: 저장 형식·id처럼 깨지면 데이터가 손상되는 곳은 처음부터 테스트 동반.
3. 광범위 변경 전에는 graphify로 호출부를 먼저 파악할 수 있다(국소 변경은 grep/read).
