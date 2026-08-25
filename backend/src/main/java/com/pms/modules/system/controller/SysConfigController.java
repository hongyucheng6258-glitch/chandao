package com.pms.modules.system.controller;

import com.pms.common.result.Result;
import com.pms.modules.system.entity.SysConfig;
import com.pms.modules.system.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统配置管理: 管理员可查看和修改全局配置, 修改后自动清除缓存实时生效
 */
@RestController
@RequestMapping("/configs")
@RequiredArgsConstructor
public class SysConfigController {

    private final SysConfigService configService;

    /** 获取全部配置详情(管理页面渲染表单) */
    @GetMapping
    @PreAuthorize("hasAuthority('system:config:list')")
    public Result<List<SysConfig>> list() {
        return Result.ok(configService.listAll());
    }

    /** 获取全部配置键值对(供其他模块运行时读取) */
    @GetMapping("/values")
    public Result<Map<String, String>> values() {
        return Result.ok(configService.getAllConfig());
    }

    /** 批量更新配置 */
    @PutMapping
    @PreAuthorize("hasAuthority('system:config:list')")
    public Result<Void> updateBatch(@RequestBody Map<String, String> values) {
        configService.updateBatch(values);
        return Result.ok();
    }

    /** 手动刷新缓存 */
    @PostMapping("/refresh")
    @PreAuthorize("hasAuthority('system:config:list')")
    public Result<Void> refresh() {
        configService.refreshCache();
        return Result.ok();
    }
}
