import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  ReactFlow,
  Background,
  Controls,
  MiniMap,
  Handle,
  Position,
  addEdge,
  useNodesState,
  useEdgesState,
} from '@xyflow/react'
import '@xyflow/react/dist/style.css'

const CATEGORY_COLORS = {
  trigger: '#2a7d4f',
  action: '#2563eb',
  flow: '#7c3aed',
  economy: '#b45309',
}
const catColor = (c) => CATEGORY_COLORS[c] || '#555'

// --- custom node: renders ports from schema (flow-in handle + one source handle per flowOut port) ---
function ColophonNode({ data, selected }) {
  const ports = data.flowOut && data.flowOut.length ? data.flowOut : []
  const cfg = data.config || {}
  const cfgEntries = Object.entries(cfg)
  return (
    <div
      style={{
        border: `2px solid ${selected ? '#111' : '#c9c9c9'}`,
        borderRadius: 8,
        background: '#fff',
        minWidth: 168,
        fontSize: 12,
        boxShadow: '0 1px 3px rgba(0,0,0,0.12)',
      }}
    >
      {data.hasFlowIn && <Handle type="target" position={Position.Left} id="in" />}
      <div
        style={{
          padding: '5px 10px',
          background: catColor(data.category),
          color: '#fff',
          borderRadius: '6px 6px 0 0',
          fontWeight: 600,
        }}
      >
        {data.label || data.nodeType}
      </div>
      {cfgEntries.length > 0 && (
        <div style={{ padding: '4px 10px', color: '#666', borderBottom: '1px solid #eee' }}>
          {cfgEntries.map(([k, v]) => (
            <div key={k} style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: 200 }}>
              {k}: {String(v)}
            </div>
          ))}
        </div>
      )}
      <div style={{ padding: '2px 0' }}>
        {ports.map((p) => (
          <div
            key={p}
            style={{ position: 'relative', display: 'flex', justifyContent: 'flex-end', alignItems: 'center', padding: '3px 14px 3px 8px', minHeight: 18 }}
          >
            <span style={{ fontSize: 10, color: '#888' }}>{ports.length > 1 ? p : ''}</span>
            <Handle type="source" id={p} position={Position.Right} />
          </div>
        ))}
        {ports.length === 0 && <div style={{ height: 6 }} />}
      </div>
    </div>
  )
}

let idSeq = 1
const nextId = (type) => `${type}_${idSeq++}`

