import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('atm_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

let refreshing = null

async function tryRefresh() {
  const refreshToken = localStorage.getItem('atm_refresh')
  if (!refreshToken) return null
  if (!refreshing) {
    refreshing = axios.post('/api/auth/refresh', { refreshToken })
      .then(({ data }) => {
        localStorage.setItem('atm_token', data.token)
        localStorage.setItem('atm_refresh', data.refreshToken)
        localStorage.setItem('atm_card', data.cardNumber || '')
        localStorage.setItem('atm_username', data.username || '')
        localStorage.setItem('atm_role', data.role || 'USER')
        return data
      })
      .finally(() => {
        refreshing = null
      })
  }
  return refreshing
}

function forceLogout() {
  localStorage.removeItem('atm_token')
  localStorage.removeItem('atm_refresh')
  localStorage.removeItem('atm_card')
  localStorage.removeItem('atm_username')
  localStorage.removeItem('atm_role')
  if (!window.location.pathname.startsWith('/login')
      && !window.location.pathname.startsWith('/register')
      && !window.location.pathname.startsWith('/reset-pin')) {
    window.location.href = '/login'
  }
}

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config
    const status = error.response?.status
    const isAuthPath = original?.url?.includes('/auth/login')
      || original?.url?.includes('/auth/refresh')
      || original?.url?.includes('/auth/register')

    if (status === 401 && original && !original._retried && !isAuthPath) {
      original._retried = true
      try {
        const data = await tryRefresh()
        if (data?.token) {
          original.headers.Authorization = `Bearer ${data.token}`
          return api(original)
        }
      } catch {
        forceLogout()
        return Promise.reject(error)
      }
      forceLogout()
      return Promise.reject(error)
    }

    if (status === 401) {
      forceLogout()
    }
    return Promise.reject(error)
  },
)

export function apiErrorMessage(error) {
  return error.response?.data?.message || error.message || 'Something went wrong';
}

export function apiErrorDetails(error) {
  const details = error.response?.data?.errors
  if (Array.isArray(details) && details.length > 0) {
    return details.join('\n')
  }
  return null
}

export default api
