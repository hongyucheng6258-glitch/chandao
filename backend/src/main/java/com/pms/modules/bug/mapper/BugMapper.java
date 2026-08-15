package com.pms.modules.bug.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pms.modules.bug.entity.Bug;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BugMapper extends BaseMapper<Bug> {
}
