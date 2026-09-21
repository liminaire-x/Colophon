# Colophon 문서

Colophon 개발의 **근거·로드맵·설계 결정**을 담는다. 진행 상태의 한 줄 요약과
워크플로우는 리포지토리 루트의 [`CLAUDE.md`](../CLAUDE.md)에 있고, 이 폴더는 그
상세를 담는 단일 출처다. (이전에는 Trilium에 있었으나 2026-09-20 이관.)

## 문서 지도

| 파일 | 내용 |
|---|---|
| [roadmap.md](roadmap.md) | 버전별 로드맵, v2 데이터 포트 구현 체크리스트(a~f), 백로그 |
| [architecture.md](architecture.md) | 스택·코드 구조·런타임 실행 모델·영속화·핵심 설계 선택(이름/애드온/라이선스) |
| [decisions/0001-data-port-contract.md](decisions/0001-data-port-contract.md) | v2 데이터 포트 계약 a~e + 기각한 대안 |
| [decisions/0002-type-system.md](decisions/0002-type-system.md) | 타입 시스템 설계(표기·TypeId·number·연결 판정·실행 계층) |
| [decisions/0003-value-flow.md](decisions/0003-value-flow.md) | 값 흐름(계약 e): 데이터 배선 + resolver + typed I/O + 데이터 노드 + 동적 포트 + async 값 push |
| [decisions/0004-testing-strategy.md](decisions/0004-testing-strategy.md) | 테스트 전략: 3층 피라미드, 계약 스코프 원칙, mock 경계, 토큰·유지보수 관리 |
| [ideas.md](ideas.md) | 아이디어·미래 방향(NC 시네마틱, GeckoLib, 멀티서버 …) |
| [discussions.md](discussions.md) | 미해결 질문·재검토 대상 |

## ADR (decisions/)

`decisions/`는 Architecture Decision Records다. **결정과 그 근거, 기각한 대안**을
파일 하나에 담는다. 잠긴 결정은 재논의가 아니라 구현 대상이며, 뒤집으려면 새 ADR로
대체한다.

## graphify로 문서·코드 탐색

이 리포는 [graphify](https://github.com/safishamsi/graphify) 스킬로 코드+문서를 하나의
지식 그래프로 만들 수 있다. `/graphify`를 실행하면 설계 결정 노드와 코드 노드가 한
그래프로 연결되어 "이 결정이 어느 코드에 걸리나", "이 챕터가 무엇과 이어지나"를 질의로
볼 수 있다. **docs를 갱신한 뒤 그래프를 다시 생성**하면 문서 챕터가 노드로 반영된다.
(산출물 `graphify-out/`은 gitignore.)
