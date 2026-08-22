-- ============================================================
-- 测试执行闭环：测试单 / 用例关联 / 执行记录
-- ============================================================

DROP TABLE IF EXISTS test_suite;
CREATE TABLE test_suite (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  name        VARCHAR(128) NOT NULL COMMENT '测试单名称',
  product_id  BIGINT NOT NULL COMMENT '所属产品',
  sprint_id   BIGINT DEFAULT NULL COMMENT '关联迭代',
  status      VARCHAR(16) NOT NULL DEFAULT 'planned' COMMENT 'planned待执行 running执行中 done已完成',
  remark      VARCHAR(512) DEFAULT '' COMMENT '备注',
  deleted     TINYINT NOT NULL DEFAULT 0,
  created_by  BIGINT DEFAULT NULL,
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_product (product_id),
  KEY idx_sprint (sprint_id)
) COMMENT '测试单';

DROP TABLE IF EXISTS suite_case;
CREATE TABLE suite_case (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  suite_id    BIGINT NOT NULL,
  case_id     BIGINT NOT NULL,
  UNIQUE KEY uk_suite_case (suite_id, case_id),
  KEY idx_case (case_id)
) COMMENT '测试单-用例关联';

DROP TABLE IF EXISTS test_run;
CREATE TABLE test_run (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  suite_id     BIGINT NOT NULL,
  case_id      BIGINT NOT NULL,
  executor_id  BIGINT DEFAULT NULL COMMENT '执行人',
  result       VARCHAR(16) DEFAULT NULL COMMENT 'pass通过 fail失败 blocked阻塞 null未执行',
  remark       VARCHAR(512) DEFAULT '' COMMENT '执行备注',
  spent_minutes INT DEFAULT 0 COMMENT '耗时(分钟)',
  executed_time DATETIME DEFAULT NULL COMMENT '执行时间',
  created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_suite_case_run (suite_id, case_id),
  KEY idx_suite (suite_id)
) COMMENT '测试执行记录';

-- 菜单：测试单（挂在质量中心 id=9 下；与 Bug/测试用例 同级显示）
INSERT INTO sys_permission (parent_id, perm_name, perm_type, perm_key, path, icon, sort, deleted)
VALUES (9, '测试单', 2, 'testsuite:list', '/qa/suite', 'Files', 3, 0);

-- 测试单权限授予测试工程师(QA)，admin 已拥有全部权限无需处理
INSERT INTO sys_role_permission (role_id, perm_id)
SELECT DISTINCT 5, id FROM sys_permission WHERE perm_key = 'testsuite:list' AND deleted = 0;
