import axios from 'axios'

// 모든 요청은 /api 로 나가고, vite dev 프록시(또는 nginx)가 Gateway/서비스로 전달한다.
const api = axios.create({ baseURL: '/api' })

// service-template 의 기본 CRUD 엔드포인트(/api/resources) 호출 예시.
// 실제 서비스 경로에 맞게 바꿔 쓴다.
export const resourceApi = {
  list: () => api.get('/resources'),
  create: (payload) => api.post('/resources', payload)
}

export default api
