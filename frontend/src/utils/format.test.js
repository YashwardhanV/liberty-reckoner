import { describe, expect, it } from 'vitest'
import { daysText, initials, labelStatus, words } from './format'

describe('format utilities', () => {
  it('expresses deadline direction without losing the legal meaning', () => {
    expect(daysText(-12)).toBe('12 days overdue')
    expect(daysText(0)).toBe('Due today')
    expect(daysText(null)).toBe('Review required')
  })

  it('uses the Liberty Reckoner fallback identity', () => {
    expect(initials()).toBe('LR')
    expect(initials('District Legal')).toBe('DL')
  })

  it('normalises enum values for the interface', () => {
    expect(labelStatus('MAXIMUM_REACHED')).toBe('Maximum reached')
    expect(words('COURT_REGISTRY')).toBe('Court Registry')
  })
})
