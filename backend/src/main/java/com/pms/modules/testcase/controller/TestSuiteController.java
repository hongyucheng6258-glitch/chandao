package com.pms.modules.testcase.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pms.common.result.Result;
import com.pms.common.utils.SecurityUtil;
import com.pms.modules.testcase.entity.TestCase;
import com.pms.modules.testcase.entity.TestRun;
import com.pms.modules.testcase.entity.TestSuite;
import com.pms.modules.testcase.entity.SuiteCase;
import com.pms.modules.testcase.mapper.TestCaseMapper;
import com.pms.modules.testcase.mapper.TestRunMapper;
import com.pms.modules.testcase.mapper.TestSuiteMapper;
import com.pms.modules.testcase.mapper.SuiteCaseMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/test-suites")
@RequiredArgsConstructor
public class TestSuiteController {

    private final TestSuiteMapper suiteMapper;
    private final SuiteCaseMapper suiteCaseMapper;
    private final TestRunMapper runMapper;
    private final TestCaseMapper caseMapper;

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('testsuite:list')")
    public Result<Page<TestSuite>> page(@RequestParam(defaultValue = "1") long pageNum,
                                        @RequestParam(defaultValue = "10") long pageSize,
                                        @RequestParam(required = false) Long productId,
                                        @RequestParam(required = false) String status,
                                        @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<TestSuite> wrapper = new LambdaQueryWrapper<TestSuite>()
                .eq(productId != null, TestSuite::getProductId, productId)
                .eq(status != null && !status.isBlank(), TestSuite::getStatus, status)
                .like(keyword != null && !keyword.isBlank(), TestSuite::getName, keyword)
                .orderByDesc(TestSuite::getId);
        return Result.ok(suiteMapper.selectPage(new Page<>(pageNum, pageSize), wrapper));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('testsuite:list')")
    public Result<Long> create(@RequestBody CreateReq req) {
        TestSuite s = new TestSuite();
        s.setName(req.name);
        s.setProductId(req.productId);
        s.setSprintId(req.sprintId);
        s.setRemark(req.remark);
        s.setStatus("planned");
        suiteMapper.insert(s);
        if (req.caseIds != null) {
            for (Long cid : req.caseIds) {
                SuiteCase sc = new SuiteCase();
                sc.setSuiteId(s.getId());
                sc.setCaseId(cid);
                // 复用 suite_case 表仅做关联记录；执行明细在 test_run
                TestRun r = new TestRun();
                r.setSuiteId(s.getId());
                r.setCaseId(cid);
                runMapper.insert(r);
                // suite_case 关联表保留以支持去重/统计
                suiteCaseMapper.insert(sc);
            }
        }
        return Result.ok(s.getId());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('testsuite:list')")
    public Result<SuiteDetail> get(@PathVariable Long id) {
        TestSuite s = suiteMapper.selectById(id);
        List<SuiteCase> scs = suiteCaseMapper.selectList(
                new LambdaQueryWrapper<SuiteCase>().eq(SuiteCase::getSuiteId, id));
        List<Long> caseIds = scs.stream().map(SuiteCase::getCaseId).collect(Collectors.toList());
        Map<Long, TestCase> caseMap = caseIds.isEmpty() ? Collections.emptyMap()
                : caseMapper.selectList(new LambdaQueryWrapper<TestCase>().in(TestCase::getId, caseIds))
                .stream().collect(Collectors.toMap(TestCase::getId, c -> c, (a, b) -> a));
        List<TestRun> runs = runMapper.selectList(
                new LambdaQueryWrapper<TestRun>().eq(TestRun::getSuiteId, id));
        Map<Long, TestRun> runMap = runs.stream().collect(
                Collectors.toMap(TestRun::getCaseId, r -> r, (a, b) -> a));

        SuiteDetail detail = new SuiteDetail();
        detail.suite = s;
        List<CaseRun> list = new ArrayList<>();
        for (Long cid : caseIds) {
            TestCase c = caseMap.get(cid);
            TestRun r = runMap.get(cid);
            CaseRun cr = new CaseRun();
            cr.caseId = cid;
            cr.title = c == null ? "" : c.getTitle();
            cr.type = c == null ? "" : c.getType();
            cr.precondition = c == null ? "" : c.getPrecondition();
            cr.steps = c == null ? "" : c.getSteps();
            if (r != null) {
                cr.runId = r.getId();
                cr.result = r.getResult();
                cr.remark = r.getRemark();
                cr.spentMinutes = r.getSpentMinutes();
                cr.executorId = r.getExecutorId();
                cr.executedTime = r.getExecutedTime();
            }
            list.add(cr);
        }
        detail.cases = list;
        detail.summary = buildSummary(runs);
        return Result.ok(detail);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('testsuite:list')")
    public Result<Void> update(@PathVariable Long id, @RequestBody TestSuite suite) {
        suite.setId(id);
        suiteMapper.updateById(suite);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('testsuite:list')")
    public Result<Void> delete(@PathVariable Long id) {
        suiteMapper.deleteById(id);
        suiteCaseMapper.delete(new LambdaQueryWrapper<SuiteCase>().eq(SuiteCase::getSuiteId, id));
        runMapper.delete(new LambdaQueryWrapper<TestRun>().eq(TestRun::getSuiteId, id));
        return Result.ok();
    }

    /** 记录某用例执行结果；全部执行完则测试单置为 done */
    @PutMapping("/{id}/run")
    @PreAuthorize("hasAuthority('testsuite:list')")
    public Result<Void> run(@PathVariable Long id, @RequestBody RunReq req) {
        TestRun run = runMapper.selectOne(new LambdaQueryWrapper<TestRun>()
                .eq(TestRun::getSuiteId, id).eq(TestRun::getCaseId, req.caseId));
        if (run == null) {
            run = new TestRun();
            run.setSuiteId(id);
            run.setCaseId(req.caseId);
        }
        run.setResult(req.result);
        run.setRemark(req.remark);
        run.setSpentMinutes(req.spentMinutes);
        run.setExecutorId(SecurityUtil.getUserId());
        run.setExecutedTime(LocalDateTime.now());
        if (run.getId() == null) {
            runMapper.insert(run);
        } else {
            runMapper.updateById(run);
        }
        long total = runMapper.selectCount(new LambdaQueryWrapper<TestRun>().eq(TestRun::getSuiteId, id));
        long executed = runMapper.selectCount(new LambdaQueryWrapper<TestRun>()
                .eq(TestRun::getSuiteId, id).isNotNull(TestRun::getResult));
        if (total > 0 && total == executed) {
            TestSuite s = suiteMapper.selectById(id);
            if (s != null && !"done".equals(s.getStatus())) {
                s.setStatus("done");
                suiteMapper.updateById(s);
            }
        }
        return Result.ok();
    }

    private Summary buildSummary(List<TestRun> runs) {
        Summary s = new Summary();
        s.total = runs.size();
        s.pass = (int) runs.stream().filter(r -> "pass".equals(r.getResult())).count();
        s.fail = (int) runs.stream().filter(r -> "fail".equals(r.getResult())).count();
        s.blocked = (int) runs.stream().filter(r -> "blocked".equals(r.getResult())).count();
        s.unexecuted = (int) runs.stream().filter(r -> r.getResult() == null).count();
        s.passRate = s.total == 0 ? 0 : (int) Math.round(s.pass * 100.0 / s.total);
        return s;
    }

    // ---------------- DTO / VO ----------------
    @Data
    public static class CreateReq {
        private String name;
        private Long productId;
        private Long sprintId;
        private String remark;
        private List<Long> caseIds;
    }

    @Data
    public static class RunReq {
        private Long caseId;
        private String result;
        private String remark;
        private Integer spentMinutes;
    }

    @Data
    public static class SuiteDetail {
        private TestSuite suite;
        private List<CaseRun> cases;
        private Summary summary;
    }

    @Data
    public static class CaseRun {
        private Long caseId;
        private Long runId;
        private String title;
        private String type;
        private String precondition;
        private String steps;
        private String result;
        private String remark;
        private Integer spentMinutes;
        private Long executorId;
        private LocalDateTime executedTime;
    }

    @Data
    public static class Summary {
        private int total;
        private int pass;
        private int fail;
        private int blocked;
        private int unexecuted;
        private int passRate;
    }
}
