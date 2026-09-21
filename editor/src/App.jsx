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

// Data-type handle colors, filled from /api/schema `types` on boot (typeId -> hex).
const typeColors = {}
const dataColor = (typeId) => typeColors[typeId] || '#888'
const FLOW_IN = 'in' // matches GraphNode.FLOW_IN_PORT on the server

// An input is a wireable data port when connectable; otherwise an inline config knob.
// {name} tokens in a format_text template become string data inputs (mirrors
// FormatTextNode.instanceInputs on the server — instance-derived, not in the schema).
const tokenInputs = (nodeType, config) => {
  if (nodeType !== 'format_text') return []
  const tpl = (config && config.template) || ''
  const re = /\{([a-zA-Z_][a-zA-Z0-9_]*)\}/g // fresh regex: /g lastIndex is stateful
  const seen = new Set()
  const out = []
  let m
  while ((m = re.exec(tpl))) {
    if (!seen.has(m[1])) { seen.add(m[1]); out.push({ id: m[1], type: 'string', label: m[1], connectable: true }) }
  }
  return out
}
// Full inputs of a placed instance: static schema inputs + dynamic tokens.
const instanceInputs = (def, config) => (def?.inputs || []).concat(tokenInputs(def?.type, config))
// Connectable (data) inputs of an instance.
const instanceDataInputs = (def, config) => instanceInputs(def, config).filter((i) => i.connectable)

// player_info exposes only the fields picked in config.fields (comma list); the
// schema's dataOut is the full catalog. Mirrors PlayerInfoNode.instanceOutputs.
const selectedFields = (config) => new Set(((config && config.fields) || '')
  .split(',').map((s) => s.trim()).filter(Boolean))
const instanceDataOut = (nodeType, dataOut, config) => {
  const all = dataOut || []
  if (nodeType !== 'player_info') return all
  const sel = selectedFields(config)
  return all.filter((p) => sel.has(p.id))
}

// --- custom node: renders ports from schema (flow-in handle + one source handle per flowOut port) ---
// Blueprint-style handle shapes: exec (flow) pins are right-pointing triangles at
// the top; data pins are type-colored circles below, inputs left / outputs right.
const execHandleStyle = {
  width: 12,
  height: 12,
  background: '#e5e7eb',
  border: '1px solid #4b5563',
  borderRadius: 2,
  clipPath: 'polygon(0 0, 100% 50%, 0 100%)',
}
const dataHandleStyle = (typeId) => ({
  width: 11,
  height: 11,
  background: dataColor(typeId),
  border: '2px solid #fff',
  borderRadius: '50%',
})

