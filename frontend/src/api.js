const BASE = '/api'

async function request(path, options = {}) {
  const res = await fetch(BASE + path, {
    headers: { 'Content-Type': 'application/json' },
    ...options
  })
  let data = null
  try {
    data = await res.json()
  } catch {
    // 无响应体
  }
  if (!res.ok) {
    const message = data?.error || `请求失败(HTTP ${res.status})`
    const err = new Error(message)
    err.status = res.status
    err.data = data
    throw err
  }
  return data
}

export const api = {
  // 基础数据
  students: () => request('/students'),
  createStudent: (fullName, email) => request('/students', { method: 'POST', body: JSON.stringify({ fullName, email }) }),
  courses: () => request('/courses'),
  createCourse: (payload) => request('/courses', { method: 'POST', body: JSON.stringify(payload) }),
  updateCourse: (id, payload) => request(`/courses/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),

  // 报名与资格
  enrollments: () => request('/enrollments'),
  enrollmentDetail: (id) => request(`/enrollments/${id}`),
  createEnrollment: (studentId, courseId) => request('/enrollments', { method: 'POST', body: JSON.stringify({ studentId, courseId }) }),
  setHours: (id, completedHours) => request(`/enrollments/${id}/hours`, { method: 'POST', body: JSON.stringify({ completedHours }) }),
  addAttempt: (id, score) => request(`/enrollments/${id}/attempts`, { method: 'POST', body: JSON.stringify({ score }) }),
  confirmName: (id, confirmedName) => request(`/enrollments/${id}/confirm-name`, { method: 'POST', body: JSON.stringify({ confirmedName }) }),
  eligibility: (id) => request(`/enrollments/${id}/eligibility`),
  issue: (id) => request(`/enrollments/${id}/issue`, { method: 'POST', body: '{}' }),
  certificateChain: (id) => request(`/enrollments/${id}/certificates`),

  // 更正
  corrections: (status) => request('/corrections' + (status ? `?status=${status}` : '')),
  submitCorrection: (certificateId, payload) => request(`/certificates/${certificateId}/corrections`, { method: 'POST', body: JSON.stringify(payload) }),
  supplement: (id, materials) => request(`/corrections/${id}/materials`, { method: 'POST', body: JSON.stringify({ materials }) }),
  review: (id, decision, reviewNote) => request(`/corrections/${id}/review`, { method: 'POST', body: JSON.stringify({ decision, reviewNote }) }),

  // 核验与邮箱
  verify: (number) => request(`/verify/${encodeURIComponent(number)}`),
  emails: () => request('/emails')
}
