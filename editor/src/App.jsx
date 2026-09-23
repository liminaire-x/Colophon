import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import {
  ReactFlow,
  Background,
  Controls,
  MiniMap,
  Handle,
  Position,
  applyNodeChanges,
  applyEdgeChanges,
} from '@xyflow/react'
import '@xyflow/react/dist/style.css'

// Must match the server (GraphFormat.java, NpcFormat.java, QuestFormat.java).
const FORMAT = 1
const NPC_FORMAT = 1
const QUEST_FORMAT = 1
const NEXT = 'next'

// Ids are made up here, never typed or edited: <kind>_<8 random a-z0-9> (Ids.java).
const ID_CHARS = 'abcdefghijklmnopqrstuvwxyz0123456789'
function newId(kind, taken) {
  for (;;) {
    const bytes = crypto.getRandomValues(new Uint8Array(8))
    const id = `${kind}_${Array.from(bytes, (b) => ID_CHARS[b % ID_CHARS.length]).join('')}`
    if (!taken.includes(id)) return id
  }
}

const CATEGORY_COLORS = { trigger: '#2a7d4f', condition: '#7c3aed', action: '#2563eb' }
const catColor = (c) => CATEGORY_COLORS[c] || '#555'

// --- saved document (server format) <-> React Flow ---

const edgeId = (from, out) => `${from}:${out}` // one link per way out, so this is unique

function toFlow(g) {
  return {
    id: g.id,
    name: g.name,
    nodes: g.nodes.map((n) => ({
      id: n.id,
      type: 'colophon',
      position: { x: n.pos?.[0] ?? 0, y: n.pos?.[1] ?? 0 },
      data: { type: n.type, config: n.config || {} },
    })),
    edges: g.links.map((l) => {
      const out = l.out ?? NEXT
      return { id: edgeId(l.from, out), source: l.from, sourceHandle: out, target: l.to }
    }),
  }
}

function toDoc(graphs) {
  return {
    format: FORMAT,
    graphs: graphs.map((g) => ({
      id: g.id,
      name: g.name,
      nodes: g.nodes.map((n) => ({
        id: n.id,
        type: n.data.type,
        config: n.data.config,
        pos: [Math.round(n.position.x), Math.round(n.position.y)],
      })),
      links: g.edges.map((e) => {
        const link = { from: e.source, to: e.target }
        if (e.sourceHandle && e.sourceHandle !== NEXT) link.out = e.sourceHandle
        return link
      }),
    })),
  }
}

// --- node on the canvas ---

const SchemaContext = createContext({})

const handleStyle = { width: 10, height: 10, background: '#e5e7eb', border: '1px solid #4b5563' }

function ColophonNode({ data, selected }) {
  const def = useContext(SchemaContext)[data.type]
  if (!def) {
    return (
      <div style={{ border: '2px solid #c0392b', borderRadius: 8, background: '#fff', padding: 8, fontSize: 12 }}>
        <Handle type="target" position={Position.Left} style={handleStyle} />
        unknown: {data.type}
      </div>
    )
  }
  const outs = def.outs || []
  return (
    <div
      style={{
        border: `2px solid ${selected ? '#111' : '#c9c9c9'}`,
        borderRadius: 8,
        background: '#fff',
        minWidth: 170,
        fontSize: 12,
        boxShadow: '0 1px 3px rgba(0,0,0,0.12)',
      }}
    >
      <div style={{ padding: '5px 10px', background: catColor(def.category), color: '#fff', borderRadius: '6px 6px 0 0', fontWeight: 600 }}>
        {def.label}
      </div>
      <div style={{ position: 'relative', padding: '4px 10px', minHeight: 18 }}>
        {!def.trigger && <Handle type="target" position={Position.Left} style={handleStyle} />}
        {(def.fields || []).map((f) => (
          <div key={f.id} style={{ color: '#555', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: 190 }}>
            {f.label}: {data.config?.[f.id] ?? ''}
          </div>
        ))}
      </div>
      {outs.map((out) => (
        <div key={out} style={{ position: 'relative', textAlign: 'right', padding: '2px 14px', fontSize: 10, color: '#374151', fontWeight: 600 }}>
          {outs.length > 1 ? out : ' '}
          <Handle type="source" id={out} position={Position.Right} style={handleStyle} />
        </div>
      ))}
    </div>
  )
}

