import { getAvatar } from '@/api/docs'
import store from '../store'
import defaultAvatar from '../assets/images/avatar.png'

export { defaultAvatar }

/**
 * 渲染用户头像：先落默认头像，再异步拉取自定义头像覆盖。
 * 无头像、接口失败、返回非图片内容或图片解码失败时，都保持默认头像不动。
 */
export const setAvatar = (img) => {
  if (!img) return
  img.src = defaultAvatar

  const staff = store.getters.staff || {}
  const avatar = staff.avatar
  if (!avatar) return

  getAvatar(avatar).then(response => {
    const blob = response.data
    // 文件缺失时后端可能以 200 + JSON 错误体返回，非图片内容一律忽略
    if (!(blob instanceof Blob) || blob.size === 0 || blob.type.indexOf('json') !== -1) return

    const objectUrl = window.URL.createObjectURL(blob)
    // 先挂事件再赋值，确保加载/解码失败时能回落到默认头像
    img.onload = () => window.URL.revokeObjectURL(objectUrl)
    img.onerror = () => {
      img.src = defaultAvatar
      window.URL.revokeObjectURL(objectUrl)
    }
    img.src = objectUrl
  }).catch(error => {
    console.warn('头像加载失败，使用默认头像:', error)
  })
}
