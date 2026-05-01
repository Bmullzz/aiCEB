async function request(path, options = {}) {
  const url = `${import.meta.env.VITE_API_BASE_URL}${path}`
  const response = await fetch(url, {
    headers: { 'Content-Type': 'application/json', ...options.headers },
    ...options,
  })
  if (!response.ok) {
    let errorBody
    try { errorBody = await response.json() }
    catch { errorBody = { status: response.status, code: 'UNKNOWN', message: response.statusText } }
    return Promise.reject({
      status: errorBody.status ?? response.status,
      code: errorBody.code ?? 'UNKNOWN',
      message: errorBody.message ?? 'An unexpected error occurred.',
    })
  }
  if (response.status === 204) return null
  return response.json()
}

function normalizePhoneNumber(raw) {
  const digits = raw.replace(/\D/g, '')
  if (digits.startsWith('1') && digits.length === 11) return `+${digits}`
  if (digits.length === 10) return `+1${digits}`
  if (raw.startsWith('+')) return raw
  return `+${digits}`
}

export function getEvents({ page = 0, size = 20, categoryId, from, to, sort = 'startTime,asc' } = {}) {
  const params = new URLSearchParams({ page, size, sort })
  if (categoryId != null) params.set('categoryId', categoryId)
  if (from != null) params.set('from', from)
  if (to != null) params.set('to', to)
  return request(`/api/events?${params}`)
}

export function getEvent(id) {
  return request(`/api/events/${id}`)
}

export function getUpcomingEvents(limit = 10) {
  return request(`/api/events/upcoming?limit=${limit}`)
}

export function getCategories() {
  return request('/api/events/categories')
}

export function subscribe({ eventId, phoneNumber }) {
  const normalizedPhoneNumber = normalizePhoneNumber(phoneNumber)
  return request('/api/subscriptions', {
    method: 'POST',
    body: JSON.stringify({ eventId, phoneNumber: normalizedPhoneNumber }),
  })
}

export function optOut({ subscriptionId, phoneNumber }) {
  return request('/api/subscriptions/opt-out', {
    method: 'POST',
    body: JSON.stringify({ subscriptionId, phoneNumber }),
  })
}
