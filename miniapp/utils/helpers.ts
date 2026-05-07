/** 格式化金额 */
export function formatMoney(amount: number | string | null | undefined): string {
  if (amount === null || amount === undefined || amount === '') return '0.00'
  const num = typeof amount === 'string' ? parseFloat(amount) : amount
  if (isNaN(num)) return '0.00'
  return num.toFixed(2)
}

/** 格式化日期 */
export function formatDate(dateStr: string | null | undefined): string {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return dateStr
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/** 格式化日期时间 */
export function formatDateTime(dateStr: string | null | undefined): string {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return dateStr
  return formatDate(dateStr) + ' ' +
    String(d.getHours()).padStart(2, '0') + ':' +
    String(d.getMinutes()).padStart(2, '0')
}

/** 相对时间 */
export function timeAgo(dateStr: string): string {
  const now = Date.now()
  const target = new Date(dateStr).getTime()
  if (isNaN(target)) return dateStr
  const diff = now - target
  const min = Math.floor(diff / 60000)
  if (min < 1) return '刚刚'
  if (min < 60) return `${min}分钟前`
  const hours = Math.floor(min / 60)
  if (hours < 24) return `${hours}小时前`
  const days = Math.floor(hours / 24)
  if (days < 30) return `${days}天前`
  return formatDate(dateStr)
}

/** 订单状态映射（后端：1=待处理 2=进行中 3=已完成 4=已取消） */
export const ORDER_STATUS: Record<number, string> = {
  1: '待处理',
  2: '进行中',
  3: '已完成',
  4: '已取消'
}

/** 订单状态颜色 */
export const ORDER_STATUS_COLOR: Record<number, string> = {
  1: 'warning',
  2: 'primary',
  3: 'success',
  4: 'default'
}

/** 支付状态映射 */
export const PAYMENT_STATUS: Record<number, string> = {
  1: '未付',
  2: '部分',
  3: '已付清',
  4: '抹零'
}
