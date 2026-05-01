import { act, renderHook } from '@testing-library/react'
import AuthProvider, { useAuth } from './AuthContext'

const wrapper = ({ children }) => <AuthProvider>{children}</AuthProvider>

test('token is null on initial render', () => {
  const { result } = renderHook(() => useAuth(), { wrapper })
  expect(result.current.token).toBeNull()
})

test('login sets the token', () => {
  const { result } = renderHook(() => useAuth(), { wrapper })
  act(() => result.current.login('test-token'))
  expect(result.current.token).toBe('test-token')
})

test('logout clears the token', () => {
  const { result } = renderHook(() => useAuth(), { wrapper })
  act(() => result.current.login('test-token'))
  act(() => result.current.logout())
  expect(result.current.token).toBeNull()
})

test('auth:expired window event calls logout', () => {
  const { result } = renderHook(() => useAuth(), { wrapper })
  act(() => result.current.login('test-token'))
  act(() => window.dispatchEvent(new Event('auth:expired')))
  expect(result.current.token).toBeNull()
})
