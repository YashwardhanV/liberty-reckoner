import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import StatusBadge from './StatusBadge'

describe('StatusBadge', () => {
  it('renders the legal status as a human-readable label', () => {
    render(<StatusBadge status="ACTION_OVERDUE" />)
    expect(screen.getByText('Action overdue')).toHaveClass('status-badge', 'status-action_overdue')
  })

  it('supports the compact presentation used in dense case lists', () => {
    render(<StatusBadge status="BAIL_GRANTED" compact />)
    expect(screen.getByText('Bail granted')).toHaveClass('status-compact')
  })
})
