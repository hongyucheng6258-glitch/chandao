import * as XLSX from 'xlsx'

/**
 * 通用导出 Excel（前端基于 SheetJS）
 * @param {Array<{title:string, key:string, formatter?:(row:any)=>any}>} columns 列定义
 * @param {Array<Object>} rows 数据行
 * @param {string} filename 文件名（不含扩展名）
 */
export function exportToExcel(columns, rows, filename) {
  const header = columns.map((c) => c.title)
  const data = rows.map((row) =>
    columns.map((c) => {
      const v = c.formatter ? c.formatter(row) : getByPath(row, c.key)
      return v == null ? '' : v
    })
  )
  const aoa = [header, ...data]
  const ws = XLSX.utils.aoa_to_sheet(aoa)
  ws['!cols'] = columns.map((c) => ({ wch: Math.max(10, (c.title.length || 8) + 4) }))
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, 'Sheet1')
  XLSX.writeFile(wb, `${filename}.xlsx`)
}

function getByPath(obj, path) {
  if (path == null) return ''
  return String(path.split('.').reduce((o, k) => (o == null ? '' : o[k]), obj) ?? '')
}
