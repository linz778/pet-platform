/**
 * 展示层格式化。放这里的都是「后端给原始值、前端负责说人话」的转换。
 */

/**
 * 月龄转自然语言。t_pet.age_months 存的是整数月，直接显示「26」没人知道是多大。
 * @param {number|null|undefined} months
 * @returns {string}
 */
export function petAgeText(months) {
  const m = Number(months)
  if (months === null || months === undefined || months === '' || !Number.isFinite(m) || m < 0) {
    return '未填写'
  }
  if (m < 1) return '不足 1 个月'
  const years = Math.floor(m / 12)
  const rest = m % 12
  if (years === 0) return `${rest} 个月`
  return rest === 0 ? `${years} 岁` : `${years} 岁 ${rest} 个月`
}
