import request from '@/utils/request'

export function listDisks(params) { return request.get('/disk/list', { params }) }
export function getDisk(id) { return request.get(`/disk/${id}`) }
export function createDisk(data) { return request.post('/disk', data) }
export function updateDisk(id, data) { return request.put(`/disk/${id}`, data) }
export function deleteDisk(id) { return request.delete(`/disk/${id}`) }
export function outbound(data) { return request.post('/disk/outbound', data) }
export function inbound(data) { return request.post('/disk/inbound', data) }
export function getRecords(diskId) { return request.get(`/disk/${diskId}/records`) }
export function getRecord(recordId) { return request.get(`/disk/records/${recordId}`) }

// 记录管理 CRUD
export function listAllRecords(params) { return request.get('/disk/records/list', { params }) }
export function updateRecord(id, data) { return request.put(`/disk/records/${id}`, data) }
export function deleteRecord(id) { return request.delete(`/disk/records/${id}`) }