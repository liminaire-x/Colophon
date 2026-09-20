# ADR 0001 — v2 데이터 포트 계약 (a~e)

**상태:** 계약 a~e 확정(설계). 구현 진행 중(a~d 완료, e 다음). 진행 상태는
[roadmap.md](../roadmap.md).

## 목적

데이터 포트(포트 모델의 데이터 차원)는 **core↔애드온 SDK 표면**이자 **저장되는 그래프
포맷**이다. 바꾸면 애드온·저장 그래프가 깨진다 = 모드의 방향성. 그래서 "구현"이 아니라
**계약(contract)**으로 접근한다.

**관통 원칙:** 계약을 지금 잠그고, 그 아래 엔진 메커니즘(push/pull 구현 순서·캐시 정책)은
나중에 무손상 진화. 하이브리드(pure/impure) 채택, push-only 기각.

**관점:** 언리얼 블루프린트 재현, **세 곳만 더 엄격** — (1) 거대한 타입 시스템 X(얇은
조각만), (2) 암묵 자동 캐스트/ToString X, (3) `self` 문맥 기본값 X(d=균일 명시).

## 기각한 대안 (재논의 방지)

- **데이터 포트: push-only 기각 → 하이브리드.** async 생산자(balance=future)와 순수
  표현식(산술·비교)이 다른 노드 종류를 요구. push-only는 순수 계산을 exec 선에 억지로
  태워 저작성↓. pull(지연 평가)은 async를 못 다룸. Blueprints의 pure/impure 분리가 유일 해.
- **b 타입: 서브타이핑 기각 → 명목 매칭만.** 계층/variance = 토끼굴, O(1) 검증 유지. 타입
  간 다리는 명시 변환으로만.
- **b: coercion-to-string 기각 → 전용 `format_text`.** 임의 와이어 암묵 문자열 변환이
  제일 지저분 → 명시 자리 하나로.
- **d 주체: 암묵 "현재 액터" 기본값 기각 → 균일 명시.** 암묵 기본은 player만 특별
  취급(비대칭)+액터 없는 트리거(타이머)서 조용히 깨짐. 블루프린트 `self`는 그래프 소유
  객체라 안전하지만 Colophon엔 소유 객체 없음 → = 블루프린트 − self.
- **에이전트 편집: standalone CLI 기각 → in-process API.** 스키마·검증·publish가 이미 그
  프로세스에 삶. CLI는 중복+리소스 과대. 에디터·에이전트가 같은 API의 클라이언트.

## 계약 a — 노드 종류 모델

`ExecNode`/`PureNode` **2종 물리 분리(컴파일러 강제)**.

- **ExecNode**: exec-in/out, Suspend 가능, 부작용 가능, 데이터 출력을 value store에 write(push).
- **PureNode**: exec 핀 없음, 동기, 부작용 없음, `evaluate(inputs) → outputs`(pull).
- **강제 수단 = 컨텍스트 능력 분리**: PureNode는 전체 ExecContext 대신 **읽기 전용
  PureContext**만 받음 → Suspend·월드 변경·스토리지 write를 물리적으로 호출 불가.
- **매개 = per-execution value store 하나**: exec push / pure pull, 수명 = 플로우 실행 전체.
- **"순수" 정의**: 참조 투명성이 아니라 **부작용 없음 + 동기**. get_variable은 가변
  스토리지를 읽어도 순수(인메모리 캐시, 동기 read).
- 순수 노드는 실행당 **0..N회 호출을 견뎌야 함** → "순수 랜덤/카운터" 금지.
- exec 데이터 출력은 그 노드 **실행 후에만 유효** → 미실행 소비 = unset → 정의된 결과.

## 계약 b — 타입 시스템

열린 `TypeRegistry`(명목 매칭, 서브타이핑 X). 값(string/number/boolean) vs 참조(player=UUID,
resolve 실패=unset, serializable=false). 단일 number(decimal). flow/data category. 연결 시점
검증(`isValidConnection`, O(1), 불일치 빨간 줄), 암묵 자동변환 없음. 값→텍스트 = 전용
`format_text` Pure 노드.

표기·`TypeId` 구조체·number 계약·연결 판정 단일 진입점의 상세는
[0002-type-system.md](0002-type-system.md).

## 계약 c — 입력 통합

config 필드 + 데이터 입력을 **"입력" 하나**(`InputSpec`: 타입 + 인라인 기본값 +
connectable). connectable = 입력별 SDK 속성(기본 true, 구성 노브만 false). 미연결 = 인라인,
연결 = 와이어(우선). 입력 = 와이어 1 or 인라인, 출력 = fan-out.

## 계약 d — 주체 모델 = 균일 명시

주체는 값(마법 아님), 모든 참조 타입 균일(출력→입력), 예외 없음. 트리거는 주체를 **이름
있는 타입 출력**(death→victim/killer). 참조 입력 = 명시 연결·문맥 기본값 없음(미연결=unset).
exec 선 ≠ 데이터(앞 노드 대상 자동 안 물려감, 같은 대상=fan-out). = 블루프린트 − self.

## 계약 e — 직렬화·버전

- **포트 ID = 불변 semantic 문자열**(위치·라벨 아님, protobuf 번호처럼 안 바꿈). 읽는
  주체가 사람 아닌 에이전트·툴·diff·MCP라 자기설명적이 유리.
- **표시 이름 = 번역 키**(ID와 분리).
- **version = 노드 디스크립터 필드**(`default int version(){return 1;}`). ID/타입/구조/제거
  변경 시만↑. 애드온 버전 업그레이드 때 올림, 로드 시 사용자 고지.
- **마이그레이션 3층**: ① 관대한 파싱(항상, 안 터짐) ② 선언적 리다이렉트(rename) ③ 명시적
  마이그레이터(값·구조).
- **Deprecation = 노드 단위 soft**(플래그+메시지+에디터 배지).
- **진단**: `/api/validate` 드라이런(dev·스테이징, 라이브 불필요) + (Javalin 후) 에디터 push.

## unset 처리 — 실행순서 의존

- **두 도구 구분**: 정적 검증(publish 전 구조 판정) vs 런타임 경고(플레이 중 실제 unset).
  트레이드오프 정반대. 비싼 건 정적 검증 전부가 아니라 "실행순서 도달성" 한 조각뿐 —
  타입 불일치는 싸고(O(1)) 연결 시점 빨간 줄로 그대로 감.
- **확정 3단**: (1) 지금 = 런타임 정의결과 + 구조화 진단 + 포트 메타 "exec 산출물" 표식 →
  (2) WebSocket(Javalin) 후 에디터 push → 노드 경고 아이콘 → (3) 정적 도달성 분석 = 백로그.
- **열린 우려**: 정적 도달성 분석을 언제까지 미뤄도 되는지 미확정 → [discussions.md](../discussions.md).
