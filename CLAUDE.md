# CLAUDE.md — Colophon 개발 가이드 (AI 어시스턴트용)

**Colophon**: 마인크래프트 **Typewriter를 NeoForge로 재개발**한 노드 기반 서버 로직 저작 플랫폼.
웹 에디터에서 노드 그래프를 짜고 **publish → 서버 반영**. (NPC·타이핑은 빼고 노드 에디터에 집중.)
방향: 취미로 지속 개발, 점차 규모 있는 프로젝트로 성장. 채택을 성공의 전제로 삼지 않음.

## 스택 / 사실
- NeoForge **1.21.1** / Java **21**. mod_id `colophon`, group `kr.guinnessgroup`, **MPL-2.0**(파일 단위).
- git author: **liminaire-x <gntodtndls156@gmail.com>**. 커밋 = **conventional commits** (`feat(...)`, `docs:` …). 어트리뷰션 라인 없음(기존 관례).
- 응답/문서 언어: **한국어**.
- git commit 언어: **영어**.
- 프런트: React + React Flow(@xyflow/react), Vite 단일 index.html. `editor/` 소스 → Gradle buildEditor/packEditor로 패키징.
- 웹 서버 v0: JDK `HttpServer` 8080 (`web/ColophonWebServer.java`). 엔드포인트 /api/health·schema·graph·publish.
- **에이전트 환경에서 Java 컴파일 금지** — NeoForge 빌드가 무겁고 Windows Gradle 캐시와 꼬임. 컴파일/실행은 **사용자 IntelliJ**에서 확인.

## 코드 구조
- `runtime/` — 엔진. `Node`/`NodeType`/`NodeResult`/`ExecContext`, `NodeRegistry`, `Graph`/`GraphParser`, `TickScheduler`, `Nodes`(awaitAction 등 헬퍼), `state/`(스토리지 계층), **`type/`(타입 레지스트리, 신규)**.
- `nodes/` — 빌트인 노드 라이브러리(`trigger/`·`action/`·`flow/`·`economy/`·`state/`). `BuiltinNodes.registerAll()`. 노드당 파일 하나(NodeType 구현). economy는 Impactor soft-dep(ModList 가드).
- `web/` — 에디터 서버. `Colophon.java` — 모드 부트스트랩(이벤트 구독·등록·서버 시작/정지).
- `api`/`core` — 빈 패키지(미사용). SDK 경계 추출은 나중 점진적.

## 현재 상태 (2026-09-19)
- **v2 1순위 상태·저장 계층 = 완료**(커밋 bbb780e). 등록 노드 13개(trigger2/action3/flow2/economy4/state2).
- **v2 2순위 데이터 포트: 계약 a~e 전부 확정(설계 완료)**, 구현 착수:
  - ✅ **a. 타입 레지스트리 뼈대** — `runtime/type/` (TypeKind, TypeDescriptor, TypeRegistry, BuiltinTypes 4종). /api/schema에 `types` 노출. 노드 미사용. **커밋 b989baa, 실행 검증 완료.**
  - ✅ **b. 포트 category(flow/data) + 데이터 포트 연결 검증** — `DataPort`(id/typeId/label), `NodeType.dataInPorts()/dataOutPorts()` default, /api/schema `dataIn`/`dataOut` 노출. GraphParser flow/data 분류(명목 타입 일치, flow↔data 금지, 데이터입력 단일 와이어; 데이터 엣지는 검증만·미배선). 에디터 `isValidConnection` 대칭 + `targetHandle` 왕복 + **블루프린트식 노드 레이아웃**(exec 삼각형 상단/데이터 원형 좌·우). **커밋 c71dddf·fdb0491·0048d62·ee553d5, 브라우저 실증 완료(타입 일치 연결·불일치 차단).**
  - ✅ **c. 노드 종류 분리 + value store 뼈대** — `ValueStore`+`PortRef`(ExecContext.values, unset=부재), `Node`→`ExecNode` 리네임+`NodeKind{EXEC,PURE}`, `PureNode`+읽기전용 `PureContext` 스켈레톤, `NodeType.kind()/createPure()`. 노드 미사용(무회귀). **커밋 bcd3c31·9e5ef23·9ba058b.** _(exec push/pure pull 실배선은 데이터 노드 생기는 e에서.)_
  - 🔄 **d. 입력 통합(진행 중)** — `FieldSpec`→`InputSpec`(타입+인라인 기본값+connectable). **d-1 완료**: `InputSpec` + `NodeType.inputs()` 브리지(fields=inline·connectable false / dataIn=connectable true) + /api/schema `inputs`. **타입 표기 결정**(타입시스템 인수인계 문서 반영): 값 원시는 **bare**(`string/number/boolean`), 참조만 네임스페이스(`colophon:player`). 내부 `TypeId` sealed(Builtin/Named). `number`=단일 IEEE754 double. **커밋 3749085·ede6c99.** 남음: 에디터 폼(미연결=인라인/연결=와이어), 13노드 `inputs()` 직접 선언으로 마이그레이트→`FieldSpec` 제거.
  - ⬜ e. 첫 데이터 노드(get_balance·get_variable·format_text·compare) + 트리거 명시 출력(victim/killer). "값이 흐른다" 실증(economy/state로).
  - ⬜ f. 안정 ID·version(디스크립터 필드)·마이그레이션 3층·deprecation·/api/validate.

