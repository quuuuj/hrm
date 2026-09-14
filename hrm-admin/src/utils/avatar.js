import { getAvatar } from '@/api/docs'
import store from '../store'
import defaultAvatar from '../assets/images/avatar.png'

export const setAvatar = (img) => {
  if (!img) return
  const avatar = store.getters.staff.avatar
  if (avatar != null && avatar !== '') { // 当没有头像时，使用默认头像
    getAvatar(avatar).then(response => {
      // 如果头像不存在使用默认头像
      if (response.data && response.data.size > 0) {
        img.src = window.URL.createObjectURL(new Blob([response.data]))
        img.onload = () => {
          URL.revokeObjectURL(img.src)
        }
      } else {
        img.src = defaultAvatar
      }
    }).catch(error => {
      // 文件不存在或其他错误时，使用默认头像
      console.warn('头像加载失败:', error)
      img.src = defaultAvatar
    })
  } else {
    img.src = defaultAvatar
  }
}
