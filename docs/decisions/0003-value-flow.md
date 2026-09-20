# ADR 0003 — 값 흐름 (계약 e)

**상태:** 설계 확정, 구현 착수. 계약 a~d(표면)를 실제 동작으로 잇는 단계 — "값이 흐른다".
계약 근거는 [0001-data-port-contract.md](0001-data-port-contract.md), 타입은
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

**수직 슬라이스(먼저):**
- [ ] e-1. 데이터 엣지 그래프 배선(`GraphNode` dataSources, GraphParser가 구축).
- [ ] e-2. `Type<T>` + `Types` 레지스트리(number/boolean/string/player 코덱) + `ValueResolver`.
- [ ] e-3. 타입 컨텍스트(PureContext.get / ExecContext get·set + 스케줄러 exec push·current node).
- [ ] e-4. `get_variable`(Pure) + `send_message.message` connectable → **데모**: 변수값이
  와이어로 흘러 채팅 출력. (미연결=인라인/연결=와이어 실증.)

**확장:**
- [ ] e-5. `compare`(Pure) + `branch_if`(Exec) — 값으로 분기.
- [ ] e-6. `get_balance`(Exec, async) — exec push 실증.
- [ ] e-7. `format_text`(Pure, 동적 포트).
- [ ] e-8. 트리거 명시 출력(victim/killer, player).
- [ ] e-9. 에디터 인라인 폼(미연결=인라인/연결=와이어) 다듬기 + E2E 데모(economy/state).

## 미룸

실행 계층 하드닝(스레드 안전·실패 격리, [0002 §9](0002-type-system.md))은 값이 흐르기
시작하면 우선순위로 재검토. resolveConnection 서브타이핑/autocast·컨테이너는 additive 미래.
