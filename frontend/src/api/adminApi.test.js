import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import {
  adminGetEvent,
  adminGetEvents,
  adminUpdateVisibility,
  login,
} from './adminApi'

const BASE = 'http://localhost:8080'
const TOKEN = 'test-jwt-token'

function mockFetch(status, body) {
  return vi.fn().mockResolvedValue({
    ok: status >= 200 && status < 300,
    status,
    json: () => Promise.resolve(body),
    statusText: 'OK',
  })
}

beforeEach(() => {
  vi.stubEnv('VITE_API_BASE_URL', BASE)
})

afterEach(() => {
  vi.unstubAllGlobals()
  vi.unstubAllEnvs()
})

describe('login', () => {
  test('POSTs to /api/admin/auth/login without Authorization header', async () => {
    const fetch = mockFetch(200, { token: 'jwt', expiresIn: 28800 })
    vi.stubGlobal('fetch', fetch)
    await login('admin', 'secret')
    const [url, opts] = fetch.mock.calls[0]
    expect(url).toBe(`${BASE}/api/admin/auth/login`)
    expect(opts.method).toBe('POST')
    expect(opts.headers?.Authorization).toBeUndefined()
    expect(JSON.parse(opts.body)).toEqual({ username: 'admin', password: 'secret' })
  })
})

describe('adminGetEvents', () => {
  test('includes Authorization: Bearer header', async () => {
    const fetch = mockFetch(200, { content: [] })
    vi.stubGlobal('fetch', fetch)
    await adminGetEvents({ token: TOKEN })
    const opts = fetch.mock.calls[0][1]
    expect(opts.headers?.Authorization).toBe(`Bearer ${TOKEN}`)
  })
})

describe('adminGetEvent', () => {
  test('uses correct URL with id path param', async () => {
    const id = '22222222-0000-0000-0000-000000000002'
    const fetch = mockFetch(200, { id })
    vi.stubGlobal('fetch', fetch)
    await adminGetEvent(id, TOKEN)
    expect(fetch.mock.calls[0][0]).toBe(`${BASE}/api/admin/events/${id}`)
  })
})

describe('adminUpdateVisibility', () => {
  test('sends { visible: true } in request body', async () => {
    const id = 'event-id'
    const fetch = mockFetch(200, { id, visible: true })
    vi.stubGlobal('fetch', fetch)
    await adminUpdateVisibility(id, true, TOKEN)
    const body = JSON.parse(fetch.mock.calls[0][1].body)
    expect(body).toEqual({ visible: true })
  })
})

describe('401 handling', () => {
  test('dispatches auth:expired event on 401', async () => {
    const fetch = mockFetch(401, { status: 401, code: 'UNAUTHORIZED', message: 'Expired.' })
    vi.stubGlobal('fetch', fetch)
    const dispatchSpy = vi.spyOn(window, 'dispatchEvent')
    await expect(adminGetEvent('x', TOKEN)).rejects.toBeDefined()
    expect(dispatchSpy).toHaveBeenCalledWith(
      expect.objectContaining({ type: 'auth:expired' })
    )
  })

  test('rejects with 401 error object after dispatching auth:expired', async () => {
    const fetch = mockFetch(401, {})
    vi.stubGlobal('fetch', fetch)
    vi.spyOn(window, 'dispatchEvent')
    await expect(adminGetEvent('x', TOKEN)).rejects.toMatchObject({
      status: 401,
      code: 'UNAUTHORIZED',
      message: 'Session expired.',
    })
  })
})
