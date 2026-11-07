/** 文件任务模块 → 前端路由映射（路由由后端动态菜单生成） */
export const FILE_TASK_ROUTES = {
  ATTENDANCE: '/performance/attendance',
  SALARY: '/money/salary',
  STAFF_OVERTIME: '/performance/overtime'
}

/** 返回模块对应的路由，未知模块返回 null */
export function routeForModule (module) {
  return FILE_TASK_ROUTES[module] || null
}
