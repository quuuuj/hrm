<template>
  <div style="display:inline-block">
    <!-- 隐藏的原生 file input，通过按钮触发点击 -->
    <input
      ref="fileInput"
      type="file"
      :accept="accept"
      style="display:none"
      @change="handleFileChange"
    >
    <el-button
      type="success"
      size="mini"
      :loading="uploading"
      :disabled="uploading"
      @click="$refs.fileInput.click()"
    >
      {{ uploading ? progressText : label }}
    </el-button>
  </div>
</template>

<script>
import request from '@/utils/request'
import { uploadInit, uploadChunk } from '@/api/fileTask'

/** 分片上传单块大小 5MB，与后端 MIN_CHUNK_SIZE 一致 */
const DEFAULT_CHUNK_SIZE = 5 * 1024 * 1024

export default {
  name: 'ChunkedImportBtn',
  props: {
    /** 分片上传范围：DOCS 走秒传，IMPORT 不秒传 */
    scope: { type: String, default: 'IMPORT' },
    /** 合并分片并落库的业务接口，如 /docs/upload-task 或 /attendance/import/task */
    completeApi: { type: String, required: true },
    /** 接受的文件类型，如 .xlsx,.pdf */
    accept: { type: String, default: '.xlsx' },
    /** 按钮文案 */
    label: { type: String, default: '导入' },
    /** DOCS 范围下是否加入知识库进行分块与向量化（默认 false） */
    ingest: { type: Boolean, default: false }
  },
  emits: ['success', 'error'],
  data () {
    return {
      uploading: false,
      progressText: '导入中...'
    }
  },
  methods: {
    /** 文件选择后的主流程：分片上传 → 调用业务接口合并落库 */
    async handleFileChange (e) {
      const file = e.target.files[0]
      if (!file) return
      this.uploading = true
      try {
        const uploadId = await this.doChunkedUpload(file)
        // 秒传命中：服务端已有同 hash 文件并完成登记，没有 uploadId 也没有新任务
        if (!uploadId) {
          this.$message.success('秒传成功，文件已存在')
          this.$emit('success')
          return
        }
        const res = await this.complete(uploadId)
        this.$message.success(res.message || '上传成功')
        this.$emit('success')
      } catch (err) {
        this.$message.error(err.message || '导入失败')
        this.$emit('error', err)
      } finally {
        this.uploading = false
        // 清空 input 值，允许重复上传同一文件
        this.$refs.fileInput.value = ''
      }
    },

    /** 分片上传协议：init → chunks，返回 uploadId；秒传命中返回 null */
    async doChunkedUpload (file) {
      // 阶段 1: 初始化上传会话
      const initRes = await uploadInit({
        scope: this.scope,
        fileName: file.name,
        fileExt: this.getExt(file.name),
        fileSize: file.size,
        fileHash: await this.sha256(file),
        chunkSize: DEFAULT_CHUNK_SIZE
      })
      if (initRes.code !== 200) throw new Error(initRes.message || '初始化上传失败')
      const initData = initRes.data || {}
      // 秒传：命中同 hash 文件，服务端已登记，不传分片也不合并
      if (initData.instantUpload) return null

      const uploadId = initData.uploadId
      // 切片大小与总分片数以服务端为准：断点续传必须沿用首传时的切片大小
      const chunkSize = initData.chunkSize || DEFAULT_CHUNK_SIZE
      const chunkCount = initData.chunkCount || Math.ceil(file.size / chunkSize)

      // 断点续传：已上传的分片不再重复上传
      const uploadedSet = new Set(initData.uploadedChunks || [])
      let uploadedCount = uploadedSet.size

      // 阶段 2: 逐片上传，跳过已完成的分片，实时更新进度文案
      for (let i = 0; i < chunkCount; i++) {
        if (uploadedSet.has(i)) continue
        const start = i * chunkSize
        const end = Math.min(start + chunkSize, file.size)
        const blob = file.slice(start, end) // 浏览器端切分，不占内存
        const form = new FormData()
        form.append('uploadId', uploadId)
        form.append('chunkIndex', i)
        form.append('chunkHash', await this.sha256Blob(blob))
        form.append('chunk', blob, `chunk-${i}`)
        const res = await uploadChunk(form)
        if (res.code !== 200) throw new Error(res.message || `分片 ${i} 上传失败`)
        uploadedCount++
        this.progressText = `上传 ${uploadedCount}/${chunkCount}`
      }
      return uploadId
    },

    /** 阶段 3: 合并分片并由业务接口落库、建任务 */
    async complete (uploadId) {
      const params = { uploadId }
      if (this.scope === 'DOCS') params.ingest = this.ingest
      const res = await request({
        url: this.completeApi,
        method: 'post',
        params
      })
      if (res.code !== 200) throw new Error(res.message || '合并分片失败')
      return res
    },

    /** 提取文件扩展名，不含点 */
    getExt (name) {
      const i = name.lastIndexOf('.')
      return i >= 0 ? name.substring(i + 1) : ''
    },

    /** 浏览器端计算文件 SHA-256，用于去重和完整性校验 */
    async sha256 (file) {
      const buf = await file.arrayBuffer()
      const hash = await crypto.subtle.digest('SHA-256', buf)
      return Array.from(new Uint8Array(hash))
        .map(b => b.toString(16).padStart(2, '0'))
        .join('')
    },

    /** 计算 Blob 的 SHA-256，用于分片完整性校验 */
    async sha256Blob (blob) {
      const buf = await blob.arrayBuffer()
      const hash = await crypto.subtle.digest('SHA-256', buf)
      return Array.from(new Uint8Array(hash))
        .map(b => b.toString(16).padStart(2, '0'))
        .join('')
    }
  }
}
</script>
