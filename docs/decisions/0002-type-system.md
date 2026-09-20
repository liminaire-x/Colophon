# ADR 0002 — Colophon 타입 시스템

> Colophon의 **타입 시스템 설계** — 결정·근거·코드·미해결 과제. [0001-data-port-contract.md](0001-data-port-contract.md)의 계약 b를 확장한다.
>
> **구현 상태(2026-09-20):** §11 "지금" 항목 중 bare 원시(§1)·`TypeId` 구조체(§3)·number=double(§6)는 d-1에서 반영 완료. `item_type`/`item_stack`(§4)·값 필드 분리(§2)는 아이템 노드가 생기는 e에서. 서브타이핑/autocast/컨테이너(§7 미래)·실행 계층(§9)은 미래.

---

## 0. 한눈에 (TL;DR)

- 타입은 **두 축**으로 구분: 표기(bare vs `네임스페이스:이름`), 제공자(코어 vs 애드온).
- **세 버킷**: ① 값 원시(bare) ② 코어 참조(`colophon:*`) ③ 애드온(`애드온:*`).
- **타입 id / 저장 인코딩 / 런타임 표현**을 3층으로 분리한다.
- 내부적으로 타입을 **문자열이 아니라 구조체(`TypeId`)**로 다룬다. (← `d-1` 전 반영할 핵심 #1)
- `item`은 모호하게 두지 말고 **`item_stack`(와이어 주력) / `item_type`(종류 조각)**으로 나눈다. (← `d-1` 전 반영할 핵심 #2)
- `number`는 IEEE754 double 하나. `int/float/double` 재분할 금지. 정확한 64-bit는 별도 `int64`(문자열 인코딩).
- 연결 판정은 전부 `resolveConnection(from, to)` 한 함수로. 지금은 exact match, 서브타이핑/변환은 미래에 여기 추가(additive).
- **남은 진짜 숙제는 타입 스킴이 아니라 "실행 계층"** — 서버 스레드 안전성, 실패 격리 등(§9).

### 결정 로그

| # | 결정 | 상태 |
|---|---|---|
| D1 | 값 원시(string/number/boolean)는 bare, 닫힌 enum | 확정 |
| D2 | 코어 참조·애드온 타입은 `네임스페이스:이름` | 확정 |
| D3 | 타입 id / 인코딩 / 런타임 표현 3층 분리 | 확정 |
| D4 | 내부 `TypeId`를 sealed 구조체로 | 확정 (d-1 반영) |
| D5 | `item_type` / `item_stack` 분리, item_stack 주력 | 확정 (d-1 반영) |
| D6 | `number`=double 단일, 별도 `int64`는 문자열 인코딩 | 확정 |
| D7 | `resolveConnection` 단일 연결 판정 함수 | 확정 |
| D8 | 서브타이핑·autocast는 미래, resolveConnection에 additive 추가 | 미래 |
| D9 | 서버 스레드 안전성·실패 격리 실행 계층 설계 | 미결(우선) |

---

## 1. 타입 스킴 — 두 축, 세 버킷

타입을 **표기 축**(bare vs `네임스페이스:이름`)과 **제공자 축**(코어 빌트인 vs 애드온)으로 구분한다. "빌트인이다"와 "bare다"는 별개다.

| 버킷 | 예시 | 표기 | 제공자 | 값 형태 |
|---|---|---|---|---|
| **① 값 원시** | `string`, `number`, `boolean` | bare | 코어(빌트인) | 리터럴 |
| **② 코어 참조** | `colophon:player`, `colophon:item_stack`, `colophon:item_type`, `colophon:vec3` | `colophon:` | 코어(빌트인) | kind별(§2) |
| **③ 애드온** | `economy:account`, `quest:objective` | `애드온:` | 애드온 | 애드온 정의 |

②와 ③은 **같은 메커니즘(네임스페이스)**, 소유자만 다르다. `colophon:` 네임스페이스와 bare 원시 이름은 코어 예약 — 애드온이 만들 수 없다. 애드온은 자기 네임스페이스 안에서 자유(이름이 겹쳐도 소유자가 다르면 다른 타입).

### 1.1 값 원시 (D1)

- 멤버: `string`, `number`, `boolean`. 엔진 하드코딩 **닫힌 enum 3개**.
  ```java
  enum PrimitiveType { STRING, NUMBER, BOOLEAN }   // int/double 없음
  ```
- `int/float/double`은 별개 타입이 아니라 전부 `number`(§6).
- 표기는 네임스페이스 없이 bare. `colophon:string` 같은 표기는 **사용하지 않음**(원시는 네임스페이스에 속하지 않음).

**근거**: 추상 값이라 소유자가 없다 / 집합이 작고 얼어(frozen) 있어 bare 예약어로 안전하며 안 늘어난다 / "bare=빌트인 원시, namespaced=등록된 것"이라는 작성자 직관과 일치 / JSON·일반 타입 시스템 관례.

### 1.2 코어 참조 (D2)

- 멤버: `colophon:player`, `colophon:item_stack`, `colophon:item_type`, `colophon:block_type`, `colophon:block_state`, `colophon:entity_type`, `colophon:entity`, `colophon:vec3`, `colophon:blockpos`, `colophon:enchantment` … (코어 제공, 확장 가능)

**bare가 아닌 이유**: 목록이 길고 MC 버전마다 늘어난다(bare 예약 공간을 부풀리면 안 됨) / 도메인 참조는 "제공/등록된 것"이라 네임스페이스 직관과 맞음 / 충돌·소유 명시(`colophon:player` vs `otheraddon:player`) / 공유 네임스페이스가 범용 호환을 준다(모두 같은 `colophon:*`를 참조 → 자동 연결). enum이 아니라 **공유 네임스페이스**가 범용성의 정체.

**`minecraft:`가 아니라 `colophon:`인 이유**: registry_ref가 MC 레지스트리를 참조하는 건 맞지만 그건 타입 정의의 `registry` 필드에 넣는다(§2.1). `player`/`vec3`는 대응 MC 레지스트리가 없어 `minecraft:`로 통일 불가. 타입 *정의*의 소유자는 Colophon.

### 1.3 애드온 (D2)

- 애드온이 자기 네임스페이스에 정의. `economy:account`, `quest:objective`.
- ②와 같은 메커니즘. 새 장치 불필요.

---

## 2. 타입 / 값 / 런타임 표현 — 3층 분리 (D3)

핵심 원칙: 아래 세 가지는 **서로 다른 것**이며 분리한다.

```
타입 id         무엇인가            colophon:player
저장 인코딩       어떻게 저장하나       "550e8400-..." (UUID 문자열)
런타임 표현       실행 시 실제 객체     ServerPlayer
```

타입과 값은 **같은 `ns:name` 모양이어도 필드 자리로 구분**된다(형식 수정 불필요):

```json
{ "type": "colophon:item_stack", "value": { "id": "minecraft:diamond", "count": 64 } }
```

콘텐츠가 폭발해도(Create 아이템 1만 개) 타입 공간은 고정 — 전부 item 타입의 **값**일 뿐. **새 종류 = 새 타입, 새 인스턴스 = 값**.

### 2.1 코어 참조의 kind별 정의

```
colophon:item_type   → { kind: registry_ref, registry: "minecraft:item" }   값: ResourceLocation
colophon:item_stack  → { kind: struct, fields: { item: item_type, count: number, components: ... } }
colophon:enchantment → { kind: registry_ref, registry: "minecraft:enchantment" }
colophon:player      → { kind: entity_ref }     값: UUID (레지스트리 아님)
colophon:vec3        → { kind: struct, fields: { x, y, z: number } }
```

- **registry_ref**: 값은 ResourceLocation. 내부적으로 `RegistryRef<R>`로 일반화하되 `RegistryRef<Item> ≠ RegistryRef<Block>` 유지(§8 리뷰 반영). Create 등 모드 레지스트리도 같은 방식으로 자동 지원.
- **entity_ref**: 값은 UUID(identity, 오프라인에도 존재). 런타임 표현은 살아있을 때만 존재 → resolve 실패 가능(§5).
- **struct**: 값은 인라인 데이터.

---

## 3. TypeId 구조체 (D4) — d-1 반영 핵심 #1

타입을 코드 곳곳에서 문자열로 비교하지 말고 얇은 구조체로 감싼다. **JSON 저장 형식은 안 바뀐다.**

```java
public sealed interface TypeId {
    record Builtin(String name) implements TypeId {}              // string/number/boolean
    record Named(String namespace, String name) implements TypeId {}  // colophon:player, economy:account

    static TypeId parse(String s) {
        int i = s.indexOf(':');
        return (i < 0)
            ? new Builtin(s)
            : new Named(s.substring(0, i), s.substring(i + 1));
    }

    default String asString() {
        return switch (this) {
            case Builtin b -> b.name();
            case Named n   -> n.namespace() + ":" + n.name();
        };
    }

    // 미래(additive): 아래를 permitted에 추가하면 됨
    // record RegistryRef(String registry) implements TypeId {}
    // record ListType(TypeId elem) implements TypeId {}
    // record OptionType(TypeId inner) implements TypeId {}
}
```

**이득 3가지**
1. 동작 100% 그대로(parse ↔ asString). 마이그레이션 없음. JSON엔 여전히 `"colophon:item_stack"`, `"number"`.
2. `type.equals("colophon:item")` 같은 문자열 특수 분기가 `switch`로 대체됨. record라 equals/hashCode 공짜 → 매칭은 `from.equals(to)`.
3. 컨테이너·registry_ref 등 미래 확장이 **여기 한 곳에 case 추가**로 붙음. 기존 코드 안 깨짐.

**검증/정규화**: `Builtin`은 enum 3개 중 하나인지 검증. bare로 안 알려진 이름(`float` 등)은 "알 수 없는 원시" 에러. `Named`의 namespace는 알려진 소유자인지 검증.

---

## 4. item_type / item_stack (D5) — d-1 반영 핵심 #2

MC 실물 구분:
- **`Item`**(=`item_type`): 등록된 **종류** 하나(diamond). 개수·데이터 없음. `net.minecraft.world.item.Item`.
- **`ItemStack`**(=`item_stack`): 실제 뭉치(diamond ×64 + 인챈트 등). 부가정보는 1.20.5+ Data Components. `net.minecraft.world.item.ItemStack`.

```
colophon:item_type   → 런타임 Item,      값 "minecraft:diamond"           (종류)
colophon:item_stack  → 런타임 ItemStack, 값 { id, count, components }      (실제 뭉치)
```

**역할 분담**
- `item_stack` = **와이어 주력**. 게임플레이 노드(주기/드롭/인벤 검사/손에 든 것)는 대부분 이것.
- `item_type` = **종류 고르기(registry_ref) + 비교용 + stack 재료**.
- 잇는 노드: `item_type + count → item_stack`(만들기), `item_stack → item_type`(종류 꺼내기).
- 직렬화는 바닐라 ItemStack 코덱 재사용.

**같은 규칙이 block/entity에도**: `block_type` vs `block_state`, `entity_type`(종) vs `entity`(살아있는 개체).

**쉬운 건 쉽게, 조립은 조립으로**: 입력 포트는 **인라인 리터럴로도, 와이어로도** 채울 수 있게(connectable). 정적이면 아이템 피커로 바로 지정, 동적이면 와이어. 둘 다 열어둔다.

### 4.1 예시 흐름 (인챈트 건 장비 주기)

인챈트는 **item_type이 아니라 item_stack에** 붙는다(Data Component라 stack에만). `enchantment`도 registry_ref 타입(`colophon:enchantment`).

```
item_type(diamond_sword) ─┐
count(1) ─────────────────┴─> [Enchant] ─> item_stack ─> [Give]
enchantment(sharpness) ──────────┘   ↑ 여기서 type→stack 전환
level(5) ────────────────────────────┘
```

Enchant 노드의 출력은 반드시 item_stack.

---

## 5. 값 읽기/쓰기 — ExecContext, Type\<T\>, 코덱

핵심 트릭: **타입 상수가 자기 런타임 자바 타입을 제네릭으로 들고 있다** → `ctx.get`/`ctx.set`이 캐스팅 없이 타입 안전.

```java
public abstract class Node {
    protected <T> InputPort<T>  input(String id, Type<T> type)  { ... }
    protected <T> OutputPort<T> output(String id, Type<T> type) { ... }
    public abstract void execute(ExecContext ctx);   // 개발자가 override
}

public class GiveItemNode extends Node {
    private final InputPort<ServerPlayer> PLAYER = input("player", Types.PLAYER);
    private final InputPort<ItemStack>    STACK  = input("stack",  Types.ITEM_STACK);

    @Override
    public void execute(ExecContext ctx) {
        ctx.get(PLAYER).getInventory().add(ctx.get(STACK).copy());
    }
}
```

- `Types.PLAYER`는 `Type<ServerPlayer>` — id(`colophon:player`) + 런타임 타입(ServerPlayer) + 코덱을 함께 보유.
- `ctx.get(PLAYER)`는 저장값(UUID)을 꺼내 **런타임 객체(ServerPlayer)로 resolve**까지 함.
- **resolve 실패 지점**: player 오프라인 등. 두 가지 접근:
  ```java
  ServerPlayer p = ctx.get(PLAYER);          // 없으면 노드 실행 스킵(엔진 처리)
  Optional<ServerPlayer> p = ctx.tryGet(PLAYER);  // 직접 다룰 때
  ```
  미래에 `Option<player>` 정식 타입이 생기면 `tryGet`이 그 자리에 대응.

### 5.1 왜 Player가 아니라 ServerPlayer

MC 클래스 계층: `Entity → LivingEntity → Player(추상, 클라/서버 공통) → ServerPlayer(서버측 실체)`.
게임 로직은 **서버(논리 서버)에서 실행**되므로 노드가 쥐는 객체는 항상 `ServerPlayer`. 서버 전용 API(메시지·게임모드·어드밴스먼트·ServerLevel)가 여기 있음. `Player`로 받으면 매번 캐스팅 필요 + 클라 플레이어 가능성까지 열려 부적합. (옛 매핑 `EntityPlayerMP`와 동일 개념.)

---

## 6. number 정밀도 계약 (D6)

- **`number` = 유한 IEEE754 double.** 정수 정확 표현 ±2⁵³(≈±9.007×10¹⁵). 소수 유효자리 15~17.
- `int/float/double` 재분할 **금지** — 쪼개면 연결마다 변환/확장 규칙 필요, 노드 에디터 대상엔 마찰. (Unreal도 UE5에서 float/double을 `real`로 통합.)
- 정수/소수 구분이 필요한 노드는 타입이 아니라 **필드 제약**("이 필드는 정수만")으로.
- **정확한 64-bit(월드 시드, long 카운터)**: `number`에 넣지 말 것(2⁵³ 초과 시 정밀도 붕괴). 별도 `colophon:int64` 타입을 두고, **JSON에서는 문자열로 인코딩**(JS `JSON.parse`가 2⁵³ 초과 정수를 뭉갬):
  ```json
  { "type": "colophon:int64", "value": "9223372036854775807" }
  ```

---

## 7. 매칭 & 확장점 (D7, D8)

- **매칭**: 타입 id 완전 일치. 지금은 이게 전부.
- **단일 진입점**: 모든 연결 판정을 한 함수로.
  ```java
  ConnectionResult resolveConnection(TypeId from, TypeId to);
  // 반환은 이유까지: { kind: EXACT | SUBTYPE | CONVERSION, converter? }  (디버깅에 유용)
  ```
- **미래(additive, 지금 구현 X)**:
  - **autocast 변환**: `(from,to)→변환노드`. v1은 2축이면 충분 — `implicit?`(자동 삽입) + `fallible?`(런타임 실패 가능). number→string은 자동, string→number는 수동/실패 가능. **단일 홉만**(A→B→C 자동 연쇄 금지), 애드온 확장 가능.
  - **서브타이핑**: MC 클래스 계층이 진실의 원천(`player <: entity`, `entity_type` 계층). resolveConnection에 규칙 추가. **문법상 additive**(기존 연결 안 깨고 새 연결만 허용).
  - **컨테이너/제네릭**: `List<T>`, `Option<T>`, `Map<K,V>`. mutable 컨테이너 도입 시 **variance 규칙** 필요(`List<player> <: List<entity>`를 무조건 허용하면 unsound). → 서브타이핑보다 **컨테이너 존재 여부를 먼저 결정**. TypeId 구조체(§3)가 이 자리를 이미 열어둠.

---

## 8. 외부 리뷰(GPT) 반영 정리

리뷰 전문은 대화 기록 참조. 판정 결과만:

**반영(좋은 지적)**
- 타입 id ≠ 타입 계약 → **스키마 버전**을 타입 정의에 둠(id엔 안 넣음). breaking change 시 새 type identity 발급.
- **의미 네이밍**: item_type/item_stack, block_type/block_state, entity_type/entity. (§4)
- **int64 문자열 인코딩** + number=double 계약 명시. (§6)
- **unknown 타입/노드 라운드트립 보존**(애드온 없이 연 그래프가 데이터 안 잃게). 소급 어려우니 일찍.
- **내부 canonical TypeId**(문자열 특수분기 방지). (§3)
- resolveConnection이 **이유 반환**. (§7)
- registry_ref 내부 일반화하되 `<Item>≠<Block>` 유지. (§2.1)

**미래로(맞지만 지금 아님)**
- 컨테이너/nullability, 서브타이핑+variance, refinement 제약, player_ref vs player_identity 분리(네이밍만 지금).

**반박(이 도메인엔 과설계)**
- **vec3를 역할별(position/velocity/direction) 분리 — 하지 말 것.** 모양 같은 것들 사이에 의미 없는 변환 노드가 양산됨(안티패턴). vec3 하나 유지, 역할은 노드 의미가 나름. (blockpos의 차원 컨텍스트는 MC처럼 pos+level **짝지어** 처리, 융합 타입 X.)
- **lifetime/scope를 타입 차원으로 — 과함.** entity 참조 stale 문제는 "resolve 실패 → Option"으로 흡수(§5), 별도 lifetime 타입 레이어 불필요.
- "버전이 resolveConnection보다 중요"라는 프레이밍은 층이 달라 경쟁 아님.

---

## 9. 실행 계층 — 남은 진짜 숙제 (D9)

타입 스킴은 견고. **리스크는 그 주변 실행 계층**에 있다.

| # | 항목 | 우선도 | 요지 |
|---|---|---|---|
| 1 | **서버 스레드 안전성** | 지금 설계 | MC 서버 로직은 메인 서버 스레드. execute가 월드/엔티티 건드리면 반드시 그 스레드. off-thread에서 ServerPlayer 접근=크래시. 비동기 노드 생기면 결과를 서버 스레드로 마샬링. `ExecContext`가 이를 보장/강제하게. |
| 2 | **실행 실패 격리** | 지금 | 그래프=사용자 코드. execute 예외로 서버가 죽으면 안 됨. try-catch로 감싸고, 실패를 작성자에게 표시, "노드 실패 시 그래프 정책"(중단/스킵/기본값) 정의. resolve 실패·0 나누기·없는 레지스트리 엔트리 포함. |
| 3 | **실행 폭주 한계** | 인지→미래 | 무한 루프, 틱당 대량 스폰 방지. 루프/재귀 캡, 틱당 예산, 노드 권한 제한. 그래프 공유/임포트 시 필수. |
| 4 | **런타임 성능** | 인지 | 20 TPS. 매 틱 대량 실행 시 UUID→ServerPlayer resolve, ResourceLocation→Item 조회, Double 박싱, 할당이 랙. 한 실행 내 resolve 캐시, 핫패스 할당 절감, number 박싱 여지. |
| 5 | **MC 버전 마이그레이션** | 미래(씨앗 지금) | MC 업데이트 시 레지스트리 rename·컴포넌트 포맷 변경 → 저장된 item_stack 값 로드 실패 가능. 저장 포맷 버전 + 바닐라 DataFixer 태우기. (스키마 버전과 같은 축.) |
| 6 | **디버깅·관측성** | 미래(채택률) | 와이어 값 인스펙트, 노드 로그, "왜 연결 안 됨" 설명(resolveConnection 이유 활용), 애드온 노드 테스트 수단. |
| 7 | **i18n·접근성** | 미래(소) | 타입 displayName 다국어(한국어 사용자), 소켓 색 색각 대응. |

참고: 초기 대화의 "exec push / pure pull" 계약이 이 실행 계층의 일부.

---

## 10. 애드온 개발자 관점 (요약)

개발자는 `TypeId` 내부도, 문자열도 안 본다. **두 가지만** 한다:
1. **기존 타입 쓰기**: `input("player", Types.PLAYER)` — 자동완성으로 상수 선택.
2. **새 타입 만들기**: 자기 네임스페이스에 등록.
   ```java
   public static final TypeId ACCOUNT = Types.named("economy", "account");
   reg.register(TypeDef.struct(ACCOUNT)
       .field("owner", Types.PLAYER)
       .field("balance", Types.NUMBER)
       .mapTo(Account.class)     // 런타임 표현 + 코덱
       .build());
   ```
3. 연결 검증은 엔진(resolveConnection)이 처리 — 개발자는 타입 비교 안 함.

**편의의 대가**: 새 타입은 코덱(저장값 ↔ 런타임 객체, resolve 실패 처리)을 한 번 정의해야 함. 이 등록 API를 쉽게 만드는 게 체감 친화도를 좌우 — 설계 시 주의.

---

## 11. d-1 체크리스트 (지금 할 것 vs 나중)

**지금 (d-1 전)**
- [ ] `TypeId` 구조체 도입, 문자열 비교를 switch로 대체 (§3)
- [ ] `colophon:item`을 실제 쓰임에 맞게 `item_stack`(주기/드롭 계열) 또는 `item_type`으로 확정, 나머지는 나중에 추가 (§4)
- [ ] 기존 브리지의 원시 3개(`string/number/boolean`)에서 `colophon:` 접두사 제거, player·참조는 유지 (§1)
- [ ] 저장 그래프에 `colophon:string` 등이 있으면 `string`으로 마이그레이션
- [ ] 값 필드(ResourceLocation/UUID/인라인)를 타입 필드와 분리 (§2)
- [ ] `number = double` 계약 문서화 (§6)

**나중 (additive)**
- [ ] resolveConnection 이유 반환, autocast, 서브타이핑, 컨테이너/nullability
- [ ] int64 타입(문자열 인코딩), 스키마 버전, unknown 라운드트립
- [ ] 실행 계층 §9 (단, 1·2는 실행 엔진 붙일 때 우선)

**영향 받는 코드(직전 작업 기준)**: `InputSpec.java`(타입 필드 표현), `NodeType.inputs()`, `NodeRegistry.schemaJson`(스키마에 타입 노출), `SetVariableNode.java` 등 원시/참조 타입을 쓰는 노드들. 기존 `string→colophon:string` 브리지 지점이 1차 수정 대상.

---

## 12. 용어집

- **TypeId**: 타입의 식별자. 내부는 sealed 구조체, 외부(JSON/UI)는 문자열.
- **registry_ref**: MC 레지스트리 엔트리를 가리키는 타입(item_type, block_type, enchantment 등). 값=ResourceLocation.
- **entity_ref**: 살아있는 개체 참조(player 등). 값=UUID, 런타임=live 객체(online 시만).
- **struct**: 필드 조합 타입(vec3, item_stack 등). 값=인라인.
- **item_type / item_stack**: 아이템 종류(Item) / 실제 뭉치(ItemStack, 개수·컴포넌트 포함).
- **Type\<T\>**: 런타임 자바 타입 T를 제네릭으로 든 타입 핸들. `ctx.get`을 타입 안전하게.
- **ExecContext**: 노드 실행 컨텍스트. `get(port)`/`set(port, v)`/`tryGet(port)`.
- **resolveConnection**: 두 타입 연결 가능 판정 단일 함수. 지금 exact, 미래 서브타이핑/변환.
- **connectable**: 포트를 와이어로 연결 가능한지(true) 아니면 인라인 리터럴 전용(false)인지.
