# 0003 — NPC 외형: GeckoLib과 표준 리소스팩

2026-09-23 · 조각 3 · 고치는 비용: **중간** (운영자의 리소스팩이 이 이름 규칙에 기대게 된다)

## 결정

- NPC는 **GeckoLib 4.7**(NeoForge 1.21.1)으로 그린다. 서버와 플레이어 모두 GeckoLib이 필요하다(`neoforge.mods.toml`에 필수 의존성으로 선언).
- 모델 파일은 **표준 리소스팩**으로 전달한다. 지금은 운영자가 배포한다(RPG 서버는 보통 CurseForge 모드팩에 넣는다). Colophon은 **이름으로 가리키기만** 한다.
- NPC 정의(`npcs.json`, format 1)에 선택 항목 두 개를 더한다. 옛 파일도 그대로 읽힌다.

```json
{ "id": "chief", "name": "촌장", "model": "chief", "idle": "animation.chief.wave" }
```

`model: chief`이면 리소스팩에서 이 파일들을 찾는다.

```
assets/colophon/geo/npc/chief.geo.json
assets/colophon/animations/npc/chief.animation.json
assets/colophon/textures/npc/chief.png
```

- `idle`은 평소 반복하는 애니메이션 이름(Blockbench에서 지은 이름)이다.
- 노드 `Play NPC Animation`(NPC, 애니메이션 이름)은 한 번 재생하고 idle로 돌아간다. 그 NPC를 우클릭해서 시작된 실행이면 **우클릭한 그 배치만**, 아니면 로드된 모든 배치가 재생한다.
- 모델 파일이 없는 클라이언트는 **스티브 모습**으로 그린다(GeckoLib은 파일이 없으면 예외를 던지므로 먼저 확인한다).
- 서버는 "모델 이름 / idle / 한 번 재생 요청"만 엔티티 동기화 값으로 보내고, 실제 재생은 클라이언트가 한다.

## 알아둘 것

- 모드팩 `resourcepacks` 폴더의 팩은 **자동으로 켜지지 않는다**. Paxi나 Resource Pack Overrides 같은 모드로 기본 활성화해야 한다.
- 모델·애니메이션도 결국 클라이언트에 모두 내려간다. 스토리 텍스트만큼 민감하지 않다고 보고 받아들인다.

## 고르지 않은 것

- **모드 jar에 내장**: 새 모델마다 모드를 다시 빌드해야 한다.
- **서버가 모델을 직접 전송**: 유출은 가장 적지만, GeckoLib은 리소스를 다시 읽을 때 모델을 미리 구워두는 구조라 실행 중 주입이 깨지기 쉽다.
- **GeckoLib 트리거 애니메이션(`triggerableAnim`)**: 애니메이션 이름을 미리 코드에 등록해야 한다. 이름이 콘텐츠(데이터)에서 오므로 동기화 값 방식을 택했다.
