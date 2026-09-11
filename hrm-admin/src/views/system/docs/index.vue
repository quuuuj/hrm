<template>
  <div class="manage">
    <el-dialog
      title="编辑"
      :visible.sync="editForm.isShow"
    >
      <el-form label-width="100px" :model="editForm.formData" size="mini">
        <el-form-item label="备注" label-width="140px" style="width:450px" prop="remark">
          <el-input type="textarea"
                    placeholder="请输入"
                    v-model.trim="editForm.formData.remark"
                    :autosize="{ minRows: 2, maxRows: 4}"
                    maxlength="100"
                    show-word-limit/>
        </el-form-item>
      </el-form>

      <div slot="footer" class="dialog-footer">
        <el-button @click="editForm.isShow = false">取消</el-button>
        <el-button type="primary" @click="confirmEdit">确定</el-button>
      </div>
    </el-dialog>

    <div style="margin-bottom: 10px">
      <!-- 通用Excel导入 -->
      <el-upload v-permission="['system:docs:import']" :action="importApi" :headers="headers" accept="xlsx" :show-file-list="false" :multiple="false"
                 :on-success="handleImportSuccess"
                 style="display:inline-block;">
        <el-button type="success" size="mini">
          导入 <i class="el-icon-bottom"></i>
        </el-button>
      </el-upload>
      <el-button v-permission="['system:docs:export']" type="warning" size="mini" @click="handleExport" style="margin-left: 10px">
        导出 <i class="el-icon-top"></i>
      </el-button>
      <!-- 大文件/普通文件分片上传 -->
      <ChunkedImportBtn
        v-permission="['system:docs:upload']"
        :import-api="chunkedUploadApi"
        label="上传文件"
        accept="*/*"
        :ingest="false"
        style="margin-left: 10px"
        @success="handleUploadSuccess"
      />
      <!-- 知识库摄入文档上传（自动触发分块与向量化） -->
      <ChunkedImportBtn
        v-permission="['system:docs:upload']"
        :import-api="chunkedUploadApi"
        label="上传知识文档"
        accept=".pdf,.docx,.md,.txt"
        :ingest="true"
        style="margin-left: 10px"
        @success="handleUploadSuccess"
      />
      <el-popconfirm
        style="margin-left: 10px"
        confirm-button-text="确定"
        cancel-button-text="我再想想"
        icon="el-icon-info"
        icon-color="red"
        title="你确定删除吗？"
        @confirm="handleDeleteBatch"
      >
        <el-button v-permission="['system:docs:delete']" type="danger" size="mini" slot="reference">
          批量删除 <i class="el-icon-remove-outline"></i>
        </el-button>
      </el-popconfirm>
    </div>

    <!------------- 搜索 ---------------------->
    <div class="manage-header">
      <el-form label-width="auto" :model="searchForm.formData"
               :inline="true" size="mini">
        <el-form-item label="原名称" prop="oldName">
          <el-input
            placeholder="请输入文件原名称"
            v-model.trim="searchForm.formData.oldName"
            prefix-icon="el-icon-search"
          />
        </el-form-item>
        <el-form-item label="上传者" prop="staffName">
          <el-input
            placeholder="请输入上传者"
            v-model.trim="searchForm.formData.staffName"
            prefix-icon="el-icon-search"
          />
        </el-form-item>
        <el-form-item>
          <el-button v-permission="['system:docs:search']" type="primary" @click="search" size="mini">搜索 <i class="el-icon-search"/></el-button>
          <el-button type="danger" @click="reset" size="mini">重置 <i class="el-icon-refresh-left"/></el-button>
        </el-form-item>
      </el-form>
    </div>
    <!------------------ 数据表格 ---------------->
    <div class="common-table">
      <el-table
        ref="table"
        :data="table.tableData"
        height="85%"
        border
        stripe
        row-key="id"
        fit
        :header-cell-style="{background: '#eef1f6',color: '#606266',
        textAlign:'center',fontWeight:'bold',borderWidth:'3px'}"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" align="center"/>
        <el-table-column prop="oldName" label="文件名称" min-width="220" align="center" fixed/>
        <el-table-column prop="type" label="类型" min-width="80" align="center"/>
        <el-table-column prop="size" label="大小(KB)" min-width="90" align="center"/>
        <el-table-column label="知识库状态" min-width="120" align="center">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.kbStatus" :type="statusTagType(scope.row.kbStatus)">
              {{ scope.row.kbStatus }}
            </el-tag>
            <span v-else style="color:#909399">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="chunkCount" label="分块数" min-width="80" align="center">
          <template slot-scope="scope">
            {{ scope.row.chunkCount != null ? scope.row.chunkCount : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="staffName" label="上传者" min-width="100" align="center"/>
        <el-table-column prop="createTime" label="上传时间" min-width="150" align="center"/>
        <el-table-column prop="remark" label="备注" min-width="150" align="center"/>
        <el-table-column label="操作" width="320" fixed="right" align="center">
          <template slot-scope="scope">
            <el-button
              v-if="scope.row.kbStatus"
              size="mini"
              type="info"
              @click="handleViewChunks(scope.row)"
            >分块</el-button>
            <el-button
              v-if="scope.row.kbStatus === 'FAILED'"
              v-permission="['system:docs:upload']"
              size="mini"
              type="warning"
              @click="handleRetry(scope.row)"
            >重试</el-button>
            <el-button v-permission="['system:docs:edit']" size="mini" type="primary" @click="handleEdit(scope.row)">
              编辑
            </el-button>
            <el-popconfirm
              style="margin-left:10px;margin-right:10px"
              confirm-button-text="确定"
              cancel-button-text="我再想想"
              icon="el-icon-info"
              icon-color="red"
              title="你确定删除吗？"
              @confirm="handleDelete(scope.row.id)">
              <el-button v-permission="['system:docs:delete']" size="mini" type="danger" slot="reference">
                删除
              </el-button>
            </el-popconfirm>
            <el-button v-permission="['system:docs:download']" size="mini" type="warning" @click="handleDownload(scope.row)">
              下载
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        class="pager"
        layout="total,sizes,prev,pager,next,jumper"
        :page-size="table.pageConfig.size ? table.pageConfig.size : 10"
        :page-sizes="[5, 10, 15, 20]"
        :total="table.pageConfig.total"
        :current-page.sync="table.pageConfig.current"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      ></el-pagination>
    </div>

    <!-- 文档分块弹窗 -->
    <el-dialog title="文档分块预览" :visible.sync="chunkDialog.isShow" width="60%">
      <div v-loading="chunkDialog.loading">
        <div v-for="(chunk, idx) in chunkDialog.list" :key="idx" style="margin-bottom:12px;padding:8px;background:#f5f7fa;border-radius:4px">
          <el-tag size="mini" type="info">#{{ chunk.chunkIndex }}</el-tag>
          <span style="font-size:12px;color:#909399;margin-left:8px">{{ chunk.tokenCount }} tokens</span>
          <div style="margin-top:4px;font-size:13px;white-space:pre-wrap">{{ chunk.chunkText }}</div>
        </div>
        <div v-if="!chunkDialog.loading && chunkDialog.list.length === 0" style="text-align:center;color:#909399">暂无分块数据</div>
      </div>
    </el-dialog>
  </div>
</template>
<script>
import {
  deleteBatch,
  del, download,
  edit, exp,
  getImportApi,
  list,
  retry,
  chunks,
  getImportTaskApi
} from '@/api/docs'
import ChunkedImportBtn from '@/components/ChunkedImportBtn'
import { mapGetters } from 'vuex'
import { write } from '@/utils/docs'

export default {
  name: 'Docs',
  components: { ChunkedImportBtn },
  data () {
    return {
      editForm: {
        isShow: false,
        formData: {}
      },
      searchForm: {
        formData: {}
      },
      table: {
        tableData: [],
        pageConfig: {
          total: 10, // 记录总数
          current: 1, // 起始页
          size: 10 // 每页展示的记录数
        }
      },
      ids: [],
      chunkDialog: {
        isShow: false,
        loading: false,
        list: []
      }
    }
  },
  computed: {
    ...mapGetters(['staff', 'token']),
    headers () {
      return this.token ? { Authorization: 'Bearer ' + this.token } : {}
    },
    importApi () {
      return getImportApi()
    },
    chunkedUploadApi () {
      return getImportTaskApi()
    }
  },
  watch: {
    'table.tableData': function () {
      this.doLayout()
    }
  },
  beforeDestroy () {
    this.stopPolling()
  },
  methods: {
    statusTagType (status) {
      if (status === 'READY') return 'success'
      if (status === 'PROCESSING') return 'warning'
      if (status === 'FAILED') return 'danger'
      return 'info'
    },
    handleExport () {
      const filename = '文件信息表'
      exp(filename).then(response => {
        write(response, filename + '.xlsx')
      })
    },
    handleDownload (row) {
      download(row.name).then(response => {
        write(response, row.oldName)
      })
    },
    doLayout () {
      this.$nextTick(() => {
        if (this.$refs.table) {
          this.$refs.table.doLayout()
        }
      })
    },
    handleDelete (id) {
      del(id).then(
        response => {
          if (response.code === 200) {
            this.$message.success('删除成功！')
            this.loading()
          } else {
            this.$message.error('删除失败！')
          }
        }
      )
    },
    handleDeleteBatch () {
      deleteBatch(this.ids).then(response => {
        if (response.code === 200) {
          this.$message.success('批量删除成功！')
          this.loading()
        } else {
          this.$message.error('批量删除失败！')
        }
      })
    },
    handleEdit (row) {
      this.editForm.isShow = true
      this.editForm.formData = Object.assign({}, row)
    },
    confirmEdit () {
      edit(this.editForm.formData).then((response) => {
        if (response.code === 200) {
          this.$message.success('修改成功！')
          this.editForm.isShow = false
          this.loading()
        } else {
          this.$message.error('修改失败！')
        }
      })
    },
    search () {
      this.loading()
    },
    reset () {
      this.searchForm.formData = {}
      this.loading()
    },
    handleSizeChange (size) {
      this.table.pageConfig.size = size
      this.loading()
    },
    handleCurrentChange (current) {
      this.table.pageConfig.current = current
      this.loading()
    },
    handleSelectionChange (list) {
      this.ids = list.map(item => item.id)
    },
    loading () {
      list({
        current: this.table.pageConfig.current,
        size: this.table.pageConfig.size,
        oldName: this.searchForm.formData.oldName,
        staffName: this.searchForm.formData.staffName
      }).then(response => {
        if (response.code === 200) {
          this.table.tableData = response.data.list
          this.table.pageConfig.total = response.data.total
          if (this.table.tableData.some(row => row.kbStatus === 'PROCESSING' || row.kbStatus === 'UPLOADED')) {
            this.startPolling()
          } else {
            this.stopPolling()
          }
        } else {
          this.$message.error(response.message)
        }
      })
    },
    startPolling () {
      this.stopPolling()
      this._pollTimer = setInterval(() => {
        const hasPending = this.table.tableData.some(row => row.kbStatus === 'PROCESSING' || row.kbStatus === 'UPLOADED')
        if (!hasPending) {
          this.stopPolling()
          return
        }
        list({
          current: this.table.pageConfig.current,
          size: this.table.pageConfig.size,
          oldName: this.searchForm.formData.oldName,
          staffName: this.searchForm.formData.staffName
        }).then(response => {
          if (response.code === 200) {
            this.table.tableData = response.data.list
            this.table.pageConfig.total = response.data.total
            if (!this.table.tableData.some(row => row.kbStatus === 'PROCESSING' || row.kbStatus === 'UPLOADED')) {
              this.stopPolling()
            }
          }
        })
      }, 3000)
    },
    stopPolling () {
      if (this._pollTimer) {
        clearInterval(this._pollTimer)
        this._pollTimer = null
      }
    },
    handleImportSuccess (response) {
      if (response.code === 200) {
        this.$message.success('数据导入成功！')
        this.loading()
      } else {
        this.$message.error('数据导入失败！')
      }
    },
    handleUploadSuccess () {
      this.loading()
      this.$message.success('上传成功！')
      this.startPolling()
    },
    handleViewChunks (row) {
      this.chunkDialog.isShow = true
      this.chunkDialog.loading = true
      this.chunkDialog.list = []
      chunks(row.id).then(response => {
        if (response.code === 200) {
          this.chunkDialog.list = response.data || []
        }
      }).finally(() => {
        this.chunkDialog.loading = false
      })
    },
    handleRetry (row) {
      this.$confirm('确定重新处理该文档？', '提示', { type: 'warning' }).then(() => {
        retry(row.id).then(response => {
          if (response.code === 200) {
            this.$message.success('已提交重新处理')
            this.loading()
            this.startPolling()
          } else {
            this.$message.error(response.message || '操作失败')
          }
        })
      }).catch(() => {})
    }
  },
  created () {
    this.loading()
  }
}
</script>
<style scoped>
.manage {
  height: 100%;
}
.manage-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}
.common-table {
  position: relative;
  height: calc(100% - 130px);
}
.pager {
  position: absolute;
  bottom: 0;
  right: 20px;
}
</style>
