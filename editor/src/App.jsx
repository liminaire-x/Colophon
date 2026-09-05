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

const initialNodes = [
  { id: 'trigger-1', type: 'input', position: { x: 120, y: 140 }, data: { label: 'On Player Join' } },
  { id: 'action-1', type: 'output', position: { x: 440, y: 140 }, data: { label: 'Send Message' } },
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

  const publish = useCallback(async () => {
    setPublishing(true)
    try {
      const res = await fetch('/api/publish', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ nodes, edges }),
      })
      const data = await res.json()
      alert(data.accepted ? 'Published to server.' : 'Publish rejected.')
    } catch (e) {
      alert('Publish error: ' + e)
    } finally {
      setPublishing(false)
    }
  }, [nodes, edges])

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
