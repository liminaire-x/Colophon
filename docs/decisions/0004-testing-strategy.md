# ADR 0004 — 테스트 전략

**상태:** 결정(하니스 미구현). 계약 e 완료 후 도입 예정 — 다음 작업. 계약 f(직렬화·
마이그레이션) 진입 전에 1·2층 하니스를 먼저 세운다.

## 맥락

지금까지 검증은 2단이었다: **CI = 컴파일 게이트**, **IntelliJ/브라우저 = 동작 게이트**.
동작 게이트는 매 슬라이스마다 사람이 "에디터 재빌드 → publish → 인게임 확인"을 수동으로
왕복해야 해서 세션의 최대 병목이었다. 값 흐름 로직 상당수는 서버 없이 검증 가능하므로,
동작 게이트의 일부를 **자동 테스트 게이트**로 내린다.

핵심 긴장: **구현과 테스트를 동시에 갱신하면 토큰·유지보수 비용이 커진다.** 특히 런타임
코어가 계속 진화하는 지금, 노드 테스트가 런타임 내부에 결합되면 리팩터마다 테스트가 깨져
재작성 비용이 눈덩이가 된다.

## 결정 — 계약 스코프 테스트 (핵심 원칙)

**테스트는 잠긴 계약(공개 입출력)에만 붙인다. 런타임 내부 구현에는 붙이지 않는다.**
근거(업계 검증): 구현에 결합된 테스트는 리팩터 시 행위가 같아도 깨진다("brittle"), 공개
계약(API)에 붙은 테스트는 내부가 바뀌어도 살아남는다. 테스트의 역할은 "리팩터가 행위를
바꾸지 않았음을 보장"하는 것 — 내부가 어떻게 하는지가 아니라 무엇을 하는지를 검증한다.

이 원칙이 "런타임 진화 시 노드 테스트 재작성 부담"의 해답이다:
- 노드 테스트는 노드의 **공개 계약**(`evaluate`/`execute`: 입력 → 출력/분기)만 본다.
- `ValueResolver`·`ExecContext` 내부가 바뀌어도(default 메서드 추가 등, [0003](0003-value-flow.md))
  노드 계약이 그대로면 테스트는 안 깨진다.
- 테스트가 깨지는 건 **계약 자체가 바뀔 때뿐** — 그건 드물고, 의도적으로 잠긴다
  ([CLAUDE.md 워크플로우 #4], 잠긴 계약 목록). 따라서 재작성 빈도가 낮다.

## 3층 피라미드

### 1층 — 순수 단위 (가볍게, 지금 시작)
MC 타입을 안 건드리는 코어. **mock 없이 실제 객체로.**
- `ValueResolver`(exec push / pure pull, 사이클 가드) — 가짜 `Graph`로 실제 resolver 구동.
- `GraphParser`(flow/data 분류, 명목 타입 매칭, `instanceInputs` 토큰 파싱, 단일 와이어).
- `FormatTextNode` 토큰 파싱·보간, `CompareNode` 연산, `Type`/`InputSpec` 파싱.
- 전부 잠긴 계약 표면 → 안정적. **CI 필수 게이트.**

### 2층 — 노드 단위 (노드 확정 시마다 작성)
노드의 `evaluate`/`execute` 계약을 검증. **mock은 진짜 외부 경계(MC 런타임)에만.**
- 실제 사용: `ExecContext`(테스트 조립), 인메모리 `StorageService`, 실제 `ValueResolver`·
  `ValueStore`·`GraphParser`. 이들은 내부 협력자 → mock 금지(그러면 mock을 테스트하게 됨).
- mock 대상: `ServerPlayer`·`MinecraftServer`(Mockito stub: `getName()`, `getX()` …),
  `EconomyService`(Impactor) — 진짜 외부 경계.
- 예: `player_info`(name/x/y/z), `send_message`(target 폴백), `get_variable`(scope×storage),
  트리거 seed → 소비 흐름.
- **가상 플레이어 = Mockito mock**(가벼움). 실제 `FakePlayer`는 3층에서.

### 3층 — GameTest (진짜 통합, 나중)
NeoForge GameTest + `FakePlayerFactory` — 실제 `ServerLevel`에서 가짜 플레이어로 트리거
발화·economy async·틱 스케줄러 통합 검증. 지금의 수동 인게임 확인의 자동화판. 무겁고
설정이 있어 **진짜 통합이 필요한 것만**(economy·이벤트) 남긴다.

## mock 경계 규칙 (over-mock 방지, 업계 검증)

AI 에이전트는 mock을 과하게 써 "mock의 설정만 검증하는" 테스트를 만드는 경향이 있다(연구
확인). 방지 규칙:
- **경계 규칙**: mock은 진짜 외부 의존성(MC 런타임, Impactor, 파일/DB)에만. 내부 협력자
  (`ValueResolver`, `StorageService`-인메모리, `ValueStore`)는 실제 객체.
- **검증 규칙**: "이 테스트가 코드의 행위를 검증하나, mock 설정만 검증하나?" mock을 빼면
  테스트가 의미 있게 실패해야 한다.
- **단순성 규칙**: mock 셋업이 테스트 로직보다 길면 실제 객체를 써라.

## 토큰·유지보수 비용 관리 (핵심 고민의 답)

- **계약 스코프 + 잠긴 계약에만** 테스트 → 런타임 내부 리팩터가 테스트를 안 깨뜨림 → 재작성
  토큰 지출이 구조적으로 낮다.
- **테스트 픽스처 1곳 집중**: `ExecContext`/인메모리 `StorageService` 조립을 헬퍼 하나로.
  런타임 생성자가 바뀌면 **헬퍼 한 곳만** 고치고 개별 테스트 N개는 유지 → 진화 비용 최소화.
- **아직 유동적인 노드는 테스트를 미룬다**: 계약이 확정(잠김)된 노드부터 작성(사용자 방침).
  확정 노드는 계약-스코프라 이후 런타임 진화에도 안정적.
- **TDD-lite**: 계약이 이미 정해진 신규 노드는 계약 테스트(입력→기대출력)를 먼저 쓰고 구현.
  유동적 설계는 예외. (test-first는 "구현이 자기 코드를 검증하는 통과용 테스트"를 막는다 —
  연구 확인.)
- **실행은 CI**: 에이전트가 로컬 컴파일 금지([CLAUDE.md])이므로 테스트도 CI에서 실행. AI가
  반복 실행하지 않으니 실행 자체엔 토큰이 안 든다. 테스트 작성=에이전트, 실행/판정=CI.

## 인프라 (도입 시 할 일)

- `build.gradle`: JUnit 5(+Mockito) test deps, NeoForge 테스트 소스셋.
- CI(`.github/workflows/build.yml`): `test` 태스크 추가 → 1·2층이 컴파일 게이트와 함께 자동.
- 첫 착수: 1층(ValueResolver·GraphParser) + 픽스처 헬퍼 + 2층 노드 1~2개(예: `player_info`,
  `get_variable`)로 패턴 확립.

## 근거 (외부 검증)

- 구현 결합 테스트는 brittle, 공개 계약 테스트는 리팩터에 견딤 — 다수 출처(Google SWE Book
  Ch.12 Unit Testing 등).
- AI 생성 테스트의 over-mock 안티패턴 — "Are Coding Agents Generating Over-Mocked Tests?"
  (arXiv 2602.00409): 내부까지 mock하면 mock을 테스트하게 됨, 경계에만 mock.
- 규율 없는 AI 사용은 배포 안정성을 낮춤(DORA 2025), test-first가 요구사항 사고를 강제 —
  AI-TDD 가이드 다수.
