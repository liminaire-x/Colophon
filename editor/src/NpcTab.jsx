import { useState } from 'react'
import { newId } from './ids.js'
import { FolderPanel, FolderSelect, FolderTree, addFolder, folderPath, placeIn } from './FolderTree.jsx'

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

// The NPC tab: a folder tree of NPCs on the left, the chosen NPC's sections (or folder) on the right.
export default function NpcTab({ npcs, setNpcs, folders, setFolders, placements, status, hidden }) {
  const [selected, setSelected] = useState(null) // { kind: 'item' | 'folder', id }
  const [collapsed, setCollapsed] = useState(() => new Set()) // folder ids
  const npc = selected?.kind === 'item' ? npcs.find((n) => n.id === selected.id) || null : null
  const folder = selected?.kind === 'folder' ? folders.find((f) => f.id === selected.id) || null : null
  const placed = npc ? placements[npc.id] || [] : []

  // Where "+ Folder" and "+ NPC" put the new thing: the chosen folder, or the chosen NPC's folder.
  const target = folder?.id ?? npc?.folder ?? ''

  const newFolder = () => {
    const id = addFolder(folders, setFolders, target)
    if (id) setSelected({ kind: 'folder', id })
  }

  const newNpc = () => {
    const name = window.prompt('NPC name (shown above the NPC in game):')
    if (name == null || !name.trim()) return
    const id = newId('npc', npcs.map((n) => n.id))
    setNpcs((ns) => ns.concat(placeIn({ id, name: name.trim() }, target)))
    setSelected({ kind: 'item', id })
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
    setSelected(null)
  }

  return (
    <div style={{ flex: 1, minHeight: 0, display: hidden ? 'none' : 'flex' }}>
      <aside style={{ width: 260, borderRight: '1px solid #ddd', display: 'flex', flexDirection: 'column', fontSize: 12 }}>
        <div style={{ display: 'flex', gap: 6, padding: 10, borderBottom: '1px solid #eee' }}>
          <button onClick={newFolder} disabled={status !== 'ok'} style={toolButton}>+ Folder</button>
          <button onClick={newNpc} disabled={status !== 'ok'} style={toolButton}>+ NPC</button>
        </div>
        <div style={{ flex: 1, overflowY: 'auto', padding: 6 }}>
          {folders.length === 0 && npcs.length === 0
            ? <div style={{ ...hint, padding: 6 }}>No NPCs yet. Create one with "+ NPC".</div>
            : (
              <FolderTree
                folders={folders} selected={selected} onSelect={setSelected}
                collapsed={collapsed} setCollapsed={setCollapsed}
                items={npcs.map((n) => ({ id: n.id, label: n.name, folder: n.folder, note: `${(placements[n.id] || []).length} placed` }))}
              />
            )}
        </div>
      </aside>

      <main style={{ flex: 1, minWidth: 0, overflowY: 'auto', padding: '16px 24px', fontSize: 13 }}>
        <div style={{ maxWidth: 680 }}>
          {npc ? (
            <>
              <div style={{ ...hint, marginBottom: 4 }}>{npc.folder ? folderPath(folders, npc.folder) : '(top)'}</div>
              <label style={label}>
                <div style={{ marginBottom: 3 }}>Name <span style={hint}>(shown above the NPC in game)</span></div>
                <input value={npc.name} onChange={(e) => setField('name', e.target.value)} style={{ ...input, fontSize: 15 }} />
              </label>
              <div style={{ ...hint, marginBottom: 12 }}>id: {npc.id} (fixed)</div>
              <label style={{ ...label, marginBottom: 14 }}>
                <div style={{ marginBottom: 3 }}>Folder</div>
                <FolderSelect folders={folders} value={npc.folder} onChange={(v) => setField('folder', v)} />
              </label>

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
          ) : folder ? (
            <FolderPanel
              folder={folder} folders={folders} setFolders={setFolders} items={npcs} setItems={setNpcs}
              onDeleted={(up) => setSelected(up ? { kind: 'folder', id: up } : null)}
            />
          ) : (
            <div style={{ color: '#888' }}>Choose an NPC or folder on the left, or create one.</div>
          )}
        </div>
      </main>
    </div>
  )
}
