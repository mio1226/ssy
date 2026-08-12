import request from '@/utils/request'

export function checkFeishuConfig() {
  return request.get('/feishu/check')
}

export function exportDisks(sheetId) {
  return request.post('/feishu/export/disks', null, { params: { sheetId } })
}

export function exportRecords(sheetId) {
  return request.post('/feishu/export/records', null, { params: { sheetId } })
}

export function exportViolations(sheetId) {
  return request.post('/feishu/export/violations', null, { params: { sheetId } })
}