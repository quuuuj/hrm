-- V7: 新增"智能问答"一级菜单，替代原悬浮问答抽屉入口
INSERT INTO `per_menu` (`id`, `code`, `name`, `icon`, `permission`, `parent_id`, `level`, `status`)
VALUES (100, 'chat', '智能问答', 'chat-dot-round', NULL, 0, 0, 1);

-- 授权给管理员角色
INSERT INTO `per_role_menu` (`role_id`, `menu_id`) VALUES (1, 100);