const nodeTypes = { colophon: ColophonNode }

const input = { width: '100%', padding: '4px 6px', boxSizing: 'border-box' }

// A quest's goals or rewards: rows of item + count. Rewards (`wide`) take a whole
// /give line such as minecraft:iron_sword[custom_name=...], so the item gets its own line.
function StackList({ stacks, onChange, wide = false }) {
  const set = (i, key, value) => onChange(stacks.map((s, j) => (j === i ? { ...s, [key]: value } : s)))
  return (
    <div style={{ marginBottom: 10 }}>
      {stacks.map((s, i) => {
        const count = (
          <input
            type="number" min={1} value={s.count}
            onChange={(e) => set(i, 'count', e.target.value === '' ? '' : Number(e.target.value))}
            style={{ ...input, width: 52 }}
          />
        )
        const remove = <button onClick={() => onChange(stacks.filter((_, j) => j !== i))} style={{ cursor: 'pointer' }}>×</button>
        return wide ? (
          <div key={i} style={{ marginBottom: 6 }}>
            <textarea
              value={s.item} rows={2} placeholder="minecraft:iron_sword[custom_name='&quot;...&quot;']"
              onChange={(e) => set(i, 'item', e.target.value)}
              style={{ ...input, resize: 'vertical', fontFamily: 'monospace', fontSize: 11 }}
            />
            <div style={{ display: 'flex', gap: 4, justifyContent: 'flex-end' }}>{count}{remove}</div>
          </div>
        ) : (
          <div key={i} style={{ display: 'flex', gap: 4, marginBottom: 3 }}>
            <input value={s.item} placeholder="minecraft:wheat" onChange={(e) => set(i, 'item', e.target.value)} style={{ ...input, flex: 1 }} />
            {count}{remove}
          </div>
        )
      })}
      <button onClick={() => onChange(stacks.concat({ item: '', count: 1 }))} style={{ cursor: 'pointer', color: '#2563eb' }}>+ add</button>
    </div>
  )
}

// --- app ---

