<template>
  <div class="chat-container">
    <!-- 左侧：历史会话列表（ChatGPT 风格边栏） -->
    <div class="chat-sidebar">
      <div class="sidebar-header">
        <el-button
          type="primary"
          icon="el-icon-plus"
          size="small"
          class="new-chat-btn"
          @click="startNewConversation"
        >
          新建对话
        </el-button>
      </div>

      <div class="conversation-list">
        <div v-if="conversations.length === 0" class="conversation-empty">
          暂无历史会话
        </div>
        <div
          v-for="item in conversations"
          :key="item.id"
          :class="['conversation-item', { active: conversationId === item.id }]"
          @click="handleConversationSelect(item.id)"
        >
          <i class="el-icon-chat-dot-round item-icon" />
          <span class="item-title" :title="item.title">{{ item.title || '新对话' }}</span>
          <i
            class="el-icon-delete item-delete"
            title="删除会话"
            @click.stop="removeConversation(item.id)"
          />
        </div>
      </div>
    </div>

    <!-- 右侧：对话区与底部输入框 -->
    <div class="chat-main">
      <!-- 顶部当前会话提示（可选/轻量） -->
      <div class="chat-header">
        <span class="header-title">{{ currentTitle }}</span>
      </div>

      <!-- 中间：对话框消息列表 -->
      <div ref="msgList" class="chat-messages" @scroll="onScroll">
        <el-skeleton v-if="loading" :rows="3" animated style="padding: 16px" />
        <div v-if="loadingMore" class="loading-more">加载中...</div>

        <div v-if="messages.length === 0 && !loading" class="chat-empty">
          <i class="el-icon-chat-dot-square empty-icon" />
          <h3>向智能问答提问</h3>
          <p>支持人事政策、薪资考勤制度与员工档案等业务咨询</p>
        </div>

        <div
          v-for="(msg, idx) in messages"
          :key="idx"
          :class="['chat-msg', msg.role === 'USER' ? 'user' : 'assistant']"
        >
          <div class="avatar-box">
            <i :class="msg.role === 'USER' ? 'el-icon-user-solid' : 'el-icon-service'" />
          </div>
          <div class="chat-bubble">
            <div class="chat-text" v-html="renderMessage(msg.content)" />
            <div v-if="msg.role !== 'USER' && msg.llmEnhanced === false" class="chat-meta">
              基础知识库检索
            </div>
          </div>
        </div>

        <!-- 流式输出打字机 -->
        <div v-if="streaming" class="chat-msg assistant">
          <div class="avatar-box">
            <i class="el-icon-service" />
          </div>
          <div class="chat-bubble">
            <div class="chat-text">
              {{ streamingText || '思考中...' }}
              <span class="chat-cursor">|</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 下方：输入框与操作栏 -->
      <div class="chat-input-wrapper">
        <div class="chat-input-card">
          <el-input
            v-model.trim="question"
            type="textarea"
            :rows="3"
            maxlength="1000"
            show-word-limit
            :disabled="streaming || loading || loadingMore"
            placeholder="输入你想咨询的人事制度或政策问题... (Enter 发送，Shift + Enter 换行)"
            @keydown.native.enter.exact.prevent="sendQuestion"
          />
          <div class="chat-input-actions">
            <span class="shortcut-tip">按 Enter 发送</span>
            <el-button
              type="primary"
              size="small"
              icon="el-icon-position"
              :loading="streaming"
              :disabled="!question"
              @click="sendQuestion"
            >
              发送
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { chatStream, deleteConversation, listConversations, queryMessages } from '@/api/chat'
import { marked } from 'marked'

marked.setOptions({
  breaks: true,
  gfm: true
})

export default {
  name: 'Chat',
  data () {
    return {
      loading: false, // 历史消息加载状态
      loadingMore: false, // 更早历史消息的分页加载状态
      conversationId: null, // 当前选中的会话 ID，null 表示新建会话
      conversations: [], // 历史会话列表
      messages: [], // 当前展示的消息列表
      hasMore: false, // 是否还有更早的历史消息可加载（游标分页）
      nextCursor: null, // 游标分页标记
      question: '', // 输入框内容
      streaming: false, // 当前回复的流式输出状态
      streamingText: '' // 流式输出中已接收的文本
    }
  },
  computed: {
    currentTitle () {
      if (!this.conversationId) return '新建对话'
      const found = this.conversations.find(c => c.id === this.conversationId)
      return found ? found.title : '智能问答'
    }
  },
  created () {
    this.loadConversations()
  },
  methods: {
    /** 加载历史会话列表 */
    loadConversations () {
      listConversations().then(response => {
        if (response.code === 200) {
          this.conversations = response.data || []
        }
      })
    },

    /** 切换选中历史会话 */
    handleConversationSelect (id) {
      if (this.conversationId === id || this.streaming) return
      this.conversationId = id
      this.loading = true
      queryMessages(id, { size: 10 }).then(response => {
        if (response.code === 200) {
          const data = response.data || {}
          this.messages = data.records || []
          this.hasMore = data.hasMore || false
          this.nextCursor = data.nextCursor || null
          this.scrollToBottom()
        } else {
          this.$message.error(response.message)
        }
      }).finally(() => {
        this.loading = false
      })
    },

    /** 重置为新会话状态，清空消息和输入 */
    startNewConversation () {
      if (this.streaming) return
      this.conversationId = null
      this.messages = []
      this.hasMore = false
      this.nextCursor = null
      this.question = ''
    },

    /** 加载更早的历史消息（向上滚动触发），插入后恢复滚动位置 */
    loadMore () {
      if (!this.hasMore || this.loadingMore || !this.conversationId) return
      this.loadingMore = true
      const prevHeight = this.$refs.msgList.scrollHeight
      queryMessages(this.conversationId, {
        size: 10,
        before: this.nextCursor
      }).then(response => {
        if (response.code === 200) {
          const data = response.data || {}
          this.messages.unshift(...(data.records || []))
          this.hasMore = data.hasMore || false
          this.nextCursor = data.nextCursor || null
          this.$nextTick(() => {
            this.$refs.msgList.scrollTop = this.$refs.msgList.scrollHeight - prevHeight
          })
        }
      }).finally(() => {
        this.loadingMore = false
      })
    },

    /** 滚动监听：距顶部 ≤20px 且还有历史消息时，自动加载更多 */
    onScroll () {
      const pane = this.$refs.msgList
      if (!pane || this.loadingMore) return
      if (pane.scrollTop <= 20 && this.hasMore) {
        this.loadMore()
      }
    },

    /** 删除单个会话 */
    removeConversation (id) {
      this.$confirm('确认删除该会话记录吗？', '提示', {
        type: 'warning',
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      }).then(() => {
        deleteConversation(id).then(response => {
          if (response.code === 200) {
            this.$message.success('会话已删除')
            if (this.conversationId === id) {
              this.startNewConversation()
            }
            this.loadConversations()
          } else {
            this.$message.error(response.message)
          }
        })
      }).catch(() => {})
    },

    /**
     * 发送消息，通过 SSE 流式接收回复。
     */
    sendQuestion () {
      if (!this.question || this.streaming) return
      const content = this.question
      this.messages.push({ role: 'USER', content })
      this.question = ''
      this.streaming = true
      this.streamingText = ''
      this.scrollToBottom()

      chatStream(
        { sessionId: this.conversationId, message: content },
        (token) => {
          this.streamingText += token
          this.scrollToBottom()
        },
        (meta) => {
          this.conversationId = meta.conversationId || this.conversationId
          // 重新拉取以同步服务端持久化与元数据
          this.handleConversationSelect(this.conversationId)
          this.loadConversations()
          this.streaming = false
          this.streamingText = ''
        },
        () => {
          if (!this.streamingText) {
            this.streamingText = '智能问答暂时不可用，请稍后再试。'
          }
          this.messages.push({ role: 'ASSISTANT', content: this.streamingText })
          this.streaming = false
          this.streamingText = ''
        }
      )
    },

    /** 滚动到消息底部 */
    scrollToBottom () {
      this.$nextTick(() => {
        const el = this.$refs.msgList
        if (el) el.scrollTop = el.scrollHeight
      })
    },

    /** Markdown 渲染 */
    renderMessage (content) {
      if (!content) return ''
      return marked.parse(content)
    }
  }
}
</script>

