import request from '@/utils/request'

export function listViolations(params) { return request.get('/rule/violations', { params }) }
export function resolveViolation(id) { return request.put('/rule/violations/' + id + '/resolve') }
export function refreshViolations() { return request.post('/rule/violations/refresh') }
export function getRuleConfigs() { return request.get('/rule/configs') }
export function createRuleConfig(data) { return request.post('/rule/configs', data) }
export function updateRuleConfig(data) { return request.put('/rule/configs', data) }
export function deleteRuleConfig(id) { return request.delete('/rule/configs/' + id) }
