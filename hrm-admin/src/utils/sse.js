/**
 * 全局 SSE 单例。
 *
 * Header 和业务页面共用一个 EventSource，按事件名分发。连接使用 httpOnly Cookie 鉴权，
 * 不依赖 request.js，避免 request → store → staff → sse 的循环依赖。
 */
const BASE_API = process.env.VUE_APP_BASE_API || ''

const EVENT_NAMES = ['connected', 'notification', 'task-update']

const listeners = {}
EVENT_NAMES.forEach(name => {
  listeners[name] = new Set()
})

let eventSource = null
let reconnectTimer = null

function scheduleReconnect () {
  if (reconnectTimer) return
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null
    connect()
  }, 10000)
}

function dispatch (eventName, event) {
  const handlers = listeners[eventName]
  if (!handlers || handlers.size === 0) return

  let payload = event.data
  if (eventName !== 'connected') {
    try {
      payload = JSON.parse(payload)
    } catch (e) {
      return
    }
  }

  handlers.forEach(handler => {
    try {
      handler(payload)
    } catch (e) {
      // 单个订阅者异常不影响其他订阅者
      console.error('[sse] listener error:', eventName, e)
    }
  })
}

function connect () {
  if (eventSource || reconnectTimer) return

  // EventSource 默认不带 Cookie；BASE_API 为绝对地址时需要显式开启
  const source = new EventSource(BASE_API + '/notification/subscribe', { withCredentials: true })
  eventSource = source

  EVENT_NAMES.forEach(name => {
    source.addEventListener(name, event => dispatch(name, event))
  })

  source.onerror = () => {
    // CONNECTING：浏览器会自动重连；CLOSED：需要自行重建（401、网关拒绝等场景）
    if (source.readyState === EventSource.CLOSED) {
      if (eventSource === source) {
        eventSource = null
      }
      scheduleReconnect()
    }
  }
}

/**
 * 订阅事件，返回取消订阅函数。首次注册时懒连接。
 * @param {string} eventName connected / notification / task-update
 * @param {Function} handler
 */
export function on (eventName, handler) {
  if (!listeners[eventName] || typeof handler !== 'function') {
    return () => {}
  }
  listeners[eventName].add(handler)
  connect()
  return () => off(eventName, handler)
}

/** 取消订阅 */
export function off (eventName, handler) {
  if (listeners[eventName]) {
    listeners[eventName].delete(handler)
  }
}

/** 关闭连接并清空所有监听器，退出登录时调用 */
export function close () {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  if (eventSource) {
    eventSource.close()
    eventSource = null
  }
  Object.keys(listeners).forEach(name => {
    listeners[name].clear()
  })
}