<style scoped>
.chat-container {
  height: calc(100vh - 110px);
  display: flex;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  margin-top: 10px;
}

/* 左侧边栏 */
.chat-sidebar {
  width: 260px;
  border-right: 1px solid #ebeef5;
  background-color: #f7f8fa;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid #ebeef5;
}

.new-chat-btn {
  width: 100%;
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.conversation-empty {
  text-align: center;
  color: #909399;
  font-size: 13px;
  padding-top: 40px;
}

.conversation-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  border-radius: 6px;
  margin-bottom: 4px;
  cursor: pointer;
  color: #606266;
  font-size: 14px;
  transition: all 0.2s;
}

.conversation-item:hover {
  background-color: #ebedf0;
}

.conversation-item.active {
  background-color: #e6f1fc;
  color: #409eff;
  font-weight: 500;
}

.item-icon {
  margin-right: 8px;
  font-size: 16px;
}

.item-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-delete {
  margin-left: 6px;
  font-size: 14px;
  color: #909399;
  display: none;
}

.conversation-item:hover .item-delete {
  display: inline-block;
}

.item-delete:hover {
  color: #f56c6c;
}

/* 右侧主区域 */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #fff;
}

.chat-header {
  height: 48px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  border-bottom: 1px solid #ebeef5;
  font-size: 15px;
  font-weight: 500;
  color: #303133;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px 32px;
}

.loading-more {
  text-align: center;
  padding: 8px;
  color: #909399;
  font-size: 12px;
}

.chat-empty {
  text-align: center;
  padding-top: 100px;
  color: #909399;
}

.empty-icon {
  font-size: 56px;
  color: #dcdfe6;
  margin-bottom: 12px;
}

.chat-empty h3 {
  font-size: 18px;
  color: #303133;
  margin-bottom: 8px;
}

.chat-empty p {
  font-size: 14px;
  color: #909399;
}

.chat-msg {
  display: flex;
  margin-bottom: 24px;
  gap: 12px;
}

.avatar-box {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.chat-msg.user {
  flex-direction: row-reverse;
}

.chat-msg.user .avatar-box {
  background-color: #409eff;
  color: #fff;
}

.chat-msg.assistant .avatar-box {
  background-color: #eef5fe;
  color: #409eff;
}

.chat-bubble {
  max-width: 72%;
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.chat-msg.user .chat-bubble {
  background: #409eff;
  color: #fff;
  border-top-right-radius: 2px;
}

.chat-msg.assistant .chat-bubble {
  background: #f4f6f8;
  color: #303133;
  border: 1px solid #e4e7ed;
  border-top-left-radius: 2px;
}

.chat-cursor {
  animation: blink 1s infinite;
  margin-left: 2px;
  font-weight: bold;
}

@keyframes blink {
  50% {
    opacity: 0;
  }
}

.chat-meta {
  margin-top: 6px;
  color: #909399;
  font-size: 12px;
}

/* 底部输入框 */
.chat-input-wrapper {
  padding: 16px 32px 20px;
  border-top: 1px solid #ebeef5;
  background: #fff;
}

.chat-input-card {
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 8px 12px;
  background: #fff;
  transition: border-color 0.2s;
}

.chat-input-card:focus-within {
  border-color: #409eff;
}

.chat-input-card /deep/ .el-textarea__inner {
  border: none;
  padding: 0;
  resize: none;
  font-size: 14px;
}

.chat-input-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-top: 8px;
  gap: 12px;
}

.shortcut-tip {
  font-size: 12px;
  color: #909399;
}
</style>
