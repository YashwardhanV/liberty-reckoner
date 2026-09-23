import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  withCredentials: true,
  withXSRFToken: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' }
})

api.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401 && !error.config?.url?.includes('/auth/login')) {
      window.dispatchEvent(new CustomEvent('libertyreckoner:unauthorized'))
    }
    return Promise.reject(error)
  }
)

export function errorMessage(error) {
  return error.response?.data?.message || (error.code === 'ECONNABORTED'
    ? 'The service took too long to respond.'
    : 'Something went wrong. Please try again.')
}

export default api
