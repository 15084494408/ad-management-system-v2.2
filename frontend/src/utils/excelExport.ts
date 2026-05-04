/**
 * 通用 Excel 导出工具（基于 xlsx + file-saver）
 *
 * 使用方式：
 * import { exportToExcel } from '@/utils/excelExport'
 * exportToExcel({
 *   filename: '订单列表',
 *   header: ['编号', '客户名', '金额', '日期'],
 *   data: [
 *     ['ORD001', '张三', '1200.00', '2026-04-28'],
 *     ['ORD002', '李四', '3400.00', '2026-04-27'],
 *   ],
 *   sheetName: '数据',
 *   title: '订单导出报表',
 *   summaryRow: ['合计', '', '4600.00', ''],
 * })
 */

import * as XLSX from 'xlsx'
import { saveAs } from 'file-saver'

export interface ExportOptions {
  /** 文件名（不含扩展名） */
  filename: string
  /** 表头行 */
  header: string[]
  /** 数据行（二维数组） */
  data: any[][]
  /** Sheet 名称，默认 'Sheet1' */
  sheetName?: string
  /** 标题行（合并单元格显示在顶部） */
  title?: string
  /** 汇总行（追加在数据末尾） */
  summaryRow?: string[]
  /** 额外的信息头（如公司名、日期范围等） */
  infoRows?: string[][]
  /** 列宽（字符数），默认自动 */
  colWidths?: number[]

  /** 是否自动添加边框样式等增强效果 */
  autoStyle?: boolean
}

/**
 * 导出数据到 Excel 文件
 */
export function exportToExcel(options: ExportOptions) {
  const {
    filename,
    header,
    data,
    sheetName = 'Sheet1',
    title,
    summaryRow,
    infoRows = [],
    colWidths,
  } = options

  const wsData: any[][] = []

  // ── 信息行（公司名、导出日期等）──
  if (infoRows.length > 0) {
    infoRows.forEach(row => wsData.push(row))
    wsData.push([]) // 空行
  }

  // ── 标题行（合并用）──
  let titleRowIndex = -1
  if (title) {
    titleRowIndex = wsData.length
    wsData.push([title])
    wsData.push([]) // 空行
  }

  // ── 表头 ──
  const headerRowIndex = wsData.length
  wsData.push(header)

  // ── 数据 ──
  const dataStartRowIndex = wsData.length
  data.forEach(row => wsData.push(row))

  // ── 汇总行 ──
  if (summaryRow) {
    wsData.push([])
    wsData.push(summaryRow)
  }

  // 创建工作表
  const ws = XLSX.utils.aoa_to_sheet(wsData)

  // ── 列宽 ──
  if (colWidths && colWidths.length) {
    ws['!cols'] = colWidths.map(w => ({ wch: w }))
  } else {
    // 自动计算列宽（取每列最大宽度）
    const maxLens = header.map((_, ci) => {
      let max = String(header[ci] || '').length
      for (let ri = 0; ri < data.length; ri++) {
        const cellVal = data[ri]?.[ci]
        if (cellVal != null) max = Math.max(max, String(cellVal).length)
      }
      return Math.min(Math.max(max + 2, 8), 50)
    })
    ws['!cols'] = maxLens.map(l => ({ wch: l }))
  }

  // ── 合并单元格 ──
  const merges: XLSX.Range[] = []
  if (title !== undefined && header.length > 1) {
    merges.push({ s: { r: titleRowIndex, c: 0 }, e: { r: titleRowIndex, c: header.length - 1 } })
  }
  if (merges.length > 0) {
    ws['!merges'] = merges
  }

  // 创建工作簿
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, sheetName.slice(0, 31))

  // 导出文件
  const fileName = `${filename}_${formatDate(new Date())}.xlsx`
  const wbout = XLSX.write(wb, { bookType: 'xlsx', type: 'array' })
  const blob = new Blob([wbout], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
  saveAs(blob, fileName)
}

/** 导出多个 Sheet 到同一个文件 */
export function exportMultiSheets(options: {
  filename: string
  sheets: Array<{
    sheetName: string
    header: string[]
    data: any[][]
    title?: string
    summaryRow?: string[]
    infoRows?: string[][]
  }>
}) {
  const wb = XLSX.utils.book_new()

  options.sheets.forEach(sheet => {
    const wsData: any[][] = []

    if (sheet.infoRows?.length) {
      sheet.infoRows.forEach(r => wsData.push(r))
      wsData.push([])
    }

    let titleRi = -1
    if (sheet.title) {
      titleRi = wsData.length
      wsData.push([sheet.title])
      wsData.push([])
    }

    wsData.push(sheet.header)
    sheet.data.forEach(r => wsData.push(r))

    if (sheet.summaryRow) {
      wsData.push([])
      wsData.push(sheet.summaryRow)
    }

    const ws = XLSX.utils.aoa_to_sheet(wsData)

    // 列宽
    const maxLens = sheet.header.map((_, ci) => {
      let max = String(sheet.header[ci] || '').length
      sheet.data.forEach(row => {
        if (row[ci] != null) max = Math.max(max, String(row[ci]).length)
      })
      return Math.min(Math.max(max + 2, 8), 50)
    })
    ws['!cols'] = maxLens.map(l => ({ wch: l }))

    const merges: XLSX.Range[] = []
    if (sheet.title !== undefined && sheet.header.length > 1) {
      merges.push({ s: { r: titleRi, c: 0 }, e: { r: titleRi, c: sheet.header.length - 1 } })
    }
    if (merges.length) ws['!merges'] = merges

    XLSX.utils.book_append_sheet(wb, ws, sheet.sheetName.slice(0, 31))
  })

  const fileName = `${options.filename}_${formatDate(new Date())}.xlsx`
  const wbout = XLSX.write(wb, { bookType: 'xlsx', type: 'array' })
  const blob = new Blob([wbout], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
  saveAs(blob, fileName)
}

// ── 辅助函数 ──

function formatDate(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

/** 将 el-table 的数据转为二维数组（按列定义提取字段） */
export function tableToArrays(
  tableData: any[],
  columns: Array<{ label: string; prop: string; formatter?: (val: any, row: any) => string }>,
): { header: string[]; data: any[][] } {
  const header = columns.map(c => c.label)
  const data = tableData.map(row =>
    columns.map(col =>
      col.formatter ? col.formatter(row[col.prop], row) : (row[col.prop] ?? ''),
    ),
  )
  return { header, data }
}
