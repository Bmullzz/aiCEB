import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'
import {
  getCategories,
  getEvent,
  getEvents,
  getUpcomingEvents,
  optOut,
  subscribe,
} from './api'

const BASE = 'http://localhost:8080'

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

describe('getEvents', () => {
  test('builds correct URL with default params', async () => {
    const fetch = mockFetch(200, { content: [] })
    vi.stubGlobal('fetch', fetch)
    await getEvents()
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/events?'),
      expect.any(Object)
    )
    const url = fetch.mock.calls[0][0]
    expect(url).toContain('page=0')
    expect(url).toContain('size=20')
    expect(url).toContain('sort=startTime%2Casc')
  })

  test('includes categoryId in query string when provided', async () => {
    const fetch = mockFetch(200, { content: [] })
    vi.stubGlobal('fetch', fetch)
    await getEvents({ categoryId: 'abc-123' })
    expect(fetch.mock.calls[0][0]).toContain('categoryId=abc-123')
  })

  test('omits categoryId from query string when not provided', async () => {
    const fetch = mockFetch(200, { content: [] })
    vi.stubGlobal('fetch', fetch)
    await getEvents()
    expect(fetch.mock.calls[0][0]).not.toContain('categoryId')
  })
})

describe('getEvent', () => {
  test('calls the correct URL', async () => {
    const id = '11111111-0000-0000-0000-000000000001'
    const fetch = mockFetch(200, { id })
    vi.stubGlobal('fetch', fetch)
    await getEvent(id)
    expect(fetch.mock.calls[0][0]).toBe(`${BASE}/api/events/${id}`)
  })

  test('rejects with server error code on 404 response', async () => {
    const fetch = mockFetch(404, { status: 404, code: 'EVENT_NOT_FOUND', message: 'Not found.' })
    vi.stubGlobal('fetch', fetch)
    await expect(getEvent('bad-id')).rejects.toMatchObject({
      status: 404,
      code: 'EVENT_NOT_FOUND',
      message: 'Not found.',
    })
  })
})

describe('getUpcomingEvents', () => {
  test('includes limit in query string', async () => {
    const fetch = mockFetch(200, [])
    vi.stubGlobal('fetch', fetch)
    await getUpcomingEvents(5)
    expect(fetch.mock.calls[0][0]).toContain('limit=5')
  })
})

describe('subscribe — phone normalization', () => {
  test('normalizes "2155550123" to "+12155550123"', async () => {
    const fetch = mockFetch(201, { subscriptionId: 'x', status: 'ACTIVE' })
    vi.stubGlobal('fetch', fetch)
    await subscribe({ eventId: 'e1', phoneNumber: '2155550123' })
    const body = JSON.parse(fetch.mock.calls[0][1].body)
    expect(body.phoneNumber).toBe('+12155550123')
  })

  test('preserves "+442071234567" unchanged', async () => {
    const fetch = mockFetch(201, { subscriptionId: 'x', status: 'ACTIVE' })
    vi.stubGlobal('fetch', fetch)
    await subscribe({ eventId: 'e1', phoneNumber: '+442071234567' })
    const body = JSON.parse(fetch.mock.calls[0][1].body)
    expect(body.phoneNumber).toBe('+442071234567')
  })

  test('strips formatting from "(215) 555-0123"', async () => {
    const fetch = mockFetch(201, { subscriptionId: 'x', status: 'ACTIVE' })
    vi.stubGlobal('fetch', fetch)
    await subscribe({ eventId: 'e1', phoneNumber: '(215) 555-0123' })
    const body = JSON.parse(fetch.mock.calls[0][1].body)
    expect(body.phoneNumber).toBe('+12155550123')
  })
})

describe('error handling', () => {
  test('rejects with { status, code, message } shape on non-2xx', async () => {
    const fetch = mockFetch(500, { status: 500, code: 'INTERNAL_ERROR', message: 'Boom.' })
    vi.stubGlobal('fetch', fetch)
    await expect(getCategories()).rejects.toMatchObject({
      status: 500,
      code: 'INTERNAL_ERROR',
      message: 'Boom.',
    })
  })
})
