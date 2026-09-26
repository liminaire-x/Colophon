# Lorebench 문서

| 파일 | 내용 |
|---|---|
| [map.md](map.md) | **한 장짜리 지도**: 세 가지 재료(콘텐츠·로직·기록), 개념, 고치는 비용 |
| [stories.md](stories.md) | 지금까지 만든 이야기: 플레이어가 겪는 장면과 필요한 콘텐츠 |
| [roadmap.md](roadmap.md) | 진행 상태 (상태는 여기에만) |
| [workflow.md](workflow.md) | 작업 방식: 조각 흐름, 완료 정의, 게임 확인 형식, 에셋 작업(Blockbench → GeckoLib) |
| `decisions/` | 비싼 결정만 짧게 기록. 한 번 쓰면 고치지 않고, 바꿀 땐 새 기록으로 대체 |

## 문서 규칙

- **같은 사실은 한 곳에만.** 개념은 map, 이야기는 stories, 상태는 roadmap, 비싼 결정의 근거는 decisions.
- **"잠김" 대신 고치는 비용**(비쌈 / 중간 / 쌈)을 표시한다.
- **이야기가 요구하는 것만** 쓴다. 쓰는 곳이 없는 개념을 미리 설계하지 않는다.
- 소유자가 쉬운 말로 설명할 수 없는 결정은 확정하지 않는다.

## 옛 문서 (legacy-v2)

2026-09-23 이야기 중심으로 다시 시작하면서 v2 문서(ADR 0001~0005, architecture, domain 등)를
보관했다. git 태그 `legacy-v2`에 있고, 형제 폴더 `../Colophon-legacy`에 꺼내 두었다
(`git worktree`). 반면교사로 읽되 기준으로 삼지 않는다.
