/**
 * 文件任务混入——提供异步导入导出的任务列表、SSE 进度更新与完成通知。
 * 使用页面通过 data 覆盖 fileTaskModule（如 'ATTENDANCE'），并实现 onTaskCompleted(task) 刷新表格。
 *
 * @mixin fileTaskMixin
 */
import { list as listFileTask, download as downloadFileTask } from '@/api/fileTask'
import { on, off } from '@/utils/sse'
import { write } from '@/utils/docs'

const TERMINAL_STATUSES = ['SUCCESS', 'PARTIAL_SUCCESS', 'FAILED']

export default {
  data () {
    return {
      taskList: [],
      taskStatusSnapshot: {},
      taskLoading: false,
      fileTaskModule: ''
    }
  },
  created () {
    if (!this.fileTaskModule) return
    this.onTaskUpdate = task => this.handleSseUpdate(task)
    this.onSseConnected = () => this.loadTasks(this.fileTaskModule)
    on('task-update', this.onTaskUpdate)
    on('connected', this.onSseConnected)
    this.loadTasks(this.fileTaskModule)
  },
  beforeDestroy () {
    if (this.onTaskUpdate) off('task-update', this.onTaskUpdate)
    if (this.onSseConnected) off('connected', this.onSseConnected)
  },
  methods: {
    /**
     * 加载当前模块的文件任务列表
     * @param {string} module - 模块标识，如 'ATTENDANCE'、'STAFF'
     */
    loadTasks (module) {
      const targetModule = module || this.fileTaskModule
      if (!targetModule) return
      this.taskLoading = true
      listFileTask({
        current: 1,
        size: 10,
        taskType: '',
        module: targetModule
      }).then(response => {
        if (response.code === 200) {
          const taskList = (response.data && response.data.list) || []
          this.taskList = taskList
          if (Object.keys(this.taskStatusSnapshot).length === 0) {
            this.initSnapshot(taskList)
          }
        }
      }).finally(() => {
        this.taskLoading = false
      })
    },

    initSnapshot (taskList) {
      taskList.forEach(task => {
        this.taskStatusSnapshot[task.id] = task.status
      })
    },

    handleSseUpdate (task) {
      if (!task || task.module !== this.fileTaskModule) return

      const index = this.taskList.findIndex(item => item.id === task.id)
      if (index >= 0) {
        this.$set(this.taskList, index, task)
      } else {
        this.taskList.unshift(task)
        if (this.taskList.length > 10) this.taskList.pop()
      }

      const previous = this.taskStatusSnapshot[task.id]
      if (previous !== task.status && TERMINAL_STATUSES.includes(task.status)) {
        this.notifyTaskComplete(task)
        this.loadTasks(this.fileTaskModule)
        if (typeof this.onTaskCompleted === 'function') {
          this.onTaskCompleted(task)
        }
      }
      this.taskStatusSnapshot[task.id] = task.status
    },

    /** 任务完成时的页面通知，子组件可覆写 */
    notifyTaskComplete (task) {
      const typeLabel = task.taskType === 'IMPORT' ? '导入' : '导出'
      const statusLabel = {
        SUCCESS: '成功',
        PARTIAL_SUCCESS: '部分成功',
        FAILED: '失败'
      }[task.status] || task.status
      const title = `${typeLabel}任务${statusLabel}`
      const body = task.status === 'SUCCESS'
        ? `${task.fileName} 共 ${task.totalCount || 0} 条全部处理成功`
        : task.status === 'PARTIAL_SUCCESS'
          ? `${task.fileName} 成功 ${task.successCount || 0} 条，失败 ${task.failCount || 0} 条`
          : `${task.fileName} 处理失败：${task.failReason || '未知原因'}`
      this.showInAppNotification(task, title, body)
    },

    showInAppNotification (task, title, body) {
      const h = this.$createElement
      const actions = []
      if (task.status === 'PARTIAL_SUCCESS' && task.errorFilePath) {
        actions.push(h('el-button', {
          props: { type: 'text', size: 'mini' },
          on: { click: () => this.downloadTask(task, 'ERROR') }
        }, '下载错误文件'))
      }
      if (task.taskType === 'EXPORT' && task.resultFilePath) {
        actions.push(h('el-button', {
          props: { type: 'text', size: 'mini' },
          on: { click: () => this.downloadTask(task, 'RESULT') }
        }, '下载结果'))
      }
      this.$notify({
        title,
        message: h('div', null, [
          h('p', { style: { marginBottom: '8px' } }, body),
          ...actions
        ]),
        type: task.status === 'SUCCESS' ? 'success' : task.status === 'FAILED' ? 'error' : 'warning',
        duration: task.status === 'FAILED' ? 0 : 10000
      })
    },

    /** 下载任务文件 */
    downloadTask (task, fileType) {
      downloadFileTask(task.id, fileType).then(response => {
        const filename = fileType === 'ERROR' ? 'import-errors.xlsx' : task.fileName
        write(response, filename)
      }).catch(() => {
        // 网络/服务端错误已由 request 拦截器统一提示，此处仅防止未捕获的 rejection
      })
    }
  }
}
