import { writable } from 'svelte/store'

export const toasts = writable([])

let nextId = 1

export function toast(message, kind = 'info') {
  const id = nextId++
  toasts.update((list) => [...list, { id, message, kind }])
  setTimeout(() => {
    toasts.update((list) => list.filter((t) => t.id !== id))
  }, 5000)
}

export function toastError(err) {
  toast(err?.message || String(err), 'error')
}

export function fmtTime(iso) {
  if (!iso) return '—'
  const d = new Date(iso)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}
