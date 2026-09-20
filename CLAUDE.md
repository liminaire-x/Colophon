# CLAUDE.md — Colophon 개발 가이드 (AI 어시스턴트용)

**Colophon**: 마인크래프트 **Typewriter를 NeoForge로 재개발**한 노드 기반 서버 로직 저작 플랫폼.
웹 에디터에서 노드 그래프를 짜고 **publish → 서버 반영**. (NPC·타이핑은 빼고 노드 에디터에 집중.)
방향: 취미로 지속 개발, 점차 규모 있는 프로젝트로 성장. 채택을 성공의 전제로 삼지 않음.

이 파일은 매 세션 로드되는 **짧은 가이드**다. 상세(근거·계약·아키텍처·아이디어)는 [`docs/`](docs/README.md)가 단일 출처.

## 스택 / 사실
- NeoForge **1.21.1** / Java **21**. mod_id `colophon`, group `kr.guinnessgroup`, **MPL-2.0**(파일 단위).
- git author: **liminaire-x <gntodtndls156@gmail.com>**. 커밋 = **conventional commits** (`feat(...)`, `docs:` …). 어트리뷰션 라인 없음(기존 관례).
- 응답/문서 언어: **한국어**. git commit 언어: **영어**.
- 프런트: React + React Flow(@xyflow/react), Vite 단일 index.html. `editor/` 소스 → Gradle buildEditor/packEditor로 패키징.
- 웹 서버 v0: JDK `HttpServer` 8080 (`web/ColophonWebServer.java`). 엔드포인트 /api/health·schema·graph·publish.
- **에이전트 환경에서 Java 컴파일 금지**(NeoForge 빌드가 무겁고 Windows Gradle 캐시와 꼬임). 컴파일/실행 확인은 **GitHub Actions CI**(push 시 클린 리눅스 빌드, `gh run`으로 결과 확인) 또는 **사용자 IntelliJ**.

## 코드 구조
- `runtime/` — 엔진. 러너블 `ExecNode`/`PureNode`; 디스크립터 `NodeType`(base)·`ExecNodeType`/`PureNodeType`(컴파일러 강제 분리); `NodeResult`, `ExecContext`/`PureContext`, `ValueStore`/`PortRef`/`ValueResolver`, `NodeRegistry`, `Graph`/`GraphNode`/`GraphParser`, `TickScheduler`, `Nodes`(헬퍼), `InputSpec`/`DataPort`, `state/`(스토리지), `type/`(`TypeRegistry`·`TypeId`·`Type<T>`/`Types`).
- `nodes/` — 빌트인 노드(`trigger/`·`action/`·`flow/`·`economy/`·`state/`). `BuiltinNodes.registerAll()`. 노드당 파일 하나(`ExecNodeType` 또는 `PureNodeType` 구현). economy는 Impactor soft-dep.
- `web/` — 에디터 서버. `Colophon.java` — 모드 부트스트랩.
- `api`/`core` — 빈 패키지. SDK 경계 추출은 나중.

## 현재 상태
- v2 1순위 상태·저장 계층 = **완료**. 등록 노드 15개(+compare/branch_if).
- v2 2순위 데이터 포트(계약 a~e 확정): **a✅ b✅ c✅ d✅ e🔄** / f(직렬화·버전).
  - **e 수직 슬라이스 = 완료(in-game 실증)**: 데이터 엣지 배선 + `ValueResolver`(exec push/pure pull) + typed `Type<T>` + `compare`(pure)·`branch_if`(exec) + 에디터 인라인/와이어.
  - **e 확장 = 다음**: `get_balance`(async)·`format_text`·트리거 명시 출력(victim/killer)·`get_variable`. 설계 [docs/decisions/0003-value-flow.md](docs/decisions/0003-value-flow.md).
- 잠긴 계약: 노드 종류 = `ExecNodeType`/`PureNodeType`(컴파일러 강제), 타입 명목 매칭(값 원시 bare·참조 네임스페이스, `TypeId`, `Type<T>`, number=double), 입력 통합(`InputSpec`), 주체 균일 명시. **상세·근거·기각 대안 → [docs/decisions/](docs/decisions/).**
- 상세 체크리스트·커밋·진행 로그 → [docs/roadmap.md](docs/roadmap.md), 새 세션 온보딩 → [docs/handoff.md](docs/handoff.md).

## 개발 워크플로우 (꼭 지킬 것)
1. **단계로 쪼갠다.** 검증 2단:
   - **CI = 컴파일 게이트** — 구조·리팩터 단계는 이걸로 충분. 운영(초기): **커밋 → master push → 사후 CI 확인**, 실패 시 fix-forward. CI 성공 판정은 **`gh run view <id> --json conclusion`으로 명시 확인**(`gh run watch` exit 코드만 믿지 말 것 — 오독 사례 있음).
   - **IntelliJ/브라우저 = 동작 게이트** — 관찰 가능한 동작이 있는 단계(데모·에디터 UX·값 흐름)는 이 확인까지 받고 진행.
2. **광범위 변경·새 단계 설계 전 graphify 선(先)질의**로 호출부·교차 관심사 오리엔테이션(국소 변경은 grep/read).
3. 코어 인터페이스 진화는 **default 메서드로 하위호환** 유지.
4. **계약(포트/타입/직렬화 표면) 결정은 잠김** — 재논의 말고 구현. 근거·기각 대안은 [docs/decisions/](docs/decisions/).
5. 패키지 재편 불필요 — `runtime`/`nodes` 유지, 신규는 `runtime/type`·`runtime/migration` 등으로 추가.

## 문서 (docs/)
- [docs/README.md](docs/README.md) — 문서 지도 / [roadmap.md](docs/roadmap.md) — 로드맵·진행 / [architecture.md](docs/architecture.md) — 아키텍처
- [decisions/0001-data-port-contract.md](docs/decisions/0001-data-port-contract.md) — 계약 a~e / [decisions/0002-type-system.md](docs/decisions/0002-type-system.md) — 타입 시스템
- [ideas.md](docs/ideas.md) — 아이디어·백로그 / [discussions.md](docs/discussions.md) — 미해결 질문
