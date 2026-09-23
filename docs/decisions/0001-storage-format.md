# 0001 — 저장 형식: 그래프 문서와 기록 DB

2026-09-23 · 조각 1 · 고치는 비용: **비쌈** (디스크에 남는다)

## 그래프 문서 (`config/colophon/graphs.json`, format 1)

```json
{
  "format": 1,
  "graphs": [
    {
      "id": "graph_p4x81czt",
      "name": "첫 인사",
      "nodes": [
        { "id": "node_k3f9x2ma", "type": "colophon:on_player_join", "config": {}, "pos": [100, 80] },
        { "id": "node_w81bq0zd", "type": "colophon:has_flag", "config": { "flag": "greeted" }, "pos": [300, 80] }
      ],
      "links": [
        { "from": "node_k3f9x2ma", "to": "node_w81bq0zd" },
        { "from": "node_w81bq0zd", "out": "no", "to": "node_…" }
      ]
    }
  ]
}
```

- **`format`**: 형식 번호. 더 높은 번호는 읽지 않고 거부한다. 형식을 바꿀 땐 번호를 올리고 옛 번호를 변환하는 코드를 같이 쓴다.
- **그래프는 여러 개.** `id`는 불변(규칙은 [0004](0004-ids-and-record-keys.md)), `name`은 사람이 보는 이름이라 언제든 바꿔도 된다.
- **노드 종류 id에 `colophon:` 접두사.** 애드온 노드와 충돌하지 않게.
- **`links`는 실행 순서만.** `out`을 생략하면 `next`. 한 갈래는 한 노드로만 이어진다.
- **`pos`도 서버가 저장**한다. 다른 PC의 에디터에서도 배치가 같게 보인다.
- **서버가 형식의 주인.** 에디터가 이 형식으로 변환해 보낸다. publish 때 잘못된 곳은 모두 모아 거부하고, 시작 때 파일이 깨져 있으면 파일은 건드리지 않고 아무 그래프도 실행하지 않는다.

## 기록 DB (`config/colophon/records.mv.db`, H2, schema 1)

```
records(owner_kind, owner_id, k, v)   PRIMARY KEY (owner_kind, owner_id, k)
meta(k, v)                            schema_version = 1
```

- **주인(owner)** = `player`(UUID) / `server`(빈 id). 파티 같은 새 주인은 종류만 추가하고 테이블은 그대로.
- **키에 종류 접두사**: 표식은 `flag_greeted`. 다른 기록과 이름이 겹치지 않게. (처음엔 `flag:`였다가 [0004](0004-ids-and-record-keys.md)에서 바뀜)
- 메모리에서 읽고 쓰고, **월드 저장 때 함께** DB에 쓴다.

## 함께 정한 것

- **데이터 선 없음.** 행동의 대상은 "이 사건의 플레이어". 나중에 노드 설정 `target`(기본값 = 사건의 플레이어)으로 넓히면 옛 그래프가 깨지지 않는다.
- **테스트**: `GraphFormatTest`·`GraphBuilderTest`(그래프 문서), `RecordStoreTest`·`H2RecordBackendTest`(기록 DB).

## 고르지 않은 것

- **에디터 라이브러리 구조를 그대로 저장**(v2): 저장 형식이 React Flow에 묶인다.
- **v2 스코프 PLAYER/GLOBAL/LOCAL**: "누구의 기록인가"와 "어디에 저장하나"가 한 단어에 섞여 있었다.
- **NeoForge 플레이어 첨부 데이터**: DB 없이 더 단순하지만, 소유자가 H2 유지를 택했다.
