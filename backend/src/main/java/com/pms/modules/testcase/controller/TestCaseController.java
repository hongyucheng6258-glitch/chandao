package com.pms.modules.testcase.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pms.common.result.Result;
import com.pms.modules.testcase.entity.TestCase;
import com.pms.modules.testcase.mapper.TestCaseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/testcases")
@RequiredArgsConstructor
public class TestCaseController {

    private final TestCaseMapper testCaseMapper;

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('testcase:list')")
    public Result<Page<TestCase>> page(@RequestParam(defaultValue = "1") long pageNum,
                                       @RequestParam(defaultValue = "10") long pageSize,
                                       @RequestParam(required = false) Long productId,
                                       @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<TestCase> wrapper = new LambdaQueryWrapper<TestCase>()
                .eq(productId != null, TestCase::getProductId, productId)
                .like(keyword != null && !keyword.isBlank(), TestCase::getTitle, keyword)
                .orderByDesc(TestCase::getId);
        return Result.ok(testCaseMapper.selectPage(new Page<>(pageNum, pageSize), wrapper));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('testcase:list')")
    public Result<TestCase> detail(@PathVariable Long id) {
        return Result.ok(testCaseMapper.selectById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('testcase:list')")
    public Result<Long> create(@RequestBody TestCase testCase) {
        testCase.setId(null);
        testCaseMapper.insert(testCase);
        return Result.ok(testCase.getId());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('testcase:list')")
    public Result<Void> update(@PathVariable Long id, @RequestBody TestCase testCase) {
        testCase.setId(id);
        testCaseMapper.updateById(testCase);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('testcase:list')")
    public Result<Void> delete(@PathVariable Long id) {
        testCaseMapper.deleteById(id);
        return Result.ok();
    }
}
