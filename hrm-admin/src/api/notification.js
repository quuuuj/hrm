import request from '../utils/request'

const url = '/notification'

/** 当前用户通知分页 */
export const pageNotifications = (params) => {
  return request({
    url: url + '/page',
    method: 'get',
    params
  })
}

/** 当前用户未读通知数 */
export const unreadCount = () => {
  return request({
    url: url + '/unread-count',
    method: 'get'
  })
}

/** 标记单条通知已读 */
export const markRead = (id) => {
  return request({
    url: url + '/read/' + id,
    method: 'put'
  })
}

/** 全部标记已读 */
export const markAllRead = () => {
  return request({
    url: url + '/read-all',
    method: 'put'
  })
}
