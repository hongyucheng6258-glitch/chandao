-- =============================================================
-- 敏捷项目管理系统 PMS 数据库脚本 (MySQL 8.0, utf8mb4)
-- 参考禅道核心业务设计, 自研精简版
-- 注意: admin 用户由后端 DataInitializer 首次启动时自动创建 (admin/123456)
-- =============================================================
CREATE DATABASE IF NOT EXISTS pms DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE pms;

SET FOREIGN_KEY_CHECKS = 0;

-- ---------------- 系统域 ----------------
DROP TABLE IF EXISTS sys_dept;
CREATE TABLE sys_dept (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '部门ID',
  parent_id   BIGINT NOT NULL DEFAULT 0 COMMENT '父部门ID, 0为根',
  dept_name   VARCHAR(64) NOT NULL COMMENT '部门名称',
  sort        INT NOT NULL DEFAULT 0 COMMENT '排序',
  deleted     TINYINT NOT NULL DEFAULT 0,
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '部门表';

DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  username    VARCHAR(32) NOT NULL UNIQUE COMMENT '登录账号',
  password    VARCHAR(100) NOT NULL COMMENT '密码(BCrypt)',
  real_name   VARCHAR(32) NOT NULL COMMENT '姓名',
  email       VARCHAR(64) DEFAULT '' COMMENT '邮箱',
  avatar      VARCHAR(255) DEFAULT '' COMMENT '头像URL',
  dept_id     BIGINT DEFAULT NULL COMMENT '部门ID',
  status      TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1正常 0停用',
  deleted     TINYINT NOT NULL DEFAULT 0,
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '用户表';

DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_code   VARCHAR(32) NOT NULL UNIQUE COMMENT '角色编码',
  role_name   VARCHAR(32) NOT NULL COMMENT '角色名称',
  remark      VARCHAR(255) DEFAULT '',
  deleted     TINYINT NOT NULL DEFAULT 0,
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '角色表';

DROP TABLE IF EXISTS sys_permission;
CREATE TABLE sys_permission (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  parent_id   BIGINT NOT NULL DEFAULT 0 COMMENT '父权限ID',
  perm_name   VARCHAR(64) NOT NULL COMMENT '名称',
  perm_type   TINYINT NOT NULL COMMENT '类型 1目录 2菜单 3按钮',
  perm_key    VARCHAR(128) DEFAULT '' COMMENT '权限标识 如 task:assign',
  path        VARCHAR(128) DEFAULT '' COMMENT '前端路由路径',
  icon        VARCHAR(64) DEFAULT '',
  sort        INT NOT NULL DEFAULT 0,
  deleted     TINYINT NOT NULL DEFAULT 0
) COMMENT '权限/菜单表';

DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
  id      BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  UNIQUE KEY uk_user_role (user_id, role_id)
) COMMENT '用户-角色关联';

DROP TABLE IF EXISTS sys_role_permission;
CREATE TABLE sys_role_permission (
  id      BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_id BIGINT NOT NULL,
  perm_id BIGINT NOT NULL,
  UNIQUE KEY uk_role_perm (role_id, perm_id)
) COMMENT '角色-权限关联';

DROP TABLE IF EXISTS sys_action_log;
CREATE TABLE sys_action_log (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  object_type VARCHAR(32) NOT NULL COMMENT '对象类型 story/task/bug/...',
  object_id   BIGINT NOT NULL COMMENT '对象ID',
  action      VARCHAR(64) NOT NULL COMMENT '动作 如 创建/指派/解决',
  actor_id    BIGINT NOT NULL COMMENT '操作人',
  actor_name  VARCHAR(32) NOT NULL DEFAULT '',
  detail      TEXT COMMENT '变更明细 JSON',
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_object (object_type, object_id),
  KEY idx_actor (actor_id)
) COMMENT '操作日志/动态';

DROP TABLE IF EXISTS sys_notice;
CREATE TABLE sys_notice (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id     BIGINT NOT NULL COMMENT '接收人',
  title       VARCHAR(128) NOT NULL,
  content     VARCHAR(512) DEFAULT '',
  biz_type    VARCHAR(32) DEFAULT '',
  biz_id      BIGINT DEFAULT NULL,
  read_flag   TINYINT NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_user_read (user_id, read_flag)
) COMMENT '站内通知';

