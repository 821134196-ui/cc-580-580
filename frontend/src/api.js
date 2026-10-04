/** 统一 API 访问：错误时抛出 { status, message } */
async function request(path, options = {}) {
  const res = await fetch(path, {
    headers: { 'Content-Type': 'application/json' },
    ...options
  });
  if (!res.ok) {
    let message = '请求失败 (' + res.status + ')';
    try {
      const data = await res.json();
      if (data && data.error) message = data.error;
    } catch (e) {
      /* ignore */
    }
    const err = new Error(message);
    err.status = res.status;
    throw err;
  }
  if (res.status === 204) return null;
  return res.json();
}

export const api = {
  listCourses: () => request('/api/courses'),
  createCourse: (body) => request('/api/courses', { method: 'POST', body: JSON.stringify(body) }),
  updateCourse: (id, body) => request('/api/courses/' + id, { method: 'PUT', body: JSON.stringify(body) }),

  listStudents: () => request('/api/students'),
  createStudent: (body) => request('/api/students', { method: 'POST', body: JSON.stringify(body) }),
  studentRecords: (id) => request(`/api/students/${id}/records`),
  studentAttempts: (id) => request(`/api/students/${id}/attempts`),

  upsertStudy: (body) => request('/api/records/study', { method: 'PUT', body: JSON.stringify(body) }),
  addExam: (body) => request('/api/records/exam', { method: 'POST', body: JSON.stringify(body) }),
  listAttempts: (studentId, courseId) => request(`/api/records/exam?studentId=${studentId}&courseId=${courseId}`),

  eligibility: (studentId, courseId) => request(`/api/eligibility?studentId=${studentId}&courseId=${courseId}`),

  issue: (body) => request('/api/certificates/issue', { method: 'POST', body: JSON.stringify(body) }),
  listCertificates: (studentId) => request('/api/certificates' + (studentId ? `?studentId=${studentId}` : '')),
  certificate: (id) => request('/api/certificates/' + id),
  certificateChain: (id) => request(`/api/certificates/${id}/chain`),

  submitCorrection: (body) => request('/api/corrections', { method: 'POST', body: JSON.stringify(body) }),
  listCorrections: (status) => request('/api/corrections' + (status ? `?status=${status}` : '')),
  correction: (id) => request('/api/corrections/' + id),
  returnCorrection: (id, body) => request(`/api/corrections/${id}/return`, { method: 'POST', body: JSON.stringify(body) }),
  resubmitCorrection: (id, body) => request(`/api/corrections/${id}/resubmit`, { method: 'POST', body: JSON.stringify(body) }),
  approveCorrection: (id, body) => request(`/api/corrections/${id}/approve`, { method: 'POST', body: JSON.stringify(body) }),
  rejectCorrection: (id, body) => request(`/api/corrections/${id}/reject`, { method: 'POST', body: JSON.stringify(body) }),

  verify: (certNumber) => request('/api/verify/' + encodeURIComponent(certNumber)),
  listMails: () => request('/api/mails')
};

export function fmtTime(value) {
  if (!value) return '—';
  return String(value).replace('T', ' ').slice(0, 19);
}
