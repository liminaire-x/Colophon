// Ids are made up here, never typed or edited: <kind>_<8 random a-z0-9> (Ids.java).
const ID_CHARS = 'abcdefghijklmnopqrstuvwxyz0123456789'

export function newId(kind, taken) {
  for (;;) {
    const bytes = crypto.getRandomValues(new Uint8Array(8))
    const id = `${kind}_${Array.from(bytes, (b) => ID_CHARS[b % ID_CHARS.length]).join('')}`
    if (!taken.includes(id)) return id
  }
}