export default function App() {
  const [nodes, setNodes, onNodesChange] = useNodesState([])
  const [edges, setEdges, onEdgesChange] = useEdgesState([])
  const [schema, setSchema] = useState([]) // [{type,label,category,hasFlowIn,flowOut,fields}]
  const [status, setStatus] = useState('checking...')
  const [selectedId, setSelectedId] = useState(null)
  const [publishing, setPublishing] = useState(false)
  const schemaRef = useRef([])

  const nodeTypes = useMemo(() => ({ colophon: ColophonNode }), [])
  const byType = useMemo(() => Object.fromEntries(schema.map((s) => [s.type, s])), [schema])

  // Load schema + current graph on mount.
  useEffect(() => {
    let cancelled = false
    async function boot() {
      try {
        const s = await fetch('/api/schema').then((r) => r.json())
        const defs = s.nodes || []
        if (cancelled) return
        schemaRef.current = defs
        setSchema(defs)
        setStatus('ok')

        const g = await fetch('/api/graph').then((r) => r.json())
        if (cancelled) return
        const defsByType = Object.fromEntries(defs.map((d) => [d.type, d]))
        const loadedNodes = (g.nodes || []).map((n) => {
          const def = defsByType[n.data?.nodeType] || {}
          return {
            id: n.id,
            type: 'colophon',
            position: n.position || { x: 100, y: 100 },
            data: {
              nodeType: n.data?.nodeType,
              config: n.data?.config || {},
              label: def.label,
              category: def.category,
              hasFlowIn: def.hasFlowIn,
              flowOut: def.flowOut,
            },
          }
        })
        setNodes(loadedNodes)
        setEdges((g.edges || []).map((e, i) => ({
          id: e.id || `e${i}`,
          source: e.source,
          target: e.target,
          sourceHandle: e.sourceHandle ?? null,
        })))
        // keep id sequence ahead of loaded ids
        loadedNodes.forEach((n) => {
          const m = /_(\d+)$/.exec(n.id)
          if (m) idSeq = Math.max(idSeq, Number(m[1]) + 1)
        })
      } catch (e) {
        if (!cancelled) setStatus('offline')
      }
    }
    boot()
    return () => { cancelled = true }
  }, [setNodes, setEdges])

  const isValidConnection = useCallback((c) => {
    const s = schemaRef.current
    // look up node defs by current nodes
    const srcNode = nodesRef.current.find((n) => n.id === c.source)
    const tgtNode = nodesRef.current.find((n) => n.id === c.target)
    if (!srcNode || !tgtNode) return false
    const srcDef = s.find((d) => d.type === srcNode.data.nodeType)
    const tgtDef = s.find((d) => d.type === tgtNode.data.nodeType)
    if (!srcDef || !tgtDef) return false
    if (!tgtDef.hasFlowIn) return false
    const port = c.sourceHandle || 'out'
    if (!(srcDef.flowOut || []).includes(port)) return false
    return true
  }, [])

  // keep a ref of nodes for isValidConnection (avoids stale closure)
  const nodesRef = useRef([])
  useEffect(() => { nodesRef.current = nodes }, [nodes])

  const onConnect = useCallback((params) => {
    setEdges((eds) => addEdge({ ...params }, eds))
  }, [setEdges])

  const addNode = useCallback((def) => {
    const config = {}
    ;(def.fields || []).forEach((f) => { config[f.name] = f.default ?? '' })
    const id = nextId(def.type)
    const node = {
      id,
      type: 'colophon',
      position: { x: 200 + Math.random() * 120, y: 120 + Math.random() * 160 },
      data: {
        nodeType: def.type,
        config,
        label: def.label,
        category: def.category,
        hasFlowIn: def.hasFlowIn,
        flowOut: def.flowOut,
      },
    }
    setNodes((ns) => ns.concat(node))
    setSelectedId(id)
  }, [setNodes])

  const selectedNode = nodes.find((n) => n.id === selectedId) || null
  const selectedDef = selectedNode ? byType[selectedNode.data.nodeType] : null

  const setConfigField = useCallback((name, value) => {
    setNodes((ns) => ns.map((n) =>
      n.id === selectedId
        ? { ...n, data: { ...n.data, config: { ...n.data.config, [name]: value } } }
        : n))
  }, [selectedId, setNodes])

  const deleteSelected = useCallback(() => {
    if (!selectedId) return
    setNodes((ns) => ns.filter((n) => n.id !== selectedId))
    setEdges((es) => es.filter((e) => e.source !== selectedId && e.target !== selectedId))
    setSelectedId(null)
  }, [selectedId, setNodes, setEdges])

  const publish = useCallback(async () => {
    setPublishing(true)
    try {
      const payload = {
        nodes: nodes.map((n) => ({
          id: n.id,
          type: n.type,
          position: n.position,
          data: { nodeType: n.data.nodeType, config: n.data.config || {} },
        })),
        edges: edges.map((e) => ({
          id: e.id,
          source: e.source,
          target: e.target,
          sourceHandle: e.sourceHandle ?? null,
        })),
      }
      const res = await fetch('/api/publish', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      })
      const data = await res.json()
      if (data.accepted) alert('Published to server.')
      else alert('Publish rejected:\n- ' + (data.errors || ['unknown']).join('\n- '))
    } catch (e) {
      alert('Publish error: ' + e)
    } finally {
      setPublishing(false)
    }
  }, [nodes, edges])

  const grouped = useMemo(() => {
    const g = {}
    schema.forEach((d) => { (g[d.category] = g[d.category] || []).push(d) })
    return g
  }, [schema])

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <header style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '10px 16px', borderBottom: '1px solid #ddd' }}>
        <strong style={{ fontSize: 18 }}>Colophon</strong>
        <span style={{ fontSize: 13, color: status === 'ok' ? '#2a7d4f' : '#c0392b' }}>server: {status}</span>
        <span style={{ flex: 1 }} />
        <button onClick={publish} disabled={publishing} style={{ padding: '6px 14px', cursor: 'pointer' }}>
          {publishing ? 'Publishing...' : 'Publish'}
        </button>
      </header>

      <div style={{ flex: 1, minHeight: 0, display: 'flex' }}>
        {/* palette */}
        <aside style={{ width: 190, borderRight: '1px solid #ddd', overflowY: 'auto', padding: 10, fontSize: 12 }}>
          <div style={{ fontWeight: 600, marginBottom: 6 }}>Nodes</div>
          {Object.keys(grouped).length === 0 && <div style={{ color: '#999' }}>no schema</div>}
          {Object.entries(grouped).map(([cat, defs]) => (
            <div key={cat} style={{ marginBottom: 10 }}>
              <div style={{ textTransform: 'uppercase', fontSize: 10, color: catColor(cat), fontWeight: 700, marginBottom: 4 }}>{cat}</div>
              {defs.map((d) => (
                <button
                  key={d.type}
                  onClick={() => addNode(d)}
                  style={{ display: 'block', width: '100%', textAlign: 'left', padding: '5px 8px', marginBottom: 3, cursor: 'pointer', border: '1px solid #ddd', borderRadius: 5, background: '#fafafa' }}
                >
                  + {d.label || d.type}
                </button>
              ))}
            </div>
          ))}
        </aside>

        {/* canvas */}
        <div style={{ flex: 1, minWidth: 0 }}>
          <ReactFlow
            nodes={nodes}
            edges={edges}
            nodeTypes={nodeTypes}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            onConnect={onConnect}
            isValidConnection={isValidConnection}
            onNodeClick={(_, n) => setSelectedId(n.id)}
            onPaneClick={() => setSelectedId(null)}
            fitView
          >
            <Background />
            <Controls />
            <MiniMap />
          </ReactFlow>
        </div>

        {/* config panel */}
        <aside style={{ width: 240, borderLeft: '1px solid #ddd', overflowY: 'auto', padding: 12, fontSize: 12 }}>
          {!selectedNode && <div style={{ color: '#999' }}>Select a node to edit.</div>}
          {selectedNode && (
            <>
              <div style={{ fontWeight: 600, marginBottom: 2 }}>{selectedDef?.label || selectedNode.data.nodeType}</div>
              <div style={{ color: '#888', marginBottom: 10 }}>{selectedNode.data.nodeType}</div>
              {(selectedDef?.fields || []).length === 0 && <div style={{ color: '#999', marginBottom: 10 }}>No config.</div>}
              {(selectedDef?.fields || []).map((f) => (
                <label key={f.name} style={{ display: 'block', marginBottom: 10 }}>
                  <div style={{ marginBottom: 3 }}>{f.name}</div>
                  <input
                    type={f.type === 'number' ? 'number' : 'text'}
                    value={selectedNode.data.config?.[f.name] ?? ''}
                    onChange={(e) => setConfigField(f.name, e.target.value)}
                    style={{ width: '100%', padding: '4px 6px', boxSizing: 'border-box' }}
                  />
                </label>
              ))}
              <button onClick={deleteSelected} style={{ padding: '5px 10px', cursor: 'pointer', color: '#c0392b' }}>
                Delete node
              </button>
            </>
          )}
        </aside>
      </div>
    </div>
  )
}
