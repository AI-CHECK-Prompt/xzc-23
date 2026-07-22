import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

request.interceptors.response.use(
  (resp) => resp.data,
  (err) => {
    console.error('移动端请求异常：', err)
    return Promise.reject(err)
  }
)

export default request
