import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import App from './App'

function renderApp(initialEntries = ['/']) {
  return render(
    <MemoryRouter initialEntries={initialEntries}>
      <App />
    </MemoryRouter>
  )
}

test('app renders without crashing', () => {
  renderApp()
})

test('/ renders KioskDisplay placeholder', () => {
  renderApp(['/'])
  expect(screen.getByTestId('kiosk-display')).toBeInTheDocument()
})

test('/tv renders TvDisplay placeholder', () => {
  renderApp(['/tv'])
  expect(screen.getByTestId('tv-display')).toBeInTheDocument()
})

test('/unsubscribe renders UnsubscribePage placeholder', () => {
  renderApp(['/unsubscribe'])
  expect(screen.getByTestId('unsubscribe-page')).toBeInTheDocument()
})

test('/admin/login renders AdminLogin placeholder', () => {
  renderApp(['/admin/login'])
  expect(screen.getByTestId('admin-login')).toBeInTheDocument()
})

test('/admin/events redirects to /admin/login when no token is present', () => {
  renderApp(['/admin/events'])
  expect(screen.getByTestId('admin-login')).toBeInTheDocument()
})
