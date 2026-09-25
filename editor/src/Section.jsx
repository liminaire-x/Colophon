// Parts shared by the quest and NPC tabs.

const input = { width: '100%', padding: '4px 6px', boxSizing: 'border-box' }

// One card per part of a quest or NPC. A new feature gets its own section.
export function Section({ title, children }) {
  return (
    <section style={{ border: '1px solid #e5e7eb', borderRadius: 8, padding: '10px 14px', marginBottom: 14 }}>
      <div style={{ fontWeight: 600, marginBottom: 8 }}>{title}</div>
      {children}
    </section>
  )
}

// What an NPC says: one page per line, shown in this order (DialogueLines.java).
// An empty list means nothing to say; the caller drops it from the document.
export function LineList({ lines = [], onChange, placeholder }) {
  const set = (i, text) => onChange(lines.map((l, j) => (j === i ? text : l)))
  return (
    <div>
      {lines.map((line, i) => (
        <div key={i} style={{ display: 'flex', gap: 4, marginBottom: 3, alignItems: 'flex-start' }}>
          <span style={{ color: '#888', fontSize: 11, width: 16, paddingTop: 5 }}>{i + 1}</span>
          <textarea
            value={line} rows={1} placeholder={placeholder}
            onChange={(e) => set(i, e.target.value)}
            style={{ ...input, flex: 1, resize: 'vertical', fontFamily: 'inherit' }}
          />
          <button onClick={() => onChange(lines.filter((_, j) => j !== i))} style={{ cursor: 'pointer' }}>×</button>
        </div>
      ))}
      <button onClick={() => onChange(lines.concat(''))} style={{ cursor: 'pointer', color: '#2563eb' }}>+ line</button>
    </div>
  )
}
