-- ============================================================
-- 系统配置管理: 全局参数键值对 + 管理员配置菜单
-- 依赖: 需在 schema.sql 之后执行
-- ============================================================

-- ---------------- 系统配置表 ----------------
CREATE TABLE IF NOT EXISTS sys_config (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
  config_key    VARCHAR(128) NOT NULL UNIQUE COMMENT '配置键',
  config_value  TEXT COMMENT '配置值',
  config_name   VARCHAR(128) NOT NULL COMMENT '配置名称(展示用)',
  description   VARCHAR(512) DEFAULT '' COMMENT '配置说明',
  config_type   VARCHAR(16) NOT NULL DEFAULT 'string' COMMENT '类型 string/number/boolean/textarea',
  sort          INT NOT NULL DEFAULT 0 COMMENT '排序',
  updated_by    BIGINT DEFAULT NULL COMMENT '最后修改人',
  updated_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- ---------------- 初始配置数据 ----------------
INSERT INTO sys_config (config_key, config_value, config_name, description, config_type, sort) VALUES
('system.name',           'PMS 项目管理系统', '系统名称',     '显示在登录页和侧边栏顶部的系统名称',       'string',   1),
('system.copyright',      '© 2026 PMS 毕业设计', '版权信息',  '页面底部显示的版权文字',                     'string',   2),
('system.icp',            '',                   'ICP备案号',   '页面底部显示的备案号, 可留空',              'string',   3),
('dashboard.welcome',     '欢迎使用 PMS 敏捷项目管理系统', '工作台欢迎语', '工作台顶部展示的欢迎文字',         'string',   4),
('upload.max-size-mb',    '10',                 '单文件大小限制(MB)', '附件上传时单个文件的最大体积',         'number',   10),
('upload.allowed-ext',    'doc,docx,pdf,xls,xlsx,ppt,pptx,txt,md,csv,png,jpg,jpeg,gif,webp,bmp,zip,rar,7z',
                           '允许的文件扩展名',   '逗号分隔, 不在列表中的扩展名将被拒绝上传',          'textarea', 11),
('jwt.expire-hours',      '12',                 '登录有效期(小时)', '用户登录后 Token 的有效时长',             'number',   20),
('user.default-password',  '123456',             '新用户默认密码', '管理员创建用户时未指定密码则使用此默认值', 'string',   21),
('session.auto-logout',    'true',               '异地登录自动下线', '同一账号在别处登录时, 原登录态是否失效',  'boolean',  30),
('log.retention-days',     '90',                 '操作日志保留天数', '超过此天数的操作日志可被清理',              'number',   31);

-- ---------------- 配置管理菜单(挂在系统管理 id=13 下) ----------------
INSERT INTO sys_permission (parent_id, perm_name, perm_type, perm_key, path, icon, sort, deleted)
VALUES (13, '系统配置', 2, 'system:config:list', '/system/config', 'Setting', 6, 0);

-- 管理员角色拥有全部权限, 无需额外分配; 此处确保 admin 角色包含新菜单
INSERT INTO sys_role_permission (role_id, perm_id)
SELECT 1, id FROM sys_permission WHERE perm_key = 'system:config:list' AND deleted = 0
AND NOT EXISTS (SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = 1 AND rp.perm_id = sys_permission.id);