export default function App() {
  const [schema, setSchema] = useState([])
  const [graphs, setGraphs] = useState([])
  const [currentId, setCurrentId] = useState(null)
  const [selectedNodeId, setSelectedNodeId] = useState(null)
  const [npcs, setNpcs] = useState([]) // [{ id, name }]
  const [placements, setPlacements] = useState({}) // { npcId: [{ dim, x, y, z }] }
  const [selectedNpcId, setSelectedNpcId] = useState(null)
  const [quests, setQuests] = useState([]) // server format: [{ id, title, icon?, text?, goals, rewards }]
  const [selectedQuestId, setSelectedQuestId] = useState(null)
  const [status, setStatus] = useState('connecting...')
  const [message, setMessage] = useState(null) // { ok, text }
  const [publishing, setPublishing] = useState(false)

  const byType = useMemo(() => Object.fromEntries(schema.map((d) => [d.type, d])), [schema])
  const current = graphs.find((g) => g.id === currentId) || null
  const selectedNpc = npcs.find((n) => n.id === selectedNpcId) || null
  const selectedQuest = quests.find((q) => q.id === selectedQuestId) || null

  const loadPlacements = useCallback(async () => {
    try {
      setPlacements(await fetch('/api/npc-placements').then((r) => r.json()))
    } catch (e) { /* shown as offline elsewhere */ }
  }, [])

  useEffect(() => {
    let cancelled = false
    async function boot() {
      try {
        const s = await fetch('/api/schema').then((r) => r.json())
        const doc = await fetch('/api/graphs').then((r) => r.json())
        const npcDoc = await fetch('/api/npcs').then((r) => r.json())
        const questDoc = await fetch('/api/quests').then((r) => r.json())
        if (cancelled) return
        setSchema(s.nodes || [])
        const loaded = (doc.graphs || []).map(toFlow)
        setGraphs(loaded)
        setCurrentId(loaded[0]?.id ?? null)
        setNpcs(npcDoc.npcs || [])
        setQuests(questDoc.quests || [])
        setStatus('ok')
        loadPlacements()
      } catch (e) {
        if (!cancelled) setStatus('offline')
      }
    }
    boot()
    return () => { cancelled = true }
  }, [loadPlacements])

  // One thing is shown on the right at a time: a node, an NPC or a quest.
  const selectNode = useCallback((id) => { setSelectedNodeId(id); setSelectedNpcId(null); setSelectedQuestId(null) }, [])
  const selectNpc = useCallback((id) => { setSelectedNpcId(id); setSelectedNodeId(null); setSelectedQuestId(null) }, [])
  const selectQuest = useCallback((id) => { setSelectedQuestId(id); setSelectedNodeId(null); setSelectedNpcId(null) }, [])

  const updateCurrent = useCallback(
    (fn) => setGraphs((gs) => gs.map((g) => (g.id === currentId ? fn(g) : g))),
    [currentId],
  )

  const onNodesChange = useCallback(
    (changes) => updateCurrent((g) => ({ ...g, nodes: applyNodeChanges(changes, g.nodes) })),
    [updateCurrent],
  )
  const onEdgesChange = useCallback(
    (changes) => updateCurrent((g) => ({ ...g, edges: applyEdgeChanges(changes, g.edges) })),
    [updateCurrent],
  )

  // A way out leads to one node: connecting it again replaces the old link.
  const onConnect = useCallback((c) => {
    const out = c.sourceHandle || NEXT
    const id = edgeId(c.source, out)
    updateCurrent((g) => ({
      ...g,
      edges: g.edges.filter((e) => e.id !== id).concat({ id, source: c.source, sourceHandle: out, target: c.target }),
    }))
  }, [updateCurrent])

  const isValidConnection = useCallback((c) => {
    if (!current || c.source === c.target) return false
    const target = current.nodes.find((n) => n.id === c.target)
    const def = target && byType[target.data.type]
    return !!def && !def.trigger
  }, [current, byType])

  const addNode = useCallback((def) => {
    if (!current) return
    const id = newId('node', current.nodes.map((n) => n.id))
    const config = Object.fromEntries((def.fields || []).map((f) => [f.id, f.default ?? '']))
    const node = {
      id,
      type: 'colophon',
      position: { x: 150 + Math.random() * 150, y: 100 + Math.random() * 150 },
      data: { type: def.type, config },
    }
    updateCurrent((g) => ({ ...g, nodes: g.nodes.concat(node) }))
    selectNode(id)
  }, [current, updateCurrent, selectNode])

  const selectedNode = current?.nodes.find((n) => n.id === selectedNodeId) || null
  const selectedDef = selectedNode ? byType[selectedNode.data.type] : null

  const setConfig = useCallback((key, value) => {
    updateCurrent((g) => ({
      ...g,
      nodes: g.nodes.map((n) => (n.id === selectedNodeId
        ? { ...n, data: { ...n.data, config: { ...n.data.config, [key]: value } } }
        : n)),
    }))
  }, [selectedNodeId, updateCurrent])

  const deleteNode = useCallback(() => {
    updateCurrent((g) => ({
      ...g,
      nodes: g.nodes.filter((n) => n.id !== selectedNodeId),
      edges: g.edges.filter((e) => e.source !== selectedNodeId && e.target !== selectedNodeId),
    }))
    setSelectedNodeId(null)
  }, [selectedNodeId, updateCurrent])

  const newGraph = useCallback(() => {
    const name = window.prompt('Graph name:')
    if (name == null || !name.trim()) return
    const id = newId('graph', graphs.map((g) => g.id))
    setGraphs((gs) => gs.concat({ id, name: name.trim(), nodes: [], edges: [] }))
    setCurrentId(id)
    setSelectedNodeId(null)
  }, [graphs])

  const deleteGraph = useCallback(() => {
    if (!current || !window.confirm(`Delete graph '${current.name}'? (takes effect on publish)`)) return
    const rest = graphs.filter((g) => g.id !== current.id)
    setGraphs(rest)
    setCurrentId(rest[0]?.id ?? null)
    setSelectedNodeId(null)
  }, [current, graphs])

  const newNpc = useCallback(() => {
    const name = window.prompt('NPC name (shown above the NPC in game):')
    if (name == null || !name.trim()) return
    const id = newId('npc', npcs.map((n) => n.id))
    setNpcs((ns) => ns.concat({ id, name: name.trim() }))
    selectNpc(id)
  }, [npcs, selectNpc])

  // Edit one field of the selected NPC; an emptied optional field is dropped.
  const setNpcField = useCallback((key, value) => {
    setNpcs((ns) => ns.map((n) => {
      if (n.id !== selectedNpcId) return n
      const next = { ...n, [key]: value }
      if (key !== 'name' && value.trim() === '') delete next[key]
      return next
    }))
  }, [selectedNpcId])

  const deleteNpc = useCallback(() => {
    if (!selectedNpc) return
    const placed = (placements[selectedNpc.id] || []).length
    const warning = placed ? `\n${placed} placed in the world will disappear on publish.` : ''
    if (!window.confirm(`Delete NPC '${selectedNpc.name}'?${warning}`)) return
    setNpcs((ns) => ns.filter((n) => n.id !== selectedNpc.id))
    setSelectedNpcId(null)
  }, [selectedNpc, placements])

  const newQuest = useCallback(() => {
    const title = window.prompt('Quest title:')
    if (title == null || !title.trim()) return
    const id = newId('quest', quests.map((q) => q.id))
    setQuests((qs) => qs.concat({ id, title: title.trim(), goals: [], rewards: [] }))
    selectQuest(id)
  }, [quests, selectQuest])

  // Edit one field of the selected quest; an emptied optional text field is dropped.
  const setQuestField = useCallback((key, value) => {
    setQuests((qs) => qs.map((q) => {
      if (q.id !== selectedQuestId) return q
      const next = { ...q, [key]: value }
      if ((key === 'icon' || key === 'text') && value.trim() === '') delete next[key]
      return next
    }))
  }, [selectedQuestId])

  const deleteQuest = useCallback(() => {
    if (!selectedQuest) return
    if (!window.confirm(`Delete quest '${selectedQuest.title}'? Players keep their progress records.`)) return
    setQuests((qs) => qs.filter((q) => q.id !== selectedQuest.id))
    setSelectedQuestId(null)
  }, [selectedQuest])

  const publish = useCallback(async () => {
    setPublishing(true)
    setMessage(null)
    try {
      const res = await fetch('/api/publish', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          graphs: toDoc(graphs),
          npcs: { format: NPC_FORMAT, npcs },
          quests: { format: QUEST_FORMAT, quests },
        }),
      })
      const data = await res.json()
      setMessage(data.accepted
        ? { ok: true, text: 'Published.' }
        : { ok: false, text: (data.errors || ['unknown error']).join('\n') })
      if (data.accepted) loadPlacements()
    } catch (e) {
      setMessage({ ok: false, text: 'Publish error: ' + e })
    } finally {
      setPublishing(false)
    }
  }, [graphs, npcs, quests, loadPlacements])

  const palette = useMemo(() => {
    const g = {}
    schema.forEach((d) => { (g[d.category] = g[d.category] || []).push(d) })
    return g
  }, [schema])

  const button = { display: 'block', width: '100%', textAlign: 'left', padding: '5px 8px', marginBottom: 3, cursor: 'pointer', border: '1px solid #ddd', borderRadius: 5, background: '#fafafa' }
  const sectionTitle = { fontWeight: 600, margin: '4px 0 6px' }

  return (
    <SchemaContext.Provider value={byType}>
      <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
        <header style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '10px 16px', borderBottom: '1px solid #ddd' }}>
          <strong style={{ fontSize: 18 }}>Colophon</strong>
          <span style={{ fontSize: 13, color: status === 'ok' ? '#2a7d4f' : '#c0392b' }}>server: {status}</span>
          <span style={{ flex: 1 }} />
          <button onClick={publish} disabled={publishing || status !== 'ok'} style={{ padding: '6px 14px', cursor: 'pointer' }}>
            {publishing ? 'Publishing...' : 'Publish'}
          </button>
        </header>
        {message && (
          <div
            onClick={() => setMessage(null)}
            style={{ padding: '6px 16px', fontSize: 12, whiteSpace: 'pre-wrap', cursor: 'pointer',
              background: message.ok ? '#ecfdf5' : '#fef2f2', color: message.ok ? '#065f46' : '#991b1b' }}
          >
            {message.text}
          </div>
        )}

        <div style={{ flex: 1, minHeight: 0, display: 'flex' }}>
          <aside style={{ width: 200, borderRight: '1px solid #ddd', overflowY: 'auto', padding: 10, fontSize: 12 }}>
            <div style={sectionTitle}>Graphs</div>
            {graphs.map((g) => (
              <button
                key={g.id}
                onClick={() => { setCurrentId(g.id); setSelectedNodeId(null) }}
                style={{ ...button, background: g.id === currentId ? '#e0e7ff' : '#fafafa' }}
              >
                {g.name} <span style={{ color: '#888', fontSize: 10 }}>{g.id}</span>
              </button>
            ))}
            <button onClick={newGraph} disabled={status !== 'ok'} style={{ ...button, color: '#2563eb' }}>+ New graph</button>

            <div style={{ ...sectionTitle, marginTop: 14 }}>NPCs</div>
            {npcs.map((n) => (
              <button
                key={n.id}
                onClick={() => selectNpc(n.id)}
                style={{ ...button, background: n.id === selectedNpcId ? '#e0e7ff' : '#fafafa' }}
              >
                {n.name} <span style={{ color: '#888', fontSize: 10 }}>{n.id} · {(placements[n.id] || []).length} placed</span>
              </button>
            ))}
            <button onClick={newNpc} disabled={status !== 'ok'} style={{ ...button, color: '#2563eb' }}>+ New NPC</button>

            <div style={{ ...sectionTitle, marginTop: 14 }}>Quests</div>
            {quests.map((q) => (
              <button
                key={q.id}
                onClick={() => selectQuest(q.id)}
                style={{ ...button, background: q.id === selectedQuestId ? '#e0e7ff' : '#fafafa' }}
              >
                {q.title} <span style={{ color: '#888', fontSize: 10 }}>{q.id}</span>
              </button>
            ))}
            <button onClick={newQuest} disabled={status !== 'ok'} style={{ ...button, color: '#2563eb' }}>+ New quest</button>

            {current && (
              <>
                <div style={{ ...sectionTitle, marginTop: 14 }}>Nodes</div>
                {Object.entries(palette).map(([cat, defs]) => (
                  <div key={cat} style={{ marginBottom: 10 }}>
                    <div style={{ textTransform: 'uppercase', fontSize: 10, color: catColor(cat), fontWeight: 700, marginBottom: 4 }}>{cat}</div>
                    {defs.map((d) => (
                      <button key={d.type} onClick={() => addNode(d)} style={button}>+ {d.label}</button>
                    ))}
                  </div>
                ))}
              </>
            )}
          </aside>

          <div style={{ flex: 1, minWidth: 0 }}>
            {current ? (
              <ReactFlow
                key={current.id}
                nodes={current.nodes}
                edges={current.edges}
                nodeTypes={nodeTypes}
                onNodesChange={onNodesChange}
                onEdgesChange={onEdgesChange}
                onConnect={onConnect}
                isValidConnection={isValidConnection}
                onNodeClick={(_, n) => selectNode(n.id)}
                onPaneClick={() => setSelectedNodeId(null)}
                fitView
              >
                <Background />
                <Controls />
                <MiniMap />
              </ReactFlow>
            ) : (
              <div style={{ padding: 24, color: '#888' }}>No graph yet. Create one with "+ New graph".</div>
            )}
          </div>

          <aside style={{ width: 300, borderLeft: '1px solid #ddd', overflowY: 'auto', padding: 12, fontSize: 12 }}>
            {selectedNode ? (
              <>
                <div style={{ fontWeight: 600 }}>{selectedDef?.label || selectedNode.data.type}</div>
                <div style={{ color: '#888', marginBottom: 10 }}>{selectedNode.data.type} · {selectedNode.id}</div>
                {(selectedDef?.fields || []).map((f) => (
                  <label key={f.id} style={{ display: 'block', marginBottom: 10 }}>
                    <div style={{ marginBottom: 3 }}>{f.label}</div>
                    {f.kind === 'npc' ? (
                      <select
                        value={selectedNode.data.config?.[f.id] ?? ''}
                        onChange={(e) => setConfig(f.id, e.target.value)}
                        style={{ width: '100%', padding: '4px 6px', boxSizing: 'border-box' }}
                      >
                        <option value="">(choose an NPC)</option>
                        {npcs.map((n) => <option key={n.id} value={n.id}>{n.name} ({n.id})</option>)}
                      </select>
                    ) : f.kind === 'quest' ? (
                      <select
                        value={selectedNode.data.config?.[f.id] ?? ''}
                        onChange={(e) => setConfig(f.id, e.target.value)}
                        style={input}
                      >
                        <option value="">(choose a quest)</option>
                        {quests.map((q) => <option key={q.id} value={q.id}>{q.title} ({q.id})</option>)}
                      </select>
                    ) : (
                      <input
                        value={selectedNode.data.config?.[f.id] ?? ''}
                        onChange={(e) => setConfig(f.id, e.target.value)}
                        style={{ width: '100%', padding: '4px 6px', boxSizing: 'border-box' }}
                      />
                    )}
                  </label>
                ))}
                <button onClick={deleteNode} style={{ padding: '5px 10px', cursor: 'pointer', color: '#c0392b' }}>Delete node</button>
              </>
            ) : selectedNpc ? (
              <>
                <div style={{ fontWeight: 600, marginBottom: 8 }}>NPC</div>
                <label style={{ display: 'block', marginBottom: 10 }}>
                  <div style={{ marginBottom: 3 }}>Name</div>
                  <input
                    value={selectedNpc.name}
                    onChange={(e) => setNpcField('name', e.target.value)}
                    style={{ width: '100%', padding: '4px 6px', boxSizing: 'border-box' }}
                  />
                </label>
                <div style={{ color: '#888', marginBottom: 12 }}>id: {selectedNpc.id} (fixed)</div>
                <label style={{ display: 'block', marginBottom: 10 }}>
                  <div style={{ marginBottom: 3 }}>Model <span style={{ color: '#888', fontSize: 10 }}>(empty = default look)</span></div>
                  <input
                    value={selectedNpc.model ?? ''}
                    placeholder="e.g. chief"
                    onChange={(e) => setNpcField('model', e.target.value)}
                    style={{ width: '100%', padding: '4px 6px', boxSizing: 'border-box' }}
                  />
                </label>
                <label style={{ display: 'block', marginBottom: 10 }}>
                  <div style={{ marginBottom: 3 }}>Idle animation <span style={{ color: '#888', fontSize: 10 }}>(loops)</span></div>
                  <input
                    value={selectedNpc.idle ?? ''}
                    placeholder="e.g. animation.chief.wave"
                    onChange={(e) => setNpcField('idle', e.target.value)}
                    style={{ width: '100%', padding: '4px 6px', boxSizing: 'border-box' }}
                  />
                </label>
                <div style={{ fontWeight: 600, marginBottom: 4 }}>Placed in the world</div>
                {(placements[selectedNpc.id] || []).length === 0 ? (
                  <div style={{ color: '#888', marginBottom: 12 }}>
                    Not placed yet. Publish, then in game: <code>/colophon npc spawn {selectedNpc.id}</code>
                  </div>
                ) : (
                  <ul style={{ margin: '0 0 12px', paddingLeft: 16, color: '#555' }}>
                    {placements[selectedNpc.id].map((p, i) => (
                      <li key={i}>{p.dim.replace('minecraft:', '')} {p.x}, {p.y}, {p.z}</li>
                    ))}
                  </ul>
                )}
                <button onClick={deleteNpc} style={{ padding: '5px 10px', cursor: 'pointer', color: '#c0392b' }}>Delete NPC</button>
              </>
            ) : selectedQuest ? (
              <>
                <div style={{ fontWeight: 600, marginBottom: 8 }}>Quest</div>
                <label style={{ display: 'block', marginBottom: 10 }}>
                  <div style={{ marginBottom: 3 }}>Title</div>
                  <input value={selectedQuest.title} onChange={(e) => setQuestField('title', e.target.value)} style={input} />
                </label>
                <div style={{ color: '#888', marginBottom: 12 }}>id: {selectedQuest.id} (fixed)</div>
                <label style={{ display: 'block', marginBottom: 10 }}>
                  <div style={{ marginBottom: 3 }}>Icon <span style={{ color: '#888', fontSize: 10 }}>(item id; empty = first need)</span></div>
                  <input
                    value={selectedQuest.icon ?? ''}
                    placeholder="e.g. minecraft:wheat"
                    onChange={(e) => setQuestField('icon', e.target.value)}
                    style={input}
                  />
                </label>
                <label style={{ display: 'block', marginBottom: 10 }}>
                  <div style={{ marginBottom: 3 }}>Text</div>
                  <textarea
                    value={selectedQuest.text ?? ''}
                    rows={4}
                    onChange={(e) => setQuestField('text', e.target.value)}
                    style={{ ...input, resize: 'vertical', fontFamily: 'inherit' }}
                  />
                </label>
                <div style={{ marginBottom: 3 }}>Needs <span style={{ color: '#888', fontSize: 10 }}>(handed in)</span></div>
                <StackList stacks={selectedQuest.goals} onChange={(v) => setQuestField('goals', v)} />
                <div style={{ marginBottom: 3 }}>Rewards <span style={{ color: '#888', fontSize: 10 }}>(item as /give writes it; [components] allowed)</span></div>
                <StackList wide stacks={selectedQuest.rewards} onChange={(v) => setQuestField('rewards', v)} />
                <button onClick={deleteQuest} style={{ padding: '5px 10px', cursor: 'pointer', color: '#c0392b' }}>Delete quest</button>
              </>
            ) : current ? (
              <>
                <div style={{ fontWeight: 600, marginBottom: 8 }}>Graph</div>
                <label style={{ display: 'block', marginBottom: 10 }}>
                  <div style={{ marginBottom: 3 }}>Name</div>
                  <input
                    value={current.name}
                    onChange={(e) => updateCurrent((g) => ({ ...g, name: e.target.value }))}
                    style={{ width: '100%', padding: '4px 6px', boxSizing: 'border-box' }}
                  />
                </label>
                <div style={{ color: '#888', marginBottom: 12 }}>id: {current.id} (fixed)</div>
                <button onClick={deleteGraph} style={{ padding: '5px 10px', cursor: 'pointer', color: '#c0392b' }}>Delete graph</button>
              </>
            ) : null}
          </aside>
        </div>
      </div>
    </SchemaContext.Provider>
  )
}
