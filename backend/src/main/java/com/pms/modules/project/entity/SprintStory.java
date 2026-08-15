package com.pms.modules.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sprint_story")
public class SprintStory implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sprintId;

    private Long storyId;
}
