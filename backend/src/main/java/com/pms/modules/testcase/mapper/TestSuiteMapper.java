package com.pms.modules.testcase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pms.modules.testcase.entity.TestSuite;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TestSuiteMapper extends BaseMapper<TestSuite> {
}