## 데이터 포트 계약 요약 (전체·근거·기각 대안은 Trilium)
**블루프린트를 따라가되 세 곳만 더 엄격**: 거대한 타입 시스템 X, 암묵 자동 캐스트/ToString X, `self` 문맥 기본값 X. 원칙: 계약을 잠그고 메커니즘은 미룬다.
- **a 노드 종류**: `ExecNode`/`PureNode` 물리 분리(컴파일러 강제). PureNode=읽기전용 PureContext. per-exec value store(exec push/pure pull, 수명=플로우 실행 전체). exec 데이터출력=실행 후 유효(미실행=unset→정의결과).
- **b 타입**: 열린 TypeRegistry(명목 매칭, 서브타이핑 X). 값(string/number/boolean) vs 참조(player=UUID, resolve 실패=unset, serializable=false). **표기: 값 원시=bare(`string`), 참조·애드온=`네임스페이스:이름`(`colophon:player`, `economy:account`)**. 내부는 `TypeId` sealed(Builtin/Named), JSON은 문자열 불변. `number`=단일 IEEE754 double(int/float 재분할 X, 정확 64bit는 별도 `int64` 문자열 인코딩=미래). 연결 판정=단일 진입점(지금 exact, 서브타이핑/autocast는 additive 미래). flow/data category. 암묵 자동변환 없음. 값→텍스트=전용 `format_text` Pure 노드. _(상세: `~/Downloads/colophon-type-system-handoff.md`)_
- **c 입력 통합**: config+데이터를 "입력" 하나(타입+인라인 기본값+connectable, 입력별 SDK 속성, 기본 true·구성 노브만 false). 입력=와이어1 or 인라인, 출력=fan-out.
- **d 주체 = 균일 명시**: 주체는 값(마법 아님), 모든 참조 타입 균일. 트리거가 이름 있는 타입 출력(victim/killer). 참조 입력=명시 연결·문맥 기본값 없음(미연결=unset). exec선≠데이터(앞 노드 대상 자동 안 물려감, 같은 대상=fan-out).
- **e 직렬화·버전**: 포트 ID=불변 semantic 문자열(라벨은 별도 번역 키). version=디스크립터 필드(`default int version(){return 1;}`, ID/타입/구조/제거 변경 시만↑). 마이그레이션 3층(관대한 파싱=안 터짐 / 선언적 리다이렉트=rename / 명시적 마이그레이터=값·구조). deprecation=노드 단위(soft). 진단은 /api/validate 드라이런 + (Javalin 후) 에디터 push.

## 개발 워크플로우 (꼭 지킬 것)
1. **한 번에 다 하지 말고 단계로 쪼갠다.** 각 단계 끝에 사용자에게 **IntelliJ 컴파일/실행 확인** 요청 → BUILD SUCCESSFUL/동작 확인되면 그 단계 **커밋**.
2. 코어 인터페이스 진화는 **default 메서드로 하위호환** 유지. `FieldSpec`→`InputSpec`은 유일한 광범위 리팩터(노드 13개인 지금이 적기).
3. **계약(포트/타입/직렬화 표면) 결정은 이미 잠김** — 재논의 말고 구현. 근거·기각 대안은 Trilium 인수인계 노트.
4. 패키지 구조 재편 불필요 — `runtime`/`nodes` 유지, 신규는 `runtime/type`·`runtime/migration` 등으로 추가.

## 참고 (Trilium — 전체 맥락·근거)
- 메인 `LN7eV5zzFvup` (Colophon) / 진행 트래커 `6YGCNc0xpcGz` / 계약 인수인계 `zLDgafSVVhg7`(a~e 상세 + "기각한 대안") / 아이디어 회고 `HWCruwR3XeD8`.
