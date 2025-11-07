-- =============================================
-- 权限初始化数据（按模块分组）
-- =============================================

-- 1. 用户管理模块权限
INSERT INTO permissions (name, description) VALUES
-- 用户基础操作
('USER_VIEW', '查看用户列表'),
('USER_DETAIL', '查看用户详情'),
('USER_ADD', '添加用户'),
('USER_EDIT', '编辑用户'),
('USER_DELETE', '删除用户'),
('USER_BATCH_DELETE', '批量删除用户'),
-- 用户状态管理
('USER_ENABLE', '启用用户'),
('USER_DISABLE', '禁用用户'),
('USER_LOCK', '锁定用户'),
('USER_UNLOCK', '解锁用户'),
-- 用户数据操作
('USER_RESET_PWD', '重置密码'),
('USER_EXPORT', '导出用户'),
('USER_IMPORT', '导入用户'),
('USER_DOWNLOAD_TEMPLATE', '下载用户模板');

-- 2. 角色管理模块权限
INSERT INTO permissions (name, description) VALUES
-- 角色基础操作
('ROLE_VIEW', '查看角色列表'),
('ROLE_DETAIL', '查看角色详情'),
('ROLE_ADD', '添加角色'),
('ROLE_EDIT', '编辑角色'),
('ROLE_DELETE', '删除角色'),
-- 角色权限管理
('ROLE_PERMISSION_VIEW', '查看角色权限'),
('ROLE_PERMISSION_ASSIGN', '分配角色权限'),
('ROLE_PERMISSION_REMOVE', '移除角色权限'),
-- 角色用户管理
('ROLE_USER_VIEW', '查看角色用户'),
('ROLE_USER_ASSIGN', '分配角色用户'),
('ROLE_USER_REMOVE', '移除角色用户');

-- 3. 权限管理模块权限
INSERT INTO permissions (name, description) VALUES
-- 权限基础操作
('PERMISSION_VIEW', '查看权限列表'),
('PERMISSION_DETAIL', '查看权限详情'),
('PERMISSION_ADD', '添加权限'),
('PERMISSION_EDIT', '编辑权限'),
('PERMISSION_DELETE', '删除权限'),
-- 权限分类管理
('PERMISSION_CATEGORY_VIEW', '查看权限分类'),
('PERMISSION_CATEGORY_MANAGE', '管理权限分类');

-- 4. 系统管理模块权限
INSERT INTO permissions (name, description) VALUES
-- 系统配置
('SYS_CONFIG_VIEW', '查看系统配置'),
('SYS_CONFIG_EDIT', '编辑系统配置'),
('SYS_CONFIG_BACKUP', '备份系统配置'),
('SYS_CONFIG_RESTORE', '恢复系统配置'),
-- 系统监控
('SYS_MONITOR_VIEW', '查看系统监控'),
('SYS_MONITOR_STATS', '查看系统统计'),
('SYS_MONITOR_LOG', '查看系统日志'),
-- 数据管理
('SYS_DATA_BACKUP', '数据备份'),
('SYS_DATA_RESTORE', '数据恢复'),
('SYS_DATA_CLEAN', '数据清理');

-- 5. 个人中心权限
INSERT INTO permissions (name, description) VALUES
                                                ('PROFILE_VIEW', '查看个人资料'),
                                                ('PROFILE_EDIT', '编辑个人资料'),
                                                ('PROFILE_AVATAR_UPLOAD', '上传头像'),
                                                ('PROFILE_PWD_CHANGE', '修改密码'),
                                                ('PROFILE_SECURITY_SETTINGS', '安全设置');

-- 6. 审计日志权限
INSERT INTO permissions (name, description) VALUES
                                                ('AUDIT_LOG_VIEW', '查看审计日志'),
                                                ('AUDIT_LOG_EXPORT', '导出审计日志'),
                                                ('AUDIT_LOG_CLEAN', '清理审计日志'),
                                                ('AUDIT_LOG_ANALYSIS', '日志分析');

-- 验证数据
SELECT
    '模块化权限初始化完成' as result,
    COUNT(*) as total_permissions
FROM permissions;