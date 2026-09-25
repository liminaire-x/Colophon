import { useState } from 'react'
import { newId } from './ids.js'

const input = { width: '100%', padding: '4px 6px', boxSizing: 'border-box' }
const label = { display: 'block', marginBottom: 10 }
const hint = { color: '#888', fontSize: 11 }
const toolButton = { padding: '4px 8px', cursor: 'pointer', color: '#2563eb', border: '1px solid #ddd', borderRadius: 5, background: '#fafafa' }

// One card per part of an NPC. A new NPC feature gets its own section below the others.
function Section({ title, children }) {
  return (
    <section style={{ border: '1px solid #e5e7eb', borderRadius: 8, padding: '10px 14px', marginBottom: 14 }}>
      <div style={{ fontWeight: 600, marginBottom: 8 }}>{title}</div>
      {children}
    </section>
  )
}

// The NPC tab: the NPC list on the left, the chosen NPC's sections on the right.
export default function NpcTab({ npcs, setNpcs, placements, status, hidden }) {
  const [selectedId, setSelectedId] = useState(null)
  const npc = npcs.find((n) => n.id === selectedId) || null
  const placed = npc ? placements[npc.id] || [] : []

  const newNpc = () => {
    const name = window.prompt('NPC name (shown above the NPC in game):')
    if (name == null || !name.trim()) return
    const id = newId('npc', npcs.map((n) => n.id))
    setNpcs((ns) => ns.concat({ id, name: name.trim() }))
    setSelectedId(id)
  }

  // Edit one field of the chosen NPC; an emptied optional field is dropped.
  const setField = (key, value) => {
    setNpcs((ns) => ns.map((n) => {
      if (n.id !== npc.id) return n
      const next = { ...n, [key]: value }
      if (key !== 'name' && value.trim() === '') delete next[key]
      return next
    }))
  }

  const deleteNpc = () => {
    const warning = placed.length ? `\n${placed.length} placed in the world will disappear on publish.` : ''
    if (!window.confirm(`Delete NPC '${npc.name}'?${warning}`)) return
    setNpcs((ns) => ns.filter((n) => n.id !== npc.id))
    setSelectedId(null)
  }

  const sorted = [...npcs].sort((a, b) => a.name.localeCompare(b.name, 'ko'))

  return (
    <div style={{ flex: 1, minHeight: 0, display: hidden ? 'none' : 'flex' }}>
      <aside style={{ width: 260, borderRight: '1px solid #ddd', display: 'flex', flexDirection: 'column', fontSize: 12 }}>
        <div style={{ padding: 10, borderBottom: '1px solid #eee' }}>
          <button onClick={newNpc} disabled={status !== 'ok'} style={toolButton}>+ NPC</button>
        </div>
        <div style={{ flex: 1, overflowY: 'auto', padding: 6 }}>
          {npcs.length === 0 && <div style={{ ...hint, padding: 6 }}>No NPCs yet. Create one with "+ NPC".</div>}
          {sorted.map((n) => (
            <button
              key={n.id}
              onClick={() => setSelectedId(n.id)}
              style={{
                display: 'flex', justifyContent: 'space-between', width: '100%', textAlign: 'left', cursor: 'pointer',
                padding: '3px 6px', border: 'none', borderRadius: 4, fontSize: 12,
                background: n.id === selectedId ? '#e0e7ff' : 'transparent',
              }}
            >
              <span>{n.name}</span>
              <span style={hint}>{(placements[n.id] || []).length} placed</span>
            </button>
          ))}
        </div>
      </aside>

      <main style={{ flex: 1, minWidth: 0, overflowY: 'auto', padding: '16px 24px', fontSize: 13 }}>
        <div style={{ maxWidth: 680 }}>
          {npc ? (
            <>
              <label style={label}>
                <div style={{ marginBottom: 3 }}>Name <span style={hint}>(shown above the NPC in game)</span></div>
                <input value={npc.name} onChange={(e) => setField('name', e.target.value)} style={{ ...input, fontSize: 15 }} />
              </label>
              <div style={{ ...hint, marginBottom: 14 }}>id: {npc.id} (fixed)</div>

              <Section title="Look">
                <label style={label}>
                  <div style={{ marginBottom: 3 }}>Model <span style={hint}>(empty = default look)</span></div>
                  <input value={npc.model ?? ''} placeholder="e.g. chief" onChange={(e) => setField('model', e.target.value)} style={input} />
                </label>
                <label style={{ ...label, marginBottom: 0 }}>
                  <div style={{ marginBottom: 3 }}>Idle animation <span style={hint}>(loops)</span></div>
                  <input value={npc.idle ?? ''} placeholder="e.g. animation.chief.wave" onChange={(e) => setField('idle', e.target.value)} style={input} />
                </label>
              </Section>

              <Section title="Placed in the world">
                {placed.length === 0 ? (
                  <div style={{ color: '#888' }}>Not placed yet.</div>
                ) : (
                  <ul style={{ margin: 0, paddingLeft: 16, color: '#555' }}>
                    {placed.map((p, i) => <li key={i}>{p.dim.replace('minecraft:', '')} {p.x}, {p.y}, {p.z}</li>)}
                  </ul>
                )}
                <div style={{ ...hint, marginTop: 8 }}>
                  To place one, publish, then in game: <code>/lorebench npc spawn {npc.id}</code>
                </div>
              </Section>

              <button onClick={deleteNpc} style={{ padding: '5px 10px', cursor: 'pointer', color: '#c0392b' }}>Delete NPC</button>
            </>
          ) : (
            <div style={{ color: '#888' }}>Choose an NPC on the left, or create one.</div>
          )}
        </div>
      </main>
    </div>
  )
}
