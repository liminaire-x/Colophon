# 미해결 질문 · 재검토 대상

잠긴 계약이 아니라, 아직 열려 있거나 다시 볼 항목.

## 정적 도달성 분석을 언제까지 미룰 수 있나 (열림)

exec 산출 데이터가 소비자보다 실행순서상 먼저인지 publish 시 검증하는 분석. unset 처리
3단 중 (3)에 해당하며 **백로그**로 두었으나, "언제까지 런타임 경고만으로 버틸 수 있는지"가
미확정. 재검토 대상. (배경: [decisions/0001-data-port-contract.md](decisions/0001-data-port-contract.md) unset 처리.)

## 실행 계층 — 값이 흐를 때 우선 (부분 열림)

타입 스킴은 견고하나 그 주변 **실행 계층**이 진짜 숙제. e에서 값이 흐르기 시작하면 우선순위:

- **서버 스레드 안전성** — execute가 월드/엔티티를 건드리면 반드시 메인 스레드. 비동기
  노드의 결과를 메인으로 마샬링. `ExecContext`가 보장/강제하게.
- **실행 실패 격리** — 그래프 = 사용자 코드. execute 예외로 서버가 죽으면 안 됨. try-catch
  + 실패 표시 + "노드 실패 시 정책"(중단/스킵/기본값). resolve 실패·0 나누기 포함.

상세: [decisions/0002-type-system.md](decisions/0002-type-system.md) §9.

## 미래로 미룬 것 (지금 결정 불필요)

- 컨테이너/nullability(`List<T>`, `Option<T>`) — 도입 시 variance 규칙 필요. **컨테이너 존재
  여부를 서브타이핑보다 먼저 결정.** `TypeId` 구조체가 자리를 열어둠.
- 서브타이핑(MC 클래스 계층) + autocast(단일 홉) — `resolveConnection`에 additive 추가.
- `player_ref` vs `player_identity` 분리 — 지금은 네이밍만.
