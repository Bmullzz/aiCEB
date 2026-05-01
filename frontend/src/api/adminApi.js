async function adminRequest(path, token, options = {}) {
  const url = `${import.meta.env.VITE_API_BASE_URL}${path}`
  const response = await fetch(url, {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
      ...options.headers,
    },
    ...options,
  })
  if (response.status === 401) {
    window.dispatchEvent(new CustomEvent('auth:expired'))
    return Promise.reject({ status: 401, code: 'UNAUTHORIZED', message: 'Session expired.' })
  }
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
  return response.json()
}

export function login(username, password) {
  return request('/api/admin/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  })
}

export function adminGetEvents({ page = 0, size = 20, categoryId, status, token }) {
  const params = new URLSearchParams({ page, size })
  if (categoryId != null) params.set('categoryId', categoryId)
  if (status != null) params.set('status', status)
  return adminRequest(`/api/admin/events?${params}`, token)
}

export function adminGetEvent(id, token) {
  return adminRequest(`/api/admin/events/${id}`, token)
}

export function adminCreateEvent(data, token) {
  return adminRequest('/api/admin/events', token, {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

export function adminUpdateEvent(id, data, token) {
  return adminRequest(`/api/admin/events/${id}`, token, {
    method: 'PUT',
    body: JSON.stringify(data),
  })
}

export function adminUpdateStatus(id, data, token) {
  return adminRequest(`/api/admin/events/${id}/status`, token, {
    method: 'PATCH',
    body: JSON.stringify(data),
  })
}

export function adminUpdateVisibility(id, visible, token) {
  return adminRequest(`/api/admin/events/${id}/visibility`, token, {
    method: 'PATCH',
    body: JSON.stringify({ visible }),
  })
}

export function adminBroadcast(id, message, token) {
  return adminRequest(`/api/admin/events/${id}/broadcast`, token, {
    method: 'POST',
    body: JSON.stringify({ message }),
  })
}

export function adminGetSubscribers(id, token) {
  return adminRequest(`/api/admin/events/${id}/subscribers`, token)
}

export function adminGetNotificationLog(id, token) {
  return adminRequest(`/api/admin/events/${id}/notification-log`, token)
}

export function adminCreateCategory(data, token) {
  return adminRequest('/api/admin/categories', token, {
    method: 'POST',
    body: JSON.stringify(data),
  })
}

export function adminUpdateCategory(id, data, token) {
  return adminRequest(`/api/admin/categories/${id}`, token, {
    method: 'PATCH',
    body: JSON.stringify(data),
  })
}
