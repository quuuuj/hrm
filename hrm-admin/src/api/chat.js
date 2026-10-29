import request from '../utils/request'

const url = '/chat'

export const chat = (data) => {
  return request({
    url: url + '/chat',
    method: 'post',
    data
  })
}

/**
 * SSE 流式聊天——使用 fetch + ReadableStream 手动解析 SSE 事件流
 *
 * 为什么用 fetch 而不是 EventSource？
 * - 聊天接口是 POST 请求，需要携带 JSON 请求体（对话内容、会话 ID 等）
 * - 浏览器原生 EventSource 只支持 GET，无法发送请求体
 * - 同时需要携带 httpOnly Cookie 做身份认证（credentials: 'include'）
 *
 * @param {Object} data - 请求体，包含 sessionId（兼容 conversationId）和 message
 * @param {Function} onToken - 收到 token 事件时回调，参数为文本片段（保留原始空白）
 * @param {Function} onMeta - 收到 meta 事件时回调，参数为解析后的 JSON 对象（含 conversationId）
 * @param {Function} onError - 请求失败、服务端 error 事件或流中断时回调
 * @returns {Promise} fetch 链式调用的 Promise
 */
export const chatStream = (data, onToken, onMeta, onError) => {
  return fetch('/api/chat/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
    // 携带 httpOnly Cookie（Access Token + Refresh Token），否则后端无法认证
    credentials: 'include'
  }).then(response => {
    if (!response.ok) {
      throw new Error('HTTP ' + response.status)
    }
    const contentType = response.headers.get('Content-Type') || ''
    if (contentType.indexOf('text/event-stream') === -1) {
      // 非 SSE 响应（如校验失败返回的 ResponseDTO JSON），按业务错误处理
      return response.text().then(text => {
        let message = ''
        try {
          message = JSON.parse(text).message || ''
        } catch (e) {
          message = ''
        }
        if (onError) onError(new Error(message || '问答服务返回了非预期响应'))
      })
    }
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    // 缓冲区：TCP 流式传输可能在一个 chunk 中间截断 SSE 帧，需要跨 chunk 拼接
    let buffer = ''

    // 当前帧的字段；SSE 帧以空行结束
    let eventType = ''
    const dataLines = []

    function dispatch () {
      if (dataLines.length > 0) {
        // 多行 data 以 \n 拼接（SSE 规范），且不做 trim——后端推的 token 可能就是一个空格
        const payload = dataLines.join('\n')
        if (eventType === 'token') {
          onToken(payload)
        } else if (eventType === 'meta') {
          try {
            onMeta(JSON.parse(payload))
          } catch (e) {
            onMeta({})
          }
        } else if (eventType === 'error') {
          onError(new Error(payload || '问答服务异常'))
        }
      }
      eventType = ''
      dataLines.length = 0
    }

    function handleLine (line) {
      if (line === '') {
        dispatch()
        return
      }
      if (line.indexOf(':') === 0) {
        return // SSE 注释行（心跳）
      }
      const colon = line.indexOf(':')
      const field = colon === -1 ? line : line.substring(0, colon)
      let value = colon === -1 ? '' : line.substring(colon + 1)
      // 规范规定：冒号后若紧跟一个空格，去掉这一个空格；其余空白原样保留
      if (value.charAt(0) === ' ') value = value.substring(1)
      if (field === 'event') {
        eventType = value
      } else if (field === 'data') {
        dataLines.push(value)
      }
    }

    function read () {
      return reader.read().then(({ done, value }) => {
        if (done) {
          // 处理残留的最后半帧（服务端未以空行收尾时），避免丢最后一段文本
          buffer += decoder.decode()
          if (buffer) {
            buffer.split('\n').forEach(handleLine)
            buffer = ''
          }
          dispatch()
          return
        }
        // stream: true 时解码器会保留不完整的多字节 UTF-8 字符，等下一个 chunk 拼接
        buffer += decoder.decode(value, { stream: true })

        const lines = buffer.split('\n')
        // 保留最后一个不完整行，等下一个 chunk 拼接
        buffer = lines.pop() || ''
        lines.forEach(handleLine)
        return read()
      }).catch(err => {
        if (onError) onError(err)
      })
    }
    return read()
  }).catch(err => {
    if (onError) onError(err)
  })
}

export const listConversations = () => {
  return request({
    url: url + '/conversations',
    method: 'get'
  })
}

export const queryConversation = (id) => {
  return request({
    url: url + '/conversations/' + id,
    method: 'get'
  })
}

export const queryMessages = (id, params) => {
  return request({
    url: url + '/conversations/' + id + '/messages',
    method: 'get',
    params
  })
}

export const deleteConversation = (id) => {
  return request({
    url: url + '/conversations/' + id,
    method: 'delete'
  })
}
