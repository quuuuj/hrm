import request from '../utils/request'

const url = '/document'

/**
 * 分页查询知识库文档
 * @param params {current, size, oldName, status}
 */
export const list = (params) => {
  return request({
    url: url,
    method: 'get',
    params
  })
}

/**
 * 删除
 * @param id
 */
export const del = (id) => {
  return request({
    url: url + '/' + id,
    method: 'delete'
  })
}

export const deleteBatch = (ids) => {
  return request({
    url: url + '/batch/' + ids,
    method: 'delete'
  })
}

// 重试失败的摄入
export const retry = (id) => {
  return request({
    url: url + '/' + id + '/retry',
    method: 'post'
  })
}

// 查看文档切片
export const chunks = (id) => {
  return request({
    url: url + '/' + id + '/chunks',
    method: 'get'
  })
}

// 下载原始文件（对象键含斜杠，原样拼在 /download/ 之后）
export const download = (filename) => {
  return request({
    url: url + '/download/' + filename,
    method: 'get',
    responseType: 'blob'
  })
}

// 分片上传合并接口（分片协议统一走 /file-upload）
export const getCompleteApi = () => {
  return url + '/upload-task'
}
