import { useCallback, useEffect, useState } from 'react'
import {
  ReactFlow,
  Background,
  Controls,
  MiniMap,
  addEdge,
  useNodesState,
  useEdgesState,
} from '@xyflow/react'
import '@xyflow/react/dist/style.css'

// v0 built-in nodes. data.nodeType + data.config are what the server reads on publish.
const initialNodes = [
  {
    id: 'trigger-1',
    type: 'input',
    position: { x: 120, y: 160 },
    data: { label: 'On Player Join', nodeType: 'on_player_join', config: {} },
  },
  {
    id: 'action-1',
    type: 'output',
    position: { x: 470, y: 160 },
    data: { label: 'Send Message', nodeType: 'send_message', config: { message: 'Welcome to the server!' } },
  },
]
const initialEdges = [{ id: 'e1', source: 'trigger-1', target: 'action-1' }]

export default function App() {
  const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes)
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges)
  const [status, setStatus] = useState('checking...')
  const [publishing, setPublishing] = useState(false)

  const onConnect = useCallback((p) => setEdges((eds) => addEdge(p, eds)), [setEdges])

  useEffect(() => {
    fetch('/api/health')
      .then((r) => r.json())
      .then((d) => setStatus(d.status ?? 'unknown'))
      .catch(() => setStatus('offline'))
  }, [])

  const messageNode = nodes.find((n) => n.data?.nodeType === 'send_message')
  const message = messageNode?.data?.config?.message ?? ''

  const setMessage = useCallback(
    (value) => {
      setNodes((nds) =>
        nds.map((n) =>
          n.data?.nodeType === 'send_message'
            ? { ...n, data: { ...n.data, config: { ...n.data.config, message: value } } }
            : n,
        ),
      )
    },
    [setNodes],
  )

  const publish = useCallback(async () => {
    setPublishing(true)
    try {
      const payload = {
        nodes: nodes.map((n) => ({
          id: n.id,
          data: { nodeType: n.data?.nodeType, config: n.data?.config ?? {} },
        })),
        edges: edges.map((e) => ({
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
      alert(data.accepted ? 'Published to server.' : 'Publish failed: ' + (data.error ?? 'unknown'))
    } catch (e) {
      alert('Publish error: ' + e)
    } finally {
      setPublishing(false)
    }
  }, [nodes, edges])

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <header style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '10px 16px', borderBottom: '1px solid #ddd', flexWrap: 'wrap' }}>
        <strong style={{ fontSize: 18 }}>Colophon</strong>
        <span style={{ fontSize: 13, color: status === 'ok' ? '#2a7d4f' : '#c0392b' }}>server: {status}</span>
        <label style={{ fontSize: 13, display: 'flex', alignItems: 'center', gap: 6 }}>
          Message:
          <input
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            style={{ padding: '4px 8px', width: 260 }}
          />
        </label>
        <span style={{ flex: 1 }} />
        <button onClick={publish} disabled={publishing} style={{ padding: '6px 14px', cursor: 'pointer' }}>
          {publishing ? 'Publishing...' : 'Publish'}
        </button>
      </header>
      <div style={{ flex: 1, minHeight: 0 }}>
        <ReactFlow
          nodes={nodes}
          edges={edges}
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
          onConnect={onConnect}
          fitView
        >
          <Background />
          <Controls />
          <MiniMap />
        </ReactFlow>
      </div>
    </div>
  )
}
