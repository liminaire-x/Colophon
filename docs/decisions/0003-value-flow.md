# ADR 0003 — 값 흐름 (계약 e)

**상태:** 수직 슬라이스 완료(in-game 실증), 확장 진행 중. 계약 a~d(표면)를 실제
동작으로 잇는 단계 — "값이 흐른다". 계약 근거는
[0001-data-port-contract.md](0001-data-port-contract.md), 타입은
[0002-type-system.md](0002-type-system.md).

## 목표

데이터 포트가 검증만 되던 상태(b)에서 **실제로 값이 흐르게** 한다: exec 노드가 데이터
출력을 push하고, pure 노드가 pull로 평가되며, 소비 노드가 입력을 해석해 쓴다.

## 결정

- **typed `Type<T>` API 도입**(핸드오프 §5) — 값은 raw Object가 아니라 `Type<T>` 경유.
  `Type<T>` = typeId + 런타임 자바 클래스 + 코덱(저장값↔런타임 객체). 노드는
  `ctx.get(portId, Types.NUMBER) → Double`처럼 타입 안전하게 읽는다. (InputPort<T> 필드
  선언 sugar는 나중 ergonomic 추가 여지.)
- **수직 슬라이스 먼저** — 파이프라인(배선+resolver+I/O)+pure 노드 1개+최소 데모로 "값이
  흐른다"를 먼저 증명한 뒤 노드를 넓힌다.

## 아키텍처 (세 축)

### 1) 데이터 엣지 그래프 배선 (b에서 미룸)
`GraphParser`가 검증한 데이터 엣지를 `GraphNode`에 저장 — 노드별
`입력포트 id → 소스 PortRef(생산노드, 출력포트)`. flow 배선(`outputs`)과 별개.

### 2) 값 해석 엔진 (exec push / pure pull)
`ValueResolver(graph, ExecContext)`:
- `input(nodeId, portId, Type<T>)` = 연결됐으면 소스 `output(...)`, 미연결이면 **InputSpec
  인라인 기본값**(코덱 파싱).
- `output(nodeId, portId, Type<T>)` = 생산노드 종류로 분기 — **EXEC**면 `ValueStore`에서
  read(미실행=unset→정의결과), **PURE**면 pull 평가(재귀, 결과를 `ValueStore`에 캐시).
- 참조 타입(player=UUID)은 사용 시점 resolve(ServerPlayer), 실패=unset.

### 3) 노드 데이터 I/O
- PURE: `evaluate(PureContext)` — `PureContext`가 resolver에 nodeId 바인딩 →
  `ctx.get(id, Type)` 해석, 출력 반환(캐시).
- EXEC: 스케줄러가 스텝마다 `ExecContext`의 current node를 세팅 → 노드가 `ctx.get(id, Type)`
  읽고 `ctx.set(id, Type, val)` push. pure pull 재귀는 별도 PureContext라 충돌 없음.

## 노드 종류 배정

- `get_variable`(**Pure**, 스토리지 동기 read → 값), `compare`(**Pure**, number×number→boolean),
  `format_text`(**Pure**, 템플릿 보간→string, 동적 입력 포트).
- `get_balance`(**Exec**, async → awaitAction/Suspend → number push).
- `branch_if`(**Exec**, boolean 입력 → true/false 분기) — compare→branch_if로 "값으로 흐름
  결정"(v1 미완).
- 트리거 명시 출력(**Exec**): `on_player_death`→victim/killer(player), `on_player_join`→player.

## 하위 단계

**수직 슬라이스 — 완료 (in-game 실증).** 슬라이스 소비 노드는 `get_variable`(스토리지
결합) 대신 **`compare`+`branch_if`**로 진행해 `PureContext`를 최소로 유지했다.
- [x] e-1. 데이터 엣지 그래프 배선(`GraphNode`가 kind별 러너블·`config`·`dataSources` 보유). 커밋 65a86b8.
- [x] e-2. `Type<T>` + `Types`(string/number/boolean/player) + `ValueResolver`(exec read/pure pull, 사이클 가드). 커밋 d3a068c.
- [x] e-3. 타입 컨텍스트(`PureContext.get`/`ExecContext.get·set` + 스케줄러 resolver·current node) + 인스턴스 인라인값(config) + 에디터 인라인/와이어 폼. 커밋 8ca6f9f·46d51de·224140c.
- [x] e-슬라이스. `compare`(Pure)·`branch_if`(Exec) + 데모 `on_player_join → branch_if(←compare(5,3,">")) → send_message`. **in-game 확인.**
- [x] **노드 종류 컴파일러 강제**: `ExecNodeType`/`PureNodeType` 분리(계약 a). pure는 flow 메서드 없음 → 순수성이 타입으로 보장. 커밋 186ac9e.

**확장 — 남음:**
- [ ] `get_balance`(Exec, async → awaitAction/Suspend → number push) — exec push 실증.
- [ ] `format_text`(Pure, 동적 입력 포트) — 값→텍스트 유일 명시 노드.
- [x] 트리거 명시 출력 **player**(`on_player_join`) — **첫 exec push 실증**. `dataOutPorts`에 `player` + `ctx.set` push, 소비자 `send_message.target`(연결 시 우선, 미연결=actor 폴백). in-game 확인. 커밋 61ae820.
- [ ] 트리거 명시 출력 **victim/killer**(`on_player_death`) — 이벤트 데이터(`LivingDeathEvent.getSource()`)를 `fireTrigger`가 받아 ValueStore seed하도록 확장 필요.
- [x] `get_variable`(Pure) — **PureContext 읽기전용 확장**. `PureContext.readVar(scope,key)` default 메서드(전체 StorageService 노출 대신 read만 → 순수성 유지, 읽기는 부작용 아님). `ValueResolver.PureView`가 exec ctx의 storage+actor로 해석. `value:string` 출력. 소비자로 `send_message.message`를 knob→연결 가능 string 입력으로 승격. in-game 확인. 커밋 d73b292.
- [ ] E2E 데모(economy/state 결합).

## 미룸

실행 계층 하드닝(스레드 안전·실패 격리, [0002 §9](0002-type-system.md))은 값이 흐르기
시작하면 우선순위로 재검토. resolveConnection 서브타이핑/autocast·컨테이너는 additive 미래.
