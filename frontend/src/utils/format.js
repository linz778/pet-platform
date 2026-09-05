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

/**
 * 金额统一两位小数。
 * @param {number|string|null|undefined} v
 * @returns {string}
 */
export function money(v) {
  const n = Number(v)
  // 后端配了 Jackson non_null，没赋值的金额字段是「键不存在」而不是 0，
  // 到前端就是 undefined；Number(undefined) 是 NaN，不挡一下会渲染出「NaN」
  return Number.isFinite(n) ? n.toFixed(2) : '0.00'
}

/**
 * Date → 'yyyy-MM-dd HH:mm:ss'，即后端 LocalDateTime 反序列化认的格式。
 *
 * el-date-picker 的 value-format 只格式化用户挑出来的值；
 * 代码里算出来的默认值（比如「明天 10:00」）必须自己格式化。
 * @param {Date} date
 * @returns {string}
 */
export function formatDateTime(date) {
  const p = (n) => String(n).padStart(2, '0')
  return (
    `${date.getFullYear()}-${p(date.getMonth() + 1)}-${p(date.getDate())} ` +
    `${p(date.getHours())}:${p(date.getMinutes())}:${p(date.getSeconds())}`
  )
}
