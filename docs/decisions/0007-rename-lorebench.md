# 0007 — 이름: Colophon → Lorebench

2026-09-24 · 고치는 비용: **비쌈** (mod_id는 월드·문서·패킷에 남는다. 첫 공개 뒤에는 사실상 못 바꾼다)

## 결정

| 대상 | 전 | 후 |
|---|---|---|
| 표시 이름 | Colophon | **Lorebench** (lore + workbench: 이야기를 만드는 작업대) |
| mod_id·네임스페이스 | `colophon` | `lorebench` (엔티티 `lorebench:npc`, 노드 id `lorebench:…`, 패킷, 리소스 `assets/lorebench/…`) |
| 명령어 | `/colophon` | `/lorebench` |
| 데이터 폴더 | `config/colophon/` | `config/lorebench/` |
| 패키지·클래스 | `kr.guinnessgroup.colophon`, `Colophon*` | `kr.guinnessgroup.lorebench`, `Lorebench*` |
| Gradle 프로젝트 | 폴더 이름을 따름 | `rootProject.name = 'lorebench'` |
| GitHub 저장소 | `Colophon` | `Lorebench` (소유자가 바꿈) |

- "Colophon"(책 끝의 간기)은 무엇을 하는 모드인지 드러나지 않았다. 소유자가 에디터를 **작업대**(그래프·퀘스트·NPC…)로 나누는 방향을 잡으면서 그 개념이 담긴 이름을 골랐다.
- 개발 단계라 mod_id까지 함께 바꾸고 로컬 데이터는 초기화했다(개발 단계에서는 형식이 바뀌면 변환 코드 대신 초기화). 옛 `run/config/colophon/`은 백업으로 남는다.
- 결정 기록 0001~0006의 본문은 당시 이름 그대로 두고, 머리에 이 기록을 가리키는 한 줄만 달았다.

게임 확인 완료(2026-09-24): 모드 목록의 Lorebench, `/lorebench npc spawn`과 촌장 모델(리소스팩 `lorebench-test`), `J` 화면, `run/config/lorebench/` 생성.

## 이름 확인 (2026-09-24)

- Modrinth: 검색 결과 0건, `lorebench` 프로젝트 주소 없음. CurseForge: 자동 확인이 막혀 웹 검색으로만 확인(해당 이름의 모드 없음). GitHub: `lorebench` 계정 없음.
- 같은 이름의 **테이블탑 RPG 게임 마스터 도구**가 있다(itch.io `lorebench`, GitHub `lorebench-tools`). 분야가 다르고, 활발하거나 널리 알려지지 않았으며, 이 프로젝트는 수익화 없는 공개 코드라 소유자가 문제없다고 판단했다.

## 고르지 않은 것

- **표시 이름만 바꾸기**: 비용은 쌌지만 내부 이름(`colophon`)과 겉 이름이 영영 달라진다. 지금이 mod_id를 바꿀 수 있는 가장 싼 때였다.
- **Storyloom, Questwright**: 각각 "베틀", "퀘스트 장인". Questwright는 퀘스트에만 한정되는 느낌.
