# 아이디어 · 백로그

확정 계약이 아니라, 미래 방향과 재사용 가능한 아이디어. 우선순위는 대체로 위→아래.

## 근시일 후보

- **에이전트 그래프 편집·생성 API** — 방향 확정: **in-process API**(에디터와 공유,
  Javalin/WebSocket 호스트), standalone CLI 안 함(스키마·검증·publish가 이미 그 프로세스에).
  에이전트 편집 = 드래프트+검증, 기본은 publish를 사람이 확정; 사용자가 켜는 옵션으로
  에이전트 직접 publish 허용(auto-approve식 opt-in). MCP = 그 API의 얇은 어댑터. 의존:
  Javalin 전환 + 편집 API 정식화. 문은 semantic ID·서버측 검증·validate/publish 분리로 열어둠.
- **Javalin 전환** — WebSocket(협업) + 라우팅 + 런타임 진단 push(→ unset·deprecation·마이그레이션
  경고) + 에이전트 편집 API 호스트.
- **마이그레이션 문서 템플릿** — 애드온 개발자 배포용(변경 범주 판정 / version·deprecate·
  redirect·migrator 작성법 / 리포트 읽는 법·스테이징 절차). SDK 경계 추출·README 정비와 묶음.
- **변수 레지스트리/카탈로그** — 변수 선언·관리(스코프·키·설명), 에디터 드롭다운. 키 충돌·
  오타 방지. 상태 노드의 scope enum 드롭다운이 첫 발.
- **웹 포트 Config화** — 워밍업용.

## 구조·확장

- **`sequence` 플로우 노드** — 다중 순차 플로우 출력(`then 0`·`then 1`·…)을 순서대로 발화.
  플로우 출력은 단일 와이어라 한 트리거로 N개를 하려면 지금은 체인(A→B→C)뿐인데, Unreal
  블루프린트 Sequence 노드와 동일한 관용구를 제공. `ExecNodeType`의 `flowOutPorts()`가 이미
  다중 명명 출력을 지원하므로(branch_if의 true/false와 같은 메커니즘) 노드 하나로 구현 가능 —
  스케줄러는 한 출력만 따라가므로 "각 then을 끝까지 실행 후 다음 then" 시맨틱(재진입/컨티뉴에이션)
  설계가 관건. 데이터 팬아웃과 대비되는 플로우 팬아웃 해법. (연결 다중도 관례는 Unreal과 일치:
  실행출력·데이터입력=단일, 데이터출력·실행입력=다중.)
- **애드온 패키징** — `implementation`→`compileOnly` + optional `mods.toml`, 별도 애드온 모드 분리.
- **애드온 SDK 경계 정식 추출** — `runtime`=SDK 표면, `nodes`=빌트인 애드온. → 경제/NPC 애드온.
- **다중 그래프(페이지)** / Suspend 이벤트 wake / 트리거 동적 구독.
- **멀티서버(Velocity)** — 공유 DB + Redis. 지금은 `StateBackend` 이음매만 유지.

## 콘텐츠 연동 (아이디어 단계)

- **NC 시네마틱 연동 노드** — '시네마틱' 노드 더블클릭 → 전용 서브에디터(멀티트랙 타임라인:
  오디오/애니메이션/카메라/대사, 영상 프리뷰 제외). 경량(NC 씬 이름 참조+재생/분기) vs
  중량(Colophon이 타임라인 저작=NC·GeckoLib 세밀 구동) → 우선 경량.
- **GeckoLib 애니메이션 매칭** — GeckoLib로 NC 대사 타이밍에 애니메이션 트리거. NC
  레코딩과 다른 소스 → 애니메이션 소스 인터페이스 추상화. 소프트뎁 애드온.

## 목표 유스케이스

- **NC 대사 분기**를 데이터 포트 계약의 목표 유스케이스로. 선행 = NC API 게이팅 확인(외부에서
  씬/대사 프로그램 시작 + 씬 종료·대사 선택 콜백).
