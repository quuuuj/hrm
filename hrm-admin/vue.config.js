module.exports = {
  lintOnSave: false, // 关闭语法检查
  devServer: {
    headers: {
      'Permissions-Policy': 'unload=*' // Chrome 新版禁止 unload 事件，SockJS 需要此权限
    },
    proxy: {
      '/api': {
        // target 为后端地址（如 http://localhost:8888），与 src 中的 VUE_APP_BASE_API 同源变量
        target: process.env.VUE_APP_BACKEND_TARGET,
        pathRewrite: { '^/api': '' },
        changeOrigin: true
      }
    }
  }
}
