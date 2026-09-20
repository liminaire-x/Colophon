# 아키텍처

## 스택 / 사실

- NeoForge **1.21.1** / Java **21**. mod_id `colophon`, group `kr.guinnessgroup`, **MPL-2.0**(파일 단위).
- 프런트: React + React Flow(@xyflow/react), Vite 단일 index.html. `editor/` 소스 → Gradle `buildEditor`/`packEditor`로 패키징.
- 웹 서버 v0: JDK `HttpServer` 8080 (`web/ColophonWebServer.java`). 엔드포인트 `/api/health·schema·graph·publish`.

## 코드 구조

- `runtime/` — 엔진(계약 + 기계). `ExecNode`/`PureNode`/`NodeType`/`NodeResult`/`ExecContext`, `NodeRegistry`, `Graph`/`GraphParser`, `TickScheduler`, `Nodes`(awaitAction 등 헬퍼), `ValueStore`/`PortRef`, `InputSpec`/`DataPort`, `state/`(스토리지 계층), `type/`(타입 레지스트리·`TypeId`).
- `nodes/` — 빌트인 노드 라이브러리(`trigger/`·`action/`·`flow/`·`economy/`·`state/`). `BuiltinNodes.registerAll()`가 등록, 엔진은 구체 노드를 모름. 노드당 파일 하나(NodeType 구현). economy는 Impactor soft-dep(ModList 가드).
- `web/` — 에디터 서버. `Colophon.java` — 모드 부트스트랩(이벤트 구독·등록·서버 시작/정지).
- `api`/`core` — 빈 패키지(미사용). SDK 경계 추출은 나중 점진적.

SDK 추출 시 `runtime` = SDK 표면, `nodes` = 빌트인 애드온으로 갈라진다.

## 런타임 실행 모델 — 최대 난제

세 제약이 동시에 성립해야 한다: ① 게임 조작은 **메인 서버 스레드 전용** ② 플로우는
**중간에 멈출 수 있어야** 함(딜레이/대기) ③ 메인 스레드 **block 금지**. → 그래프 실행을
"일시정지·재개되는 실행"으로 모델링.

- 선택: **틱 스케줄러 + 노드 상태머신**(순수 자바, 단순·디버깅 쉬움). Kotlin 코루틴은
  도입 비용으로 기각하되, 노드 인터페이스에 `Suspend` 자리를 미리 뚫어 둠.
- 노드 인터페이스: `execute(ctx) → NodeResult { Continue | Branch | Suspend | Done | Fail }`.
- `ExecContext` = 대상 액터/플레이어, 서버, 스토리지, 로컬 변수 스코프, 그리고 데이터
  포트 값 저장소(`ValueStore`, 실행 1회 수명).
- **안전장치**: 사이클 + 무대기 그래프의 한 틱 무한루프 → **틱당 노드 예산**으로 방어.
- **핫 리로드**: publish → 검증 → 옛 트리거 해제 → 진행 중 실행 취소(v0) → 새 트리거 등록.

**비동기 다리 패턴**: 게임 상태는 무조건 메인에서. 무거운/기다리는 일은 오프 스레드에서
하고 결과를 `server.execute()`로 다음 틱에 메인으로 넘겨 적용. 경제 async(계정 획득이
CompletableFuture)가 정확히 이 패턴 — `future.isDone()`을 ResumeCondition으로 삼아
Suspend, 완료 후 진행. 배관은 `Nodes.awaitAction` 헬퍼로 추출.

노드 종류(ExecNode/PureNode)와 데이터 값 흐름(exec push/pure pull)의 상세는
[decisions/0001-data-port-contract.md](decisions/0001-data-port-contract.md) 참조.

## 영속화 — 정의 vs 상태 분리

**정의**(콘텐츠)와 **상태**(실행 결과)는 성격이 완전히 다르므로 따로 다룬다.

- **정의**: 서버별 파일로 흩어놓으면 관리 지옥 → 중앙 스토어 하나 + 그래프별 서버 scope 태그.
- **상태**: raw DB 노드 강제는 개발 경험이 나쁨 → 변수에 **스코프**(LOCAL/PLAYER/GLOBAL)를
  두고 런타임이 백엔드로 라우팅. 명시적 DB 노드는 파워유저 옵션.
- **스코프별 전략**: PLAYER = per-player 캐시(join 로드 / 플레이 중 메모리 / quit flush).
  LOCAL = 마크의 SavedData가 이미 "메모리 상주 + 월드 저장 편승"이라 거의 공짜. GLOBAL = 전 서버 공유.
- **flush 정책**: 월드 저장 주기에 편승해 dirty만 flush, **quit은 비저장**(오프라인 캐시를
  1주기 들고 감). 이유 — 데이터가 월드보다 앞서면 롤백 시 불공정 → 데이터·월드를 같은
  스냅샷 시점에 정렬. 즉시 재접속 레이스는 "캐시 살아있으면 재사용, 없을 때만 DB 로드"로 방어.
- **v0 결정**: 단일 서버 + 임베디드 **H2**(순수 자바, 무설치). 멀티서버 전환 시 datasource만
  공유 DB로 바꾸고 Redis 추가. 멀티서버는 능동 개발 없이 **이음매(`StateBackend`)만 유지**.

## 핵심 설계 선택

- **이름 Colophon** — 책 끝의 간기(刊記)/인쇄 계보. Typewriter의 "타이핑" 은유가 웹 에디터
  방향과 안 맞아 개명. author = "Liminaire".
- **퍼스트파티 애드온 모델** — 3rd-party에 "우리와 호환되게 개발하라"는 부담을 지우면
  생태계가 안 큼. 코어 + 본인이 만드는 퍼스트파티 애드온. 코어–애드온 경계(SDK)만 명확히.
  빌트인 노드 = "첫 퍼스트파티 애드온"이라 코어가 곧 애드온 템플릿.
- **프런트 React Flow** — 사실상 표준. Typewriter 원본은 Flutter 웹이라 재사용이 어려워
  웹 표준 스택으로 신규 개발.
- **라이선스 MPL-2.0** — 파일 단위 copyleft. 코어 포크는 오픈 유지, 링크하는 애드온·사설
  연동은 자유. Typewriter 코드는 한 줄도 안 가져오는 원칙(독립 작성 = 커밋 히스토리로 입증).

## 개발 워크플로우 원칙

1. **단계로 쪼갠다.** 각 단계 끝에 빌드/동작 확인(CI 그린 또는 IntelliJ) → 그 단계 커밋.
2. 코어 인터페이스 진화는 **default 메서드로 하위호환** 유지.
3. **계약(포트/타입/직렬화 표면) 결정은 잠김** — 재논의 말고 구현. 근거·기각 대안은 `decisions/`.
4. 패키지 재편 불필요 — `runtime`/`nodes` 유지, 신규는 `runtime/type`·`runtime/migration` 등 추가.
