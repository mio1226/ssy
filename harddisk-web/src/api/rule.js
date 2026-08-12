import request from '@/utils/request'

export function listViolations(params) { return request.get('/rule/violations', { params }) }
export function resolveViolation(id) { return request.put(`/rule/violations/${id}/resolve`) }
export function getRuleConfigs() { return request.get('/rule/configs') }
export function updateRuleConfig(data) { return request.put('/rule/configs', data) }
