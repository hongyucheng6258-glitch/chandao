-- 附件表(参考禅道 file 模块): 需求/任务/Bug 均可挂载附件
CREATE TABLE IF NOT EXISTS sys_attachment (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '附件ID',
  object_type VARCHAR(32) NOT NULL COMMENT '业务对象类型: story/bug/task',
  object_id BIGINT NOT NULL COMMENT '业务对象ID',
  file_name VARCHAR(256) NOT NULL COMMENT '原始文件名',
  stored_name VARCHAR(128) NOT NULL COMMENT '存储文件名(uuid, 防重名)',
  file_ext VARCHAR(16) COMMENT '扩展名(不含点)',
  file_size BIGINT COMMENT '文件大小(字节)',
  uploader_id BIGINT COMMENT '上传者用户ID',
  uploader_name VARCHAR(64) COMMENT '上传者姓名',
  created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='附件(参考禅道 file 模块)';

CREATE INDEX IF NOT EXISTS idx_att_obj ON sys_attachment(object_type, object_id);
