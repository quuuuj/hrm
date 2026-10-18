<template>
  <el-card class="task-card" shadow="never">
    <div slot="header" class="task-header">
      <span class="task-title">文件任务</span>
      <div class="task-actions">
        <el-button type="text" size="mini" :loading="loading" @click="$emit('refresh')">
          <i class="el-icon-refresh"></i> 刷新
        </el-button>
        <el-button type="text" size="mini" @click="collapsed = !collapsed">
          {{ collapsed ? '展开' : '收起' }}
          <i :class="collapsed ? 'el-icon-arrow-down' : 'el-icon-arrow-up'"></i>
        </el-button>
      </div>
    </div>
    <el-table
      v-show="!collapsed"
      v-loading="loading"
      :data="taskList"
      :row-class-name="rowClassName"
      :max-height="220"
      size="mini"
      stripe
      empty-text="暂无任务"
    >
      <el-table-column prop="id" label="任务ID" width="80" />
      <el-table-column label="类型" width="70">
        <template slot-scope="scope">{{ taskTypeLabel(scope.row.taskType) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template slot-scope="scope">
          <el-tag size="mini" :type="taskTagType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="进度" min-width="220">
        <template slot-scope="scope">
          <el-progress
            v-if="scope.row.totalCount > 0"
            :percentage="progressOf(scope.row)"
            :status="progressStatus(scope.row)"
            :stroke-width="12"
          />
          <span v-else class="task-muted">{{ statusLabel(scope.row.status) }}</span>
          <div class="task-count">
            已处理 {{ scope.row.processedCount || 0 }} / {{ scope.row.totalCount || '?' }}
            <span class="task-success">{{ scope.row.successCount || 0 }}</span>
            <span v-if="scope.row.failCount" class="task-fail">{{ scope.row.failCount }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="fileName" label="文件名" min-width="160" show-overflow-tooltip />
      <el-table-column prop="createTime" label="创建时间" width="160" />
      <el-table-column label="操作" width="150" fixed="right">
        <template slot-scope="scope">
          <el-button
            v-if="scope.row.taskType === 'EXPORT' && scope.row.resultFilePath"
            type="text"
            size="mini"
            @click="$emit('download', scope.row, 'RESULT')"
          >下载结果</el-button>
          <el-button
            v-if="scope.row.errorFilePath"
            type="text"
            size="mini"
            @click="$emit('download', scope.row, 'ERROR')"
          >下载错误</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script>
const STATUS_LABELS = {
  PENDING: '等待中',
  RUNNING: '处理中',
  SUCCESS: '成功',
  PARTIAL_SUCCESS: '部分成功',
  FAILED: '失败'
}

const STATUS_TAG_TYPES = {
  PENDING: 'info',
  RUNNING: 'warning',
  SUCCESS: 'success',
  PARTIAL_SUCCESS: 'warning',
  FAILED: 'danger'
}

export default {
  name: 'FileTaskCard',
  props: {
    /** 文件任务列表 */
    taskList: {
      type: Array,
      default: () => []
    },
    /** 列表加载状态 */
    loading: {
      type: Boolean,
      default: false
    },
    /** 从通知跳转而来时需要高亮的任务 id */
    highlightTaskId: {
      type: [String, Number],
      default: ''
    }
  },
  data () {
    return {
      collapsed: false
    }
  },
  methods: {
    taskTypeLabel (taskType) {
      return taskType === 'IMPORT' ? '导入' : taskType === 'EXPORT' ? '导出' : taskType || '-'
    },
    statusLabel (status) {
      return STATUS_LABELS[status] || status || '-'
    },
    taskTagType (status) {
      return STATUS_TAG_TYPES[status] || 'info'
    },
    progressOf (task) {
      if (!task.totalCount) return 0
      const percentage = Math.round((task.processedCount || 0) / task.totalCount * 100)
      return Math.min(100, percentage)
    },
    progressStatus (task) {
      if (task.status === 'FAILED') return 'exception'
      if (task.status === 'PARTIAL_SUCCESS') return 'warning'
      if (task.status === 'SUCCESS') return 'success'
      return undefined
    },
    rowClassName ({ row }) {
      if (this.highlightTaskId === '' || this.highlightTaskId == null) return ''
      return String(row.id) === String(this.highlightTaskId) ? 'task-row--highlight' : ''
    }
  }
}
</script>

<style lang="less" scoped>
.task-card {
  margin-bottom: 12px;
}

.task-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.task-title {
  font-weight: 600;
  font-size: 13px;
}

.task-actions {
  display: flex;
  align-items: center;

  .el-button + .el-button {
    margin-left: 8px;
  }
}

.task-muted {
  color: #909399;
}

.task-count {
  margin-top: 2px;
  font-size: 12px;
  color: #909399;
}

.task-success {
  margin-left: 6px;
  color: #67c23a;
}

.task-fail {
  margin-left: 6px;
  color: #f56c6c;
}

::v-deep .task-row--highlight > td {
  background-color: #ecf5ff !important;
}
</style>