function ColophonNode({ data, selected }) {
  const flowOut = data.flowOut && data.flowOut.length ? data.flowOut : []
  const dataIn = (data.inputs || []).filter((i) => i.connectable)
    .concat(tokenInputs(data.nodeType, data.config))
  const dataOut = instanceDataOut(data.nodeType, data.dataOut, data.config)
  const cfg = data.config || {}
  const cfgEntries = Object.entries(cfg)

  const execRows = Math.max(data.hasFlowIn ? 1 : 0, flowOut.length)
  const dataRows = Math.max(dataIn.length, dataOut.length)

  return (
    <div
      style={{
        border: `2px solid ${selected ? '#111' : '#c9c9c9'}`,
        borderRadius: 8,
        background: '#fff',
        minWidth: 184,
        fontSize: 12,
        boxShadow: '0 1px 3px rgba(0,0,0,0.12)',
      }}
    >
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

      {/* exec (flow) pins — in on the left, out(s) on the right */}
      {execRows > 0 && (
        <div style={{ padding: '2px 0', borderBottom: dataRows > 0 || cfgEntries.length > 0 ? '1px solid #eee' : 'none' }}>
          {Array.from({ length: execRows }).map((_, i) => {
            const showIn = i === 0 && data.hasFlowIn
            const out = flowOut[i]
            return (
              <div
                key={`exec-${i}`}
                style={{ position: 'relative', display: 'flex', justifyContent: 'space-between', alignItems: 'center', minHeight: 20, padding: '2px 12px' }}
              >
                {showIn && <Handle type="target" id={FLOW_IN} position={Position.Left} style={execHandleStyle} />}
                <span style={{ fontSize: 10, color: '#374151', fontWeight: 600 }} />
                <span style={{ fontSize: 10, color: '#374151', fontWeight: 600 }}>
                  {out && flowOut.length > 1 ? out : ''}
                </span>
                {out && <Handle type="source" id={out} position={Position.Right} style={execHandleStyle} />}
              </div>
            )
          })}
        </div>
      )}

      {/* data pins — inputs on the left, outputs on the right (paired rows) */}
      {dataRows > 0 && (
        <div style={{ padding: '2px 0', borderBottom: cfgEntries.length > 0 ? '1px solid #eee' : 'none' }}>
          {Array.from({ length: dataRows }).map((_, i) => {
            const inp = dataIn[i]
            const out = dataOut[i]
            return (
              <div
                key={`data-${i}`}
                style={{ position: 'relative', display: 'flex', justifyContent: 'space-between', alignItems: 'center', minHeight: 20, padding: '2px 12px', gap: 14 }}
              >
                {inp && <Handle type="target" id={inp.id} position={Position.Left} style={dataHandleStyle(inp.type)} />}
                <span style={{ fontSize: 10, color: '#555' }}>{inp ? inp.label || inp.id : ''}</span>
                <span style={{ fontSize: 10, color: '#555' }}>{out ? out.label || out.id : ''}</span>
                {out && <Handle type="source" id={out.id} position={Position.Right} style={dataHandleStyle(out.type)} />}
              </div>
            )
          })}
        </div>
      )}

      {cfgEntries.length > 0 && (
        <div style={{ padding: '4px 10px', color: '#666' }}>
          {cfgEntries.map(([k, v]) => (
            <div key={k} style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: 200 }}>
              {k}: {String(v)}
            </div>
          ))}
        </div>
      )}
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
        ;(s.types || []).forEach((t) => { typeColors[t.id] = t.color })
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
              inputs: def.inputs,
              dataOut: def.dataOut,
            },
          }
        })
        setNodes(loadedNodes)
        setEdges((g.edges || []).map((e, i) => ({
          id: e.id || `e${i}`,
          source: e.source,
          target: e.target,
          sourceHandle: e.sourceHandle ?? null,
          targetHandle: e.targetHandle ?? null,
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

    // Classify the source handle: a flow output (default "out") or a data output.
    const flowPort = c.sourceHandle || 'out'
    const isFlowSrc = (srcDef.flowOut || []).includes(flowPort)
    const srcData = instanceDataOut(srcNode.data.nodeType, srcDef.dataOut, srcNode.data.config)
      .find((p) => p.id === c.sourceHandle)

    if (isFlowSrc) {
      // Flow edge: target must accept a flow input and must not be a data input.
      if (c.targetHandle && c.targetHandle !== FLOW_IN
          && instanceDataInputs(tgtDef, tgtNode.data.config).some((p) => p.id === c.targetHandle)) return false
      return !!tgtDef.hasFlowIn
    }
    if (srcData) {
      // Data edge: target must be a data input of the SAME type, and unconnected.
      if (!c.targetHandle || c.targetHandle === FLOW_IN) return false
      const tgtData = instanceDataInputs(tgtDef, tgtNode.data.config).find((p) => p.id === c.targetHandle)
      if (!tgtData) return false
      if (tgtData.type !== srcData.type) return false
      const already = edgesRef.current.some((e) => e.target === c.target && e.targetHandle === c.targetHandle)
      if (already) return false
      return true
    }
    return false
  }, [])

  // keep refs of nodes/edges for isValidConnection (avoids stale closure)
  const nodesRef = useRef([])
  useEffect(() => { nodesRef.current = nodes }, [nodes])
  const edgesRef = useRef([])
  useEffect(() => { edgesRef.current = edges }, [edges])

  const onConnect = useCallback((params) => {
    setEdges((eds) => addEdge({ ...params }, eds))
  }, [setEdges])

  const addNode = useCallback((def) => {
    const config = {}
    // Seed every input's inline default (connectable inputs fall back to it when unwired).
    ;(def.inputs || []).forEach((i) => { config[i.id] = i.default ?? '' })
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
        inputs: def.inputs,
        dataOut: def.dataOut,
      },
    }
    setNodes((ns) => ns.concat(node))
    setSelectedId(id)
  }, [setNodes])

  const selectedNode = nodes.find((n) => n.id === selectedId) || null
  const selectedDef = selectedNode ? byType[selectedNode.data.nodeType] : null
  // Data input ports of the selected node that are currently wired (so they take a
  // value from the wire, not an inline field).
  const connectedInputs = useMemo(
    () => new Set(edges.filter((e) => e.target === selectedId && e.targetHandle).map((e) => e.targetHandle)),
    [edges, selectedId])

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
          targetHandle: e.targetHandle ?? null,
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
              {instanceInputs(selectedDef, selectedNode.data.config)
                .filter((f) => !(selectedNode.data.nodeType === 'player_info' && f.id === 'fields'))
                .map((f) => {
                const wired = f.connectable && connectedInputs.has(f.id)
                return (
                  <label key={f.id} style={{ display: 'block', marginBottom: 10 }}>
                    <div style={{ marginBottom: 3 }}>
                      {f.label || f.id}
                      {f.connectable && <span style={{ color: '#888', fontSize: 10 }}> · {wired ? 'wired' : 'input'}</span>}
                    </div>
                    {wired ? (
                      <div style={{ color: '#0d9488', fontStyle: 'italic', padding: '4px 0' }}>connected (from wire)</div>
                    ) : (f.options || []).length > 0 ? (
                      <select
                        value={selectedNode.data.config?.[f.id] ?? (f.default ?? '')}
                        onChange={(e) => setConfigField(f.id, e.target.value)}
                        style={{ width: '100%', padding: '4px 6px', boxSizing: 'border-box' }}
                      >
                        {(f.options || []).map((opt) => (
                          <option key={opt} value={opt}>{opt}</option>
                        ))}
                      </select>
                    ) : (
                      <input
                        type={f.type === 'number' ? 'number' : 'text'}
                        value={selectedNode.data.config?.[f.id] ?? ''}
                        onChange={(e) => setConfigField(f.id, e.target.value)}
                        style={{ width: '100%', padding: '4px 6px', boxSizing: 'border-box' }}
                      />
                    )}
                  </label>
                )
              })}
              {selectedNode.data.nodeType === 'player_info' && (
                <div style={{ marginBottom: 12 }}>
                  <div style={{ marginBottom: 4, fontWeight: 600 }}>Outputs</div>
                  {(selectedDef?.dataOut || []).map((p) => {
                    const sel = selectedFields(selectedNode.data.config)
                    const checked = sel.has(p.id)
                    return (
                      <label key={p.id} style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 3 }}>
                        <input
                          type="checkbox"
                          checked={checked}
                          onChange={() => {
                            const next = new Set(sel)
                            if (checked) next.delete(p.id); else next.add(p.id)
                            // preserve catalog order
                            const list = (selectedDef?.dataOut || []).filter((q) => next.has(q.id)).map((q) => q.id)
                            setConfigField('fields', list.join(','))
                          }}
                        />
                        <span>{p.label || p.id}</span>
                        <span style={{ color: '#888', fontSize: 10 }}>· {p.type}</span>
                      </label>
                    )
                  })}
                </div>
              )}
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