-- ---------------- 产品域 ----------------
DROP TABLE IF EXISTS product;
CREATE TABLE product (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  name        VARCHAR(64) NOT NULL COMMENT '产品名称',
  code        VARCHAR(32) NOT NULL DEFAULT '' COMMENT '产品代号',
  owner_id    BIGINT DEFAULT NULL COMMENT '产品负责人',
  description TEXT,
  status      VARCHAR(16) NOT NULL DEFAULT 'normal' COMMENT 'normal正常 closed已关闭',
  deleted     TINYINT NOT NULL DEFAULT 0,
  created_by  BIGINT DEFAULT NULL,
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '产品表';

DROP TABLE IF EXISTS product_plan;
CREATE TABLE product_plan (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id  BIGINT NOT NULL,
  title       VARCHAR(64) NOT NULL COMMENT '计划名称',
  begin_date  DATE DEFAULT NULL,
  end_date    DATE DEFAULT NULL,
  goal        TEXT COMMENT '目标描述',
  deleted     TINYINT NOT NULL DEFAULT 0,
  created_by  BIGINT DEFAULT NULL,
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_product (product_id)
) COMMENT '产品计划';

DROP TABLE IF EXISTS story;
CREATE TABLE story (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id  BIGINT NOT NULL,
  plan_id     BIGINT DEFAULT NULL COMMENT '所属计划',
  title       VARCHAR(128) NOT NULL COMMENT '需求标题',
  description TEXT COMMENT '需求描述',
  priority    TINYINT NOT NULL DEFAULT 3 COMMENT '优先级 1-4, 1最高',
  estimate    DECIMAL(8,1) DEFAULT NULL COMMENT '预估工时(小时)',
  status      VARCHAR(16) NOT NULL DEFAULT 'draft' COMMENT 'draft草稿 active已激活 changed已变更 closed已关闭',
  stage       VARCHAR(16) NOT NULL DEFAULT 'wait' COMMENT '阶段 wait未开始 developing研发中 testing测试中 released已发布',
  assigned_to BIGINT DEFAULT NULL COMMENT '当前指派给',
  closed_reason VARCHAR(32) DEFAULT '' COMMENT '关闭原因 done/duplicate/cancelled...',
  deleted     TINYINT NOT NULL DEFAULT 0,
  created_by  BIGINT DEFAULT NULL,
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_product_status (product_id, status),
  KEY idx_assigned (assigned_to, status)
) COMMENT '需求表';

-- ---------------- 项目域 ----------------
DROP TABLE IF EXISTS project;
CREATE TABLE project (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  name        VARCHAR(64) NOT NULL COMMENT '项目名称',
  code        VARCHAR(32) NOT NULL DEFAULT '',
  product_id  BIGINT DEFAULT NULL COMMENT '关联产品',
  owner_id    BIGINT DEFAULT NULL COMMENT '项目负责人',
  begin_date  DATE DEFAULT NULL,
  end_date    DATE DEFAULT NULL,
  description TEXT,
  status      VARCHAR(16) NOT NULL DEFAULT 'wait' COMMENT 'wait未开始 doing进行中 suspended已暂停 closed已关闭',
  deleted     TINYINT NOT NULL DEFAULT 0,
  created_by  BIGINT DEFAULT NULL,
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '项目表';

DROP TABLE IF EXISTS project_member;
CREATE TABLE project_member (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id  BIGINT NOT NULL,
  user_id     BIGINT NOT NULL,
  role        VARCHAR(32) DEFAULT 'dev' COMMENT '项目内角色 pm/dev/qa',
  hours_per_day DECIMAL(4,1) DEFAULT 8.0,
  UNIQUE KEY uk_proj_user (project_id, user_id)
) COMMENT '项目成员';

DROP TABLE IF EXISTS sprint;
CREATE TABLE sprint (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  project_id  BIGINT NOT NULL,
  name        VARCHAR(64) NOT NULL COMMENT '迭代名称',
  goal        TEXT COMMENT '迭代目标',
  begin_date  DATE DEFAULT NULL,
  end_date    DATE DEFAULT NULL,
  status      VARCHAR(16) NOT NULL DEFAULT 'wait' COMMENT 'wait未开始 doing进行中 closed已关闭',
  deleted     TINYINT NOT NULL DEFAULT 0,
  created_by  BIGINT DEFAULT NULL,
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_project (project_id, status)
) COMMENT '迭代(Sprint)表';

DROP TABLE IF EXISTS sprint_story;
CREATE TABLE sprint_story (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  sprint_id   BIGINT NOT NULL,
  story_id    BIGINT NOT NULL,
  UNIQUE KEY uk_sprint_story (sprint_id, story_id)
) COMMENT '迭代-需求关联(需求拉入迭代)';

-- ---------------- 任务域 ----------------
DROP TABLE IF EXISTS task;
CREATE TABLE task (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  sprint_id   BIGINT NOT NULL COMMENT '所属迭代',
  story_id    BIGINT DEFAULT NULL COMMENT '关联需求',
  parent_id   BIGINT NOT NULL DEFAULT 0 COMMENT '父任务(支持拆分)',
  name        VARCHAR(128) NOT NULL COMMENT '任务名称',
  description TEXT,
  type        VARCHAR(16) NOT NULL DEFAULT 'dev' COMMENT 'dev开发 test测试 design设计 study研究',
  priority    TINYINT NOT NULL DEFAULT 3,
  assigned_to BIGINT DEFAULT NULL COMMENT '指派给',
  estimate    DECIMAL(8,1) NOT NULL DEFAULT 0 COMMENT '预估工时',
  consumed    DECIMAL(8,1) NOT NULL DEFAULT 0 COMMENT '已消耗工时',
  `left`      DECIMAL(8,1) NOT NULL DEFAULT 0 COMMENT '剩余工时',
  deadline    DATE DEFAULT NULL,
  status      VARCHAR(16) NOT NULL DEFAULT 'wait' COMMENT 'wait未开始 doing进行中 done已完成 pause已暂停 cancel已取消 closed已关闭',
  finished_by BIGINT DEFAULT NULL,
  finished_time DATETIME DEFAULT NULL,
  deleted     TINYINT NOT NULL DEFAULT 0,
  created_by  BIGINT DEFAULT NULL,
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_sprint_status (sprint_id, status),
  KEY idx_assigned (assigned_to, status),
  KEY idx_story (story_id)
) COMMENT '任务表';

-- ---------------- 质量域 ----------------
DROP TABLE IF EXISTS bug;
CREATE TABLE bug (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id  BIGINT NOT NULL,
  sprint_id   BIGINT DEFAULT NULL COMMENT '所属迭代',
  story_id    BIGINT DEFAULT NULL COMMENT '关联需求',
  task_id     BIGINT DEFAULT NULL COMMENT '关联任务',
  title       VARCHAR(128) NOT NULL,
  steps       TEXT COMMENT '重现步骤',
  severity    TINYINT NOT NULL DEFAULT 3 COMMENT '严重程度 1致命 2严重 3一般 4轻微',
  priority    TINYINT NOT NULL DEFAULT 3,
  status      VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT 'active激活 resolved已解决 closed已关闭',
  assigned_to BIGINT DEFAULT NULL COMMENT '当前处理人',
  resolved_by BIGINT DEFAULT NULL,
  resolution  VARCHAR(32) DEFAULT '' COMMENT '解决方案 fixed/notbug/duplicate/bydesign/wontfix',
  resolved_time DATETIME DEFAULT NULL,
  closed_by   BIGINT DEFAULT NULL,
  closed_time DATETIME DEFAULT NULL,
  deleted     TINYINT NOT NULL DEFAULT 0,
  created_by  BIGINT DEFAULT NULL,
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_product_status (product_id, status),
  KEY idx_assigned (assigned_to, status)
) COMMENT '缺陷表';

DROP TABLE IF EXISTS test_case;
CREATE TABLE test_case (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id  BIGINT NOT NULL,
  story_id    BIGINT DEFAULT NULL,
  title       VARCHAR(128) NOT NULL,
  precondition VARCHAR(512) DEFAULT '' COMMENT '前置条件',
  steps       TEXT COMMENT '步骤与预期(JSON数组 [{step, expect}])',
  type        VARCHAR(16) DEFAULT 'feature' COMMENT 'feature功能 ui界面 performance性能',
  deleted     TINYINT NOT NULL DEFAULT 0,
  created_by  BIGINT DEFAULT NULL,
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_product (product_id)
) COMMENT '测试用例';

DROP TABLE IF EXISTS `release`;
CREATE TABLE `release` (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id  BIGINT NOT NULL,
  name        VARCHAR(64) NOT NULL COMMENT '版本号 如 v1.0.0',
  release_date DATE DEFAULT NULL,
  description TEXT,
  story_ids   JSON COMMENT '本次完成的需求ID清单',
  bug_ids     JSON COMMENT '本次修复的BugID清单',
  deleted     TINYINT NOT NULL DEFAULT 0,
  created_by  BIGINT DEFAULT NULL,
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT '产品发布';

DROP TABLE IF EXISTS sprint_burndown;
CREATE TABLE sprint_burndown (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  sprint_id   BIGINT NOT NULL,
  stat_date   DATE NOT NULL COMMENT '统计日期',
  left_hours  DECIMAL(10,1) NOT NULL DEFAULT 0 COMMENT '当日剩余总工时',
  task_total  INT NOT NULL DEFAULT 0,
  task_done   INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_sprint_date (sprint_id, stat_date)
) COMMENT '迭代燃尽图快照(定时任务每日生成)';

SET FOREIGN_KEY_CHECKS = 1;

-- ---------------- 基础数据: 角色 ----------------
INSERT INTO sys_role (id, role_code, role_name, remark) VALUES
 (1, 'ADMIN',  '系统管理员', '拥有全部权限'),
 (2, 'PO',     '产品经理',   '产品与需求管理'),
 (3, 'PM',     '项目经理',   '项目与迭代管理'),
 (4, 'DEV',    '开发工程师', '任务处理'),
 (5, 'QA',     '测试工程师', '缺陷与用例管理');

-- ---------------- 基础数据: 权限/菜单 ----------------
INSERT INTO sys_permission (id, parent_id, perm_name, perm_type, perm_key, path, icon, sort) VALUES
 (1,   0, '工作台',   2, 'dashboard',          '/dashboard', 'Odometer',   1),
 (2,   0, '产品管理', 1, '',                    '/product',   'Goods',      2),
 (3,   2, '产品列表', 2, 'product:list',        '/product/list',  '',       1),
 (4,   2, '需求管理', 2, 'story:list',          '/product/story', '',       2),
 (5,   2, '发布管理', 2, 'release:list',        '/product/release','',      3),
 (6,   0, '项目管理', 1, '',                    '/project',   'Folder',     3),
 (7,   6, '项目列表', 2, 'project:list',        '/project/list',  '',       1),
 (8,   6, '迭代看板', 2, 'sprint:board',        '/project/board', '',       2),
 (9,   0, '质量中心', 1, '',                    '/qa',        'Warning',    4),
 (10,  9, 'Bug管理', 2, 'bug:list',            '/qa/bug',     '',          1),
 (11,  9, '测试用例', 2, 'testcase:list',      '/qa/case',    '',          2),
 (12,  0, '统计报表', 2, 'stats:view',          '/stats',      'DataAnalysis', 5),
 (13,  0, '系统管理', 1, '',                    '/system',    'Setting',    6),
 (14, 13, '用户管理', 2, 'system:user:list',   '/system/user', '',         1),
 (15, 13, '角色管理', 2, 'system:role:list',   '/system/role', '',         2),
 (16, 13, '权限管理', 2, 'system:perm:list',   '/system/perm', '',         3),
 (17, 13, '部门管理', 2, 'system:dept:list',   '/system/dept', '',         4),
 (18, 13, '操作日志', 2, 'system:log:list',    '/system/log',  '',         5),
 -- 按钮级权限
 (19, 3,  '新增产品', 3, 'product:add',  '', '', 0),
 (20, 4,  '新增需求', 3, 'story:add',    '', '', 0),
 (21, 4,  '需求流转', 3, 'story:flow',   '', '', 0),
 (22, 7,  '新增项目', 3, 'project:add',  '', '', 0),
 (23, 8,  '任务指派', 3, 'task:assign',  '', '', 0),
 (24, 10, '提Bug',   3, 'bug:add',      '', '', 0),
 (25, 10, 'Bug处理', 3, 'bug:handle',   '', '', 0);

-- 管理员角色拥有全部权限
INSERT INTO sys_role_permission (role_id, perm_id)
SELECT 1, id FROM sys_permission;

-- 产品经理(PO)权限: 首页 + 产品管理(产品列表/需求管理/发布管理 + 按钮) + 统计报表
INSERT INTO sys_role_permission (role_id, perm_id) VALUES
 (2, 1),  (2, 2),  (2, 3),  (2, 4),  (2, 5),
 (2, 12),
 (2, 19), (2, 20), (2, 21);

-- 项目经理(PM)权限: 首页 + 项目管理(项目列表/迭代看板 + 按钮) + 统计报表
INSERT INTO sys_role_permission (role_id, perm_id) VALUES
 (3, 1),  (3, 6),  (3, 7),  (3, 8),
 (3, 12),
 (3, 22), (3, 23);

-- 开发工程师(DEV)权限: 首页 + 项目管理(迭代看板 + 任务指派) + 质量中心(Bug管理 + 提Bug/Bug处理) + 统计报表
INSERT INTO sys_role_permission (role_id, perm_id) VALUES
 (4, 1),  (4, 6),  (4, 8),  (4, 23),
 (4, 9),  (4, 10), (4, 24), (4, 25),
 (4, 12);

-- 测试工程师(QA)权限: 首页 + 质量中心(Bug管理/测试用例/测试单 + 提Bug/Bug处理) + 统计报表
INSERT INTO sys_role_permission (role_id, perm_id) VALUES
 (5, 1),  (5, 9),  (5, 10), (5, 11),
 (5, 12),
 (5, 24), (5, 25);

-- 默认部门
INSERT INTO sys_dept (id, parent_id, dept_name, sort) VALUES
 (1, 0, '研发部', 1), (2, 0, '产品部', 2), (3, 0, '测试部', 3);
