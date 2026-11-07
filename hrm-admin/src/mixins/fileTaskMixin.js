/**
 * 文件任务混入——提供异步导入导出的任务列表与 SSE 进度更新。
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
        this.loadTasks(this.fileTaskModule)
        if (typeof this.onTaskCompleted === 'function') {
          this.onTaskCompleted(task)
        }
      }
      this.taskStatusSnapshot[task.id] = task.status
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
