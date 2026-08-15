package com.pms.modules.testcase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pms.modules.testcase.entity.TestCase;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TestCaseMapper extends BaseMapper<TestCase> {
}
