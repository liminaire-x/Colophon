# 인수인계 (새 세션용)

새 세션/개발자가 빠르게 맥락을 잡기 위한 서술형 요약. 정식 상태는
[roadmap.md](roadmap.md), 근거는 [decisions/](decisions/), 매 세션 로드되는 짧은
상태는 루트 [`CLAUDE.md`](../CLAUDE.md).

## 지금 어디인가 (2026-09-20)

- **계약 a~d = 완료.** 타입 레지스트리, 포트 category + 연결 검증, 노드 종류 분리 뼈대,
  입력 통합(`InputSpec`).
- **계약 e = 수직 슬라이스 완료.** "값이 흐른다"를 in-game으로 실증했다.
- **다음 = e 확장** → 그 뒤 f(직렬화·버전).

## 이 세션에 한 일

### 계약 c·d 구현
- `Node`→`ExecNode` 리네임 + `NodeKind`, `PureNode`/`PureContext`, per-execution
  `ValueStore`.
- `FieldSpec`→`InputSpec` 단일 소스로 통합. 13노드 마이그레이트, `FieldSpec`·
  `SimpleNodeType`·`dataInPorts()` 제거.

### 타입 시스템 결정 (외부 논의 반영)
- 값 원시 **bare**(`string/number/boolean`), 참조·애드온만 `네임스페이스:이름`
  (`colophon:player`). 내부 `TypeId` sealed(Builtin/Named), JSON은 문자열 불변.
  `number` = 단일 IEEE754 double. 상세 [decisions/0002-type-system.md](decisions/0002-type-system.md).

### 계약 e 수직 슬라이스 ("값이 흐른다")
- **데이터 엣지 배선**: `GraphNode`가 kind별 러너블(exec/pure) + `config` +
  `dataSources`(입력포트→생산자 PortRef)를 보유. GraphParser가 배선.
- **`ValueResolver`**: 입력=연결됐으면 생산자 출력, 아니면 인스턴스 인라인값(config→
  디스크립터 기본값). 출력=exec는 `ValueStore` read(미실행=unset), pure는 pull 평가(캐시,
  사이클 가드).
- **typed `Type<T>`/`Types`**(string/number/boolean/player) — `ctx.get(id, Types.NUMBER)`.
- 첫 데이터 노드: **`compare`(Pure)** number×number→boolean, **`branch_if`(Exec)**
  boolean→true/false.
- 에디터: connectable 입력이 **미연결=인라인 필드 / 연결=와이어**.
- 데모: `on_player_join → branch_if(condition←compare(5,3,">")) → true/false→send_message`.
- 설계 [decisions/0003-value-flow.md](decisions/0003-value-flow.md).

### 노드 종류 = 컴파일러 강제 (계약 a 실현)
- `NodeType`(공유 base) → **`ExecNodeType`**(create·hasFlowIn·flowOutPorts abstract) /
  **`PureNodeType`**(createPure abstract, kind=PURE). pure 노드엔 flow 메서드가 없어
  순수성이 타입으로 보장된다. `NodeType`은 애드온 SDK 경계라 애드온 생기기 전 지금 분리
  (나중엔 breaking).

### 인프라
- **Trilium → `docs/` 이관** (단일 출처). Trilium은 아카이브, 갱신 중단.
- **GitHub Actions CI** (`.github/workflows/build.yml`) — push마다 `./gradlew build`.
- graphify 그래프 코드+docs로 재생성 (`graphify-out/`, gitignore).

## 다음 단계 (e 확장)

[decisions/0003-value-flow.md](decisions/0003-value-flow.md) 하위:
- `get_balance`(Exec, async → awaitAction/Suspend → number push) — exec push 실증.
- `format_text`(Pure, 동적 입력 포트) — 값→텍스트 유일 명시 노드.
- 트리거 명시 출력: `on_player_death`→victim/killer, `on_player_join`→player (계약 d 주체).
- `get_variable`(Pure) — 단, `PureContext`에 읽기전용 스토리지 접근 추가 설계 필요.
- 그 뒤 **계약 f**: 안정 ID·version(디스크립터 필드)·마이그레이션 3층·deprecation·/api/validate.

## 일하는 방식 (꼭 읽을 것)

- **2단 검증**: CI = 컴파일 게이트(구조·리팩터), IntelliJ/브라우저 = 동작 게이트(데모·UX·값 흐름).
- 운영: 커밋 → master push → **CI 결과는 `gh run view <id> --json conclusion`으로 명시 확인**
  (`gh run watch`의 exit 코드만 믿지 말 것 — 한 번 오독한 적 있음).
- 에이전트는 **로컬 Java 컴파일 안 함**(Windows Gradle 캐시 꼬임). CI 또는 사용자 IntelliJ.
- 광범위 변경·새 단계 설계 전 **graphify 선질의**(국소는 grep/read).
- 커밋 = conventional, 영어. author liminaire-x. 응답·문서 = 한국어.

## 함정 (겪은 것)

- **pure 노드와 abstract 메서드**: pure 노드는 flow가 없다. flow 메서드를 `NodeType`
  base에 abstract로 두면 pure 노드가 구현을 강제당한다 → `ExecNodeType`/`PureNodeType`
  분리로 해결(위 참조).
- **graphify 파이썬**: 인라인 heredoc + `Remove-Item` 체이닝이 이 환경에서 죽는다 →
  스크래치패드 `.py`로 실행, 정리는 별도 호출.
- **CI 그린 오독**: `gh run watch --exit-status`의 배경 실행 exit 코드를 green으로
  오독한 적 있음. conclusion은 `--json`으로 확인.
