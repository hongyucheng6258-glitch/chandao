package com.pms.modules.attachment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 附件(参考禅道 file 模块)
 */
@Data
@TableName("sys_attachment")
public class SysAttachment implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String objectType;

    private Long objectId;

    /** 原始文件名 */
    private String fileName;

    /** 磁盘存储文件名(uuid) */
    private String storedName;

    /** 扩展名(不含点) */
    private String fileExt;

    /** 字节数 */
    private Long fileSize;

    private Long uploaderId;

    private String uploaderName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
