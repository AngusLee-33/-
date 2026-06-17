import axios from 'axios'

const instance = axios.create({
  baseURL: '/api',
  timeout: 10000
})

export const getErrorMessage = (error, fallback = '操作失败') => {
  return error?.response?.data?.message || error?.message || fallback
}

instance.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

instance.interceptors.response.use(
  response => {
    if (response.config.responseType === 'blob') {
      return response
    }
    return response.data
  },
  error => {
    if (error.response && error.response.status === 401 && window.location.pathname !== '/login') {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login'
    }
    if (error.response && error.response.status === 403 && window.location.pathname !== '/403') {
      window.location.href = '/403'
    }
    return Promise.reject(error)
  }
)

export const authApi = {
  register(data) {
    return instance.post('/auth/register', data)
  },
  login(data) {
    return instance.post('/auth/login', data)
  },
  sendResetPasswordCode(phone) {
    return instance.post('/auth/password/code', { phone })
  },
  resetPassword(data) {
    return instance.post('/auth/password/reset', data)
  }
}

export const userApi = {
  getProfile() {
    return instance.get('/user/profile')
  },
  updateProfile(data) {
    return instance.put('/user/profile', data)
  }
}

export const carpoolApi = {
  getAll() {
    return instance.get('/carpool')
  },
  getById(id) {
    return instance.get(`/carpool/${id}`)
  },
  create(data) {
    return instance.post('/carpool', data)
  },
  getMy() {
    return instance.get('/carpool/my')
  },
  getReceivedApplications() {
    return instance.get('/carpool/applications/received')
  },
  apply(id) {
    return instance.post(`/carpool/${id}/apply`)
  },
  cancel(id) {
    return instance.put(`/carpool/${id}/cancel`)
  },
  handleApply(carpoolId, applyId, status) {
    return instance.put(`/carpool/${carpoolId}/apply/${applyId}`, { status })
  }
}

export const partTimeApi = {
  getAll() {
    return instance.get('/part-time')
  },
  getById(id) {
    return instance.get(`/part-time/${id}`)
  },
  create(data) {
    return instance.post('/part-time', data)
  },
  getMy() {
    return instance.get('/part-time/my')
  },
  getReceivedApplications() {
    return instance.get('/part-time/applications/received')
  },
  apply(id, resume) {
    return instance.post(`/part-time/${id}/apply`, { resume })
  },
  close(id) {
    return instance.put(`/part-time/${id}/close`)
  },
  handleApply(partTimeId, applyId, status) {
    return instance.put(`/part-time/${partTimeId}/apply/${applyId}`, { status })
  }
}

export const secondhandApi = {
  getAll() {
    return instance.get('/secondhand')
  },
  getById(id) {
    return instance.get(`/secondhand/${id}`)
  },
  create(data) {
    return instance.post('/secondhand', data)
  },
  getMy() {
    return instance.get('/secondhand/my')
  },
  getByCategory(category) {
    return instance.get(`/secondhand/category/${category}`)
  },
  markAsSold(id) {
    return instance.put(`/secondhand/${id}/sold`)
  },
  offline(id) {
    return instance.put(`/secondhand/${id}/offline`)
  }
}

export const uploadApi = {
  image(file) {
    const formData = new FormData()
    formData.append('file', file)
    return instance.post('/upload/image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}

export const chatApi = {
  getConversations() {
    return instance.get('/chat/conversations')
  },
  getMessages(params) {
    return instance.get('/chat/messages', { params })
  },
  sendMessage(data) {
    return instance.post('/chat/messages', data)
  },
  getUnreadCount() {
    return instance.get('/chat/unread-count')
  }
}

export const lostFoundApi = {
  getAll(type) {
    return instance.get('/lost-found', { params: type ? { type } : {} })
  },
  create(data) {
    return instance.post('/lost-found', data)
  },
  getMy() {
    return instance.get('/lost-found/my')
  },
  resolve(id) {
    return instance.put(`/lost-found/${id}/resolve`)
  },
  close(id) {
    return instance.put(`/lost-found/${id}/close`)
  }
}

export const orderApi = {
  create(data) {
    return instance.post('/order', data)
  },
  getById(id) {
    return instance.get(`/order/${id}`)
  },
  getMy() {
    return instance.get('/order/my')
  },
  pay(id) {
    return instance.post(`/order/${id}/pay`)
  },
  complete(id) {
    return instance.post(`/order/${id}/complete`)
  },
  cancel(id) {
    return instance.post(`/order/${id}/cancel`)
  }
}

export const adminApi = {
  getSummary() {
    return instance.get('/admin/summary')
  },
  getUsers() {
    return instance.get('/admin/users')
  },
  updateUserStatus(id, status) {
    return instance.put(`/admin/users/${id}/status`, { status })
  },
  updateUserRole(id, status) {
    return instance.put(`/admin/users/${id}/role`, { status })
  },
  updateUserPassword(id, password) {
    return instance.put(`/admin/users/${id}/password`, { password })
  },
  deleteUser(id) {
    return instance.delete(`/admin/users/${id}`)
  },
  getCarpools() {
    return instance.get('/admin/carpools')
  },
  updateCarpoolStatus(id, status) {
    return instance.put(`/admin/carpools/${id}/status`, { status })
  },
  deleteCarpool(id) {
    return instance.delete(`/admin/carpools/${id}`)
  },
  getPartTimes() {
    return instance.get('/admin/part-times')
  },
  updatePartTimeStatus(id, status) {
    return instance.put(`/admin/part-times/${id}/status`, { status })
  },
  deletePartTime(id) {
    return instance.delete(`/admin/part-times/${id}`)
  },
  getSecondhands() {
    return instance.get('/admin/secondhands')
  },
  updateSecondhandStatus(id, status) {
    return instance.put(`/admin/secondhands/${id}/status`, { status })
  },
  deleteSecondhand(id) {
    return instance.delete(`/admin/secondhands/${id}`)
  },
  getLostFounds() {
    return instance.get('/admin/lost-founds')
  },
  updateLostFoundStatus(id, status) {
    return instance.put(`/admin/lost-founds/${id}/status`, { status })
  },
  deleteLostFound(id) {
    return instance.delete(`/admin/lost-founds/${id}`)
  },
  getOrders() {
    return instance.get('/admin/orders')
  },
  deleteOrder(id) {
    return instance.delete(`/admin/orders/${id}`)
  },
  backupDatabase(data) {
    return instance.post('/admin/backup', data, {
      responseType: 'blob'
    })
  },
  importSpreadsheet(file) {
    const formData = new FormData()
    formData.append('file', file)
    return instance.post('/admin/import/spreadsheet', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  downloadUserImportTemplate() {
    return instance.get('/admin/import/user-template', {
      responseType: 'blob'
    })
  },
  exportUsers() {
    return instance.get('/admin/users/export', {
      responseType: 'blob'
    })
  }
}
