import { describe, expect, it } from 'vitest'
import { distanceText, formatDateTime, money, petAgeText } from './format'

describe('展示格式化', () => {
  it('把月龄转换为自然年龄', () => {
    expect(petAgeText(0)).toBe('不足 1 个月')
    expect(petAgeText(14)).toBe('1 岁 2 个月')
    expect(petAgeText(24)).toBe('2 岁')
    expect(petAgeText(null)).toBe('未填写')
  })

  it('统一金额和距离格式', () => {
    expect(money('35')).toBe('35.00')
    expect(money(undefined)).toBe('0.00')
    expect(distanceText('0.406')).toBe('406 m')
    expect(distanceText(3)).toBe('3.0 km')
    expect(distanceText(null)).toBe('')
  })

  it('生成后端可接收的日期时间', () => {
    expect(formatDateTime(new Date(2026, 8, 7, 10, 2, 3))).toBe('2026-09-07 10:02:03')
  })
})
