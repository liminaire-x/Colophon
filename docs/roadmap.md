# 로드맵 · 진행 상태

추천 순서: **1순위 상태(flow-only) → 2순위 데이터 포트 → 3순위 안전 원칙(2순위에 묶음).**

## v0 / v1 — 완료

- **v0**: "루프가 돈다" 증명. 틱 스케줄러 + 노드 상태머신, publish→검증→핫스왑.
- **v1**: 저작 가능한 에디터 + 제어흐름(Suspend/Branch). 타입 있는 포트 모델(플로우
  포트만; 데이터는 v2). Impactor 경제 어댑터 스파이크(`Nodes.awaitAction` 추출).
- 등록 노드(v1 기준) 13개: trigger 2 / action 3 / flow 2 / economy 4 / state 2. (e에서 flow에 `compare`·`branch_if` 추가 → 현재 15.)

## v2 1순위 — 상태·저장 계층 (flow-only) — 완료

`StateBackend` 추상화(단일=H2, 멀티=공유 DB+Redis 미래), `StorageService` 라우팅,
스코프 3종(LOCAL=SavedData / PLAYER·GLOBAL=H2), 라이프사이클 훅(join 로드 / 월드
save flush / quit markOffline). 노드 `set_variable` / `has_variable`. (커밋 bbb780e 등)

- [ ] f. (릴리스 전) H2 jarJar 실서버 로드 최종 확인 — 사실상 검증됨, 릴리스 시만.

## v2 2순위 — 데이터 포트

계약 a~e 전부 확정(설계). 상세는 [decisions/0001-data-port-contract.md](decisions/0001-data-port-contract.md)
및 [decisions/0002-type-system.md](decisions/0002-type-system.md).

구현 체크리스트:

- [x] **a. 타입 레지스트리 뼈대** — `runtime/type/`(TypeKind·TypeDescriptor·TypeRegistry·BuiltinTypes). /api/schema `types`. (b989baa)
- [x] **b. 포트 category + 데이터 포트 연결 검증** — `DataPort`, `dataInPorts()/dataOutPorts()`, GraphParser flow/data 분류(명목 타입 일치, flow↔data 금지, 단일 와이어), 에디터 `isValidConnection` + 블루프린트식 노드 레이아웃. (c71dddf·fdb0491·0048d62·ee553d5)
- [x] **c. 노드 종류 분리 + value store 뼈대** — `ValueStore`+`PortRef`(ExecContext.values), `Node`→`ExecNode`+`NodeKind`, `PureNode`+`PureContext` 스켈레톤. (bcd3c31·9e5ef23·9ba058b)
- [x] **d. 입력 통합** — `FieldSpec`→`InputSpec` 단일 소스, 13노드 마이그레이트, `FieldSpec`/`SimpleNodeType`/`dataInPorts()` 제거. 타입 표기 bare 원시 + `TypeId` 구조체 + number=double. (3749085·ede6c99·5749cc1·9cb24fd·367e351·1f7bc73)
- [ ] **e. 다음 (진행 중)** — 값 흐름: 데이터 엣지 배선 + `ValueResolver`(exec push/pure pull) + typed `Type<T>` I/O + 첫 데이터 노드(get_variable·compare·format_text·get_balance·branch_if) + 트리거 명시 출력(victim/killer). 설계: [decisions/0003-value-flow.md](decisions/0003-value-flow.md). 수직 슬라이스(get_variable→send_message)부터.
- [ ] **f.** 안정 ID·version(디스크립터 필드)·마이그레이션 3층·deprecation·/api/validate.

## v2 3순위 — 안전 원칙 강화 (크로스커팅 하드닝)

- [ ] 없는 대상·미준비 값 → 예외 대신 **정의된 결과**(false/skip + 경고). (StorageService는 이미 warn+skip.) unset 처리와 같은 갈래.
- [ ] 2순위 "잘못된 대상 지정" 케이스와 함께 처리.

## 진행 로그

- **2026-09-19** — 2순위 b 완료(포트 category + 연결 검증, 블루프린트 노드 레이아웃, 브라우저 실증).
- **2026-09-20** — 2순위 c(뼈대)·d 완료. 타입 표기 결정(bare 원시 + TypeId + number=double, 외부 논의 반영). Trilium → `docs/` 이관, GitHub Actions CI 도입.
- **2026-09-21** — e 확장 시작: 트리거 **player 출력**(`on_player_join`) = 첫 exec push 실증. `send_message.target`(player 입력) 소비자 추가. in-game 확인. 커밋 61ae820.
- **2026-09-21** — e 확장: **`get_variable`**(Pure) — `PureContext.readVar` 읽기전용 확장(순수성 유지). `send_message.message` 연결 가능 string 승격. in-game 확인. 커밋 d73b292.
- **2026-09-20** — 2순위 e 수직 슬라이스 완료(값이 흐른다 실증): 데이터 엣지 배선 + `ValueResolver`(exec push/pure pull) + typed `Type<T>`/`Types` + `compare`(pure)·`branch_if`(exec) + 에디터 인라인/와이어. 데모 in-game 확인. **노드 종류 = `ExecNodeType`/`PureNodeType` 인터페이스로 분리(컴파일러 강제, 계약 a)** — pure 노드는 flow 메서드가 없어 순수성이 타입으로 보장됨. NodeType은 애드온 SDK 경계라 애드온 생기기 전 지금 분리(나중엔 breaking).

## 백로그

[ideas.md](ideas.md) 참조 — 에이전트 편집 API, 마이그레이션 문서 템플릿, 변수
레지스트리, 정적 도달성 분석, Javalin 전환, 멀티서버, NC 시네마틱, GeckoLib 등.
