import { useCallback, useEffect, useMemo, useState } from 'react'
import { newId } from './ids.js'

const input = { width: '100%', padding: '4px 6px', boxSizing: 'border-box' }
const label = { display: 'block', marginBottom: 12 }
const hint = { color: '#888', fontSize: 11 }
const toolButton = { padding: '4px 8px', cursor: 'pointer', color: '#2563eb', border: '1px solid #ddd', borderRadius: 5, background: '#fafafa' }
const byText = (a, b) => a.localeCompare(b, 'ko')

// A quest's goals or rewards: rows of target + count. Goals pick hand in / kill.
// Rewards (`wide`) take a whole /give line such as minecraft:iron_sword[custom_name=...],
// so the item gets its own line.
function StackList({ stacks, onChange, fetchHeld, wide = false, goals = false }) {
  const set = (i, key, value) => onChange(stacks.map((s, j) => (j === i ? { ...s, [key]: value } : s)))
  return (
    <div style={{ marginBottom: 12 }}>
      {stacks.map((s, i) => {
        const count = (
          <input
            type="number" min={1} value={s.count}
            onChange={(e) => set(i, 'count', e.target.value === '' ? '' : Number(e.target.value))}
            style={{ ...input, width: 60 }}
          />
        )
        const remove = <button onClick={() => onChange(stacks.filter((_, j) => j !== i))} style={{ cursor: 'pointer' }}>×</button>
        // Fill this row's item with what the chosen player holds in game.
        const held = (key) => (
          <button
            title="Use the item the chosen player is holding"
            onClick={async () => { const spec = await fetchHeld(); if (spec) set(i, key, spec) }}
            style={{ cursor: 'pointer' }}
          >✋</button>
        )
        if (goals) {
          // A goal is { item, count } (hand in) or { kill, count } (kill while active).
          const kind = s.kill !== undefined ? 'kill' : 'item'
          const setKind = (k) => onChange(stacks.map((x, j) => (j === i ? { [k]: x[kind], count: x.count } : x)))
          return (
            <div key={i} style={{ display: 'flex', gap: 4, marginBottom: 3 }}>
              <select value={kind} onChange={(e) => setKind(e.target.value)} style={{ padding: '4px 2px' }}>
                <option value="item">hand in</option>
                <option value="kill">kill</option>
              </select>
              <input
                value={s[kind]} placeholder={kind === 'kill' ? 'minecraft:wolf' : 'minecraft:wheat'}
                onChange={(e) => set(i, kind, e.target.value)} style={{ ...input, flex: 1 }}
              />
              {kind === 'item' && held('item')}{count}{remove}
            </div>
          )
        }
        return wide ? (
          <div key={i} style={{ marginBottom: 6 }}>
            <textarea
              value={s.item} rows={2} placeholder="minecraft:iron_sword[custom_name='&quot;...&quot;']"
              onChange={(e) => set(i, 'item', e.target.value)}
              style={{ ...input, resize: 'vertical', fontFamily: 'monospace', fontSize: 11 }}
            />
            <div style={{ display: 'flex', gap: 4, justifyContent: 'flex-end' }}>{held('item')}{count}{remove}</div>
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

// The quest tab: a folder tree on the left, the chosen quest or folder on the right.
// Folders only group quests in the editor; the game doesn't see them (0008).
// A missing `folder` / `parent` means the top of the tree.
export default function QuestTab({ quests, setQuests, folders, setFolders, status, setMessage, hidden }) {
  const [selected, setSelected] = useState(null) // { kind: 'quest' | 'folder', id }
  const [collapsed, setCollapsed] = useState(() => new Set()) // folder ids
  const [players, setPlayers] = useState([]) // online, for "use held item"
  const [heldPlayer, setHeldPlayer] = useState(() => {
    try { return localStorage.getItem('lorebench.heldPlayer') || '' } catch (e) { return '' }
  })

  const quest = selected?.kind === 'quest' ? quests.find((q) => q.id === selected.id) || null : null
  const folder = selected?.kind === 'folder' ? folders.find((f) => f.id === selected.id) || null : null

  const byId = useMemo(() => Object.fromEntries(folders.map((f) => [f.id, f])), [folders])
  // "마을 / 촌장" for a folder, from the top down.
  const path = useCallback((id) => {
    const names = []
    for (let f = byId[id]; f && names.length <= folders.length; f = byId[f.parent]) names.unshift(f.name)
    return names.join(' / ')
  }, [byId, folders.length])
  // The folder and every folder inside it, which can't become its parent.
  const within = useCallback((id) => {
    const out = new Set([id])
    for (let grew = true; grew;) {
      grew = false
      folders.forEach((f) => { if (f.parent && out.has(f.parent) && !out.has(f.id)) { out.add(f.id); grew = true } })
    }
    return out
  }, [folders])

  // Where "+ Folder" and "+ Quest" put the new thing: the chosen folder, or the chosen quest's folder.
  const target = folder?.id ?? quest?.folder ?? ''

  const loadPlayers = useCallback(async () => {
    try {
      setPlayers((await fetch('/api/players').then((r) => r.json())).players || [])
    } catch (e) { /* shown as offline elsewhere */ }
  }, [])
  const questId = quest?.id
  useEffect(() => { if (questId) loadPlayers() }, [questId, loadPlayers])

  const pickHeldPlayer = useCallback((name) => {
    setHeldPlayer(name)
    try { localStorage.setItem('lorebench.heldPlayer', name) } catch (e) { /* not remembered */ }
  }, [])

  // What the chosen player holds, as /give writes it; null (with a message) if none.
  const fetchHeld = useCallback(async () => {
    if (!heldPlayer) { setMessage({ ok: false, text: 'Choose whose held item to use (above Needs).' }); return null }
    try {
      const r = await fetch('/api/held-item?player=' + encodeURIComponent(heldPlayer)).then((res) => res.json())
      if (r.error) { setMessage({ ok: false, text: r.error }); return null }
      return r.item
    } catch (e) {
      setMessage({ ok: false, text: 'Reading the held item failed: ' + e })
      return null
    }
  }, [heldPlayer, setMessage])

  const expand = (id) => setCollapsed((c) => { const n = new Set(c); n.delete(id); return n })
  const toggle = (id) => setCollapsed((c) => { const n = new Set(c); if (!n.delete(id)) n.add(id); return n })

  const newFolder = () => {
    const name = window.prompt('Folder name:')
    if (name == null || !name.trim()) return
    const id = newId('folder', folders.map((f) => f.id))
    setFolders((fs) => fs.concat(target ? { id, name: name.trim(), parent: target } : { id, name: name.trim() }))
    if (target) expand(target)
    setSelected({ kind: 'folder', id })
  }

  const newQuest = () => {
    const title = window.prompt('Quest title:')
    if (title == null || !title.trim()) return
    const id = newId('quest', quests.map((q) => q.id))
    const q = { id, title: title.trim(), goals: [], rewards: [] }
    setQuests((qs) => qs.concat(target ? { ...q, folder: target } : q))
    if (target) expand(target)
    setSelected({ kind: 'quest', id })
  }

  // Edit one field of the chosen quest; an emptied optional field is dropped.
  const setQuestField = (key, value) => {
    setQuests((qs) => qs.map((q) => {
      if (q.id !== quest.id) return q
      const next = { ...q, [key]: value }
      if ((key === 'icon' || key === 'text' || key === 'folder') && value.trim() === '') delete next[key]
      return next
    }))
  }

  const setFolderField = (key, value) => {
    setFolders((fs) => fs.map((f) => {
      if (f.id !== folder.id) return f
      const next = { ...f, [key]: value }
      if (key === 'parent' && value === '') delete next.parent
      return next
    }))
  }

  const deleteQuest = () => {
    if (!window.confirm(`Delete quest '${quest.title}'? Players keep their progress records.`)) return
    setQuests((qs) => qs.filter((q) => q.id !== quest.id))
    setSelected(null)
  }

  // What was inside moves up to the deleted folder's parent; nothing else is lost.
  const deleteFolder = () => {
    const up = folder.parent || ''
    const where = up ? `'${path(up)}'` : 'the top'
    if (!window.confirm(`Delete folder '${folder.name}'? What's inside moves up to ${where}.`)) return
    const move = (x, key) => {
      if (x[key] !== folder.id) return x
      const next = { ...x }
      if (up) next[key] = up; else delete next[key]
      return next
    }
    setFolders((fs) => fs.filter((f) => f.id !== folder.id).map((f) => move(f, 'parent')))
    setQuests((qs) => qs.map((q) => move(q, 'folder')))
    setSelected(up ? { kind: 'folder', id: up } : null)
  }

  const row = (depth, isSelected) => ({
    display: 'flex', alignItems: 'center', gap: 4, width: '100%', textAlign: 'left', cursor: 'pointer',
    padding: `3px 6px 3px ${6 + depth * 14}px`, border: 'none', borderRadius: 4, fontSize: 12,
    background: isSelected ? '#e0e7ff' : 'transparent',
  })

  const renderLevel = (parent, depth) => {
    const subFolders = folders.filter((f) => (f.parent || '') === parent).sort((a, b) => byText(a.name, b.name))
    const subQuests = quests.filter((q) => (q.folder || '') === parent).sort((a, b) => byText(a.title, b.title))
    return (
      <>
        {subFolders.map((f) => {
          const open = !collapsed.has(f.id)
          return (
            <div key={f.id}>
              <button
                onClick={() => setSelected({ kind: 'folder', id: f.id })}
                style={{ ...row(depth, folder?.id === f.id), fontWeight: 600 }}
              >
                <span
                  onClick={(e) => { e.stopPropagation(); toggle(f.id) }}
                  style={{ width: 12, color: '#666' }}
                  title={open ? 'Collapse' : 'Expand'}
                >{open ? '▾' : '▸'}</span>
                {f.name}
              </button>
              {open && renderLevel(f.id, depth + 1)}
            </div>
          )
        })}
        {subQuests.map((q) => (
          <button key={q.id} onClick={() => setSelected({ kind: 'quest', id: q.id })} style={row(depth, quest?.id === q.id)}>
            <span style={{ width: 12 }} />
            {q.title}
          </button>
        ))}
      </>
    )
  }

  const folderOptions = (exclude) => [
    <option key="" value="">(top)</option>,
    ...folders.filter((f) => !exclude.has(f.id))
      .map((f) => ({ id: f.id, label: path(f.id) }))
      .sort((a, b) => byText(a.label, b.label))
      .map((f) => <option key={f.id} value={f.id}>{f.label}</option>),
  ]

  return (
    <div style={{ flex: 1, minHeight: 0, display: hidden ? 'none' : 'flex' }}>
      <aside style={{ width: 260, borderRight: '1px solid #ddd', display: 'flex', flexDirection: 'column', fontSize: 12 }}>
        <div style={{ display: 'flex', gap: 6, padding: 10, borderBottom: '1px solid #eee' }}>
          <button onClick={newFolder} disabled={status !== 'ok'} style={toolButton}>+ Folder</button>
          <button onClick={newQuest} disabled={status !== 'ok'} style={toolButton}>+ Quest</button>
        </div>
        <div style={{ flex: 1, overflowY: 'auto', padding: 6 }}>
          {folders.length === 0 && quests.length === 0
            ? <div style={{ ...hint, padding: 6 }}>No quests yet. Create one with "+ Quest".</div>
            : renderLevel('', 0)}
        </div>
      </aside>

      <main style={{ flex: 1, minWidth: 0, overflowY: 'auto', padding: '16px 24px', fontSize: 13 }}>
        <div style={{ maxWidth: 680 }}>
          {quest ? (
            <>
              <div style={{ ...hint, marginBottom: 4 }}>{quest.folder ? path(quest.folder) : '(top)'}</div>
              <label style={label}>
                <div style={{ marginBottom: 3 }}>Title</div>
                <input value={quest.title} onChange={(e) => setQuestField('title', e.target.value)} style={{ ...input, fontSize: 15 }} />
              </label>
              <div style={{ ...hint, marginBottom: 12 }}>id: {quest.id} (fixed)</div>
              <label style={label}>
                <div style={{ marginBottom: 3 }}>Folder</div>
                <select value={quest.folder || ''} onChange={(e) => setQuestField('folder', e.target.value)} style={input}>
                  {folderOptions(new Set())}
                </select>
              </label>
              <label style={label}>
                <div style={{ marginBottom: 3 }}>Icon <span style={hint}>(item id; empty = first need)</span></div>
                <input
                  value={quest.icon ?? ''}
                  placeholder="e.g. minecraft:wheat"
                  onChange={(e) => setQuestField('icon', e.target.value)}
                  style={input}
                />
              </label>
              <label style={label}>
                <div style={{ marginBottom: 3 }}>Text</div>
                <textarea
                  value={quest.text ?? ''}
                  rows={6}
                  onChange={(e) => setQuestField('text', e.target.value)}
                  style={{ ...input, resize: 'vertical', fontFamily: 'inherit' }}
                />
              </label>
              <label style={{ display: 'flex', gap: 4, alignItems: 'center', marginBottom: 4 }}>
                <span>✋ held item of</span>
                <select value={heldPlayer} onChange={(e) => pickHeldPlayer(e.target.value)} style={{ flex: 1, padding: '3px 2px' }}>
                  <option value="">(player)</option>
                  {(players.includes(heldPlayer) || !heldPlayer ? players : [heldPlayer, ...players]).map((p) => (
                    <option key={p} value={p}>{p}{players.includes(p) ? '' : ' (offline)'}</option>
                  ))}
                </select>
                <button onClick={loadPlayers} title="Refresh online players" style={{ cursor: 'pointer' }}>↻</button>
              </label>
              <div style={{ ...hint, marginBottom: 10 }}>
                In a need, only the listed parts must match. Delete damage=… to accept any wear.
              </div>
              <div style={{ marginBottom: 3 }}>Needs <span style={hint}>(all of them, in this order)</span></div>
              <StackList goals fetchHeld={fetchHeld} stacks={quest.goals} onChange={(v) => setQuestField('goals', v)} />
              <div style={{ marginBottom: 3 }}>Rewards <span style={hint}>(item as /give writes it; [components] allowed)</span></div>
              <StackList wide fetchHeld={fetchHeld} stacks={quest.rewards} onChange={(v) => setQuestField('rewards', v)} />
              <button onClick={deleteQuest} style={{ padding: '5px 10px', cursor: 'pointer', color: '#c0392b' }}>Delete quest</button>
            </>
          ) : folder ? (
            <>
              <div style={{ ...hint, marginBottom: 4 }}>Folder</div>
              <label style={label}>
                <div style={{ marginBottom: 3 }}>Name</div>
                <input value={folder.name} onChange={(e) => setFolderField('name', e.target.value)} style={{ ...input, fontSize: 15 }} />
              </label>
              <label style={label}>
                <div style={{ marginBottom: 3 }}>Inside</div>
                <select value={folder.parent || ''} onChange={(e) => setFolderField('parent', e.target.value)} style={input}>
                  {folderOptions(within(folder.id))}
                </select>
              </label>
              <div style={{ ...hint, marginBottom: 12 }}>
                id: {folder.id} (fixed) · {folders.filter((f) => f.parent === folder.id).length} folders,{' '}
                {quests.filter((q) => q.folder === folder.id).length} quests directly inside.
                Folders only sort quests here in the editor; players don't see them.
              </div>
              <button onClick={deleteFolder} style={{ padding: '5px 10px', cursor: 'pointer', color: '#c0392b' }}>Delete folder</button>
            </>
          ) : (
            <div style={{ color: '#888' }}>Choose a quest or folder on the left, or create one.</div>
          )}
        </div>
      </main>
    </div>
  )
}
