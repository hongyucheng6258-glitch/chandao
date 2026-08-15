package com.pms.modules.product.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pms.common.annotation.LogOperation;
import com.pms.common.result.Result;
import com.pms.modules.product.entity.Release;
import com.pms.modules.product.mapper.ReleaseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/releases")
@RequiredArgsConstructor
public class ReleaseController {

    private final ReleaseMapper releaseMapper;

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('release:list')")
    public Result<Page<Release>> page(@RequestParam(defaultValue = "1") long pageNum,
                                      @RequestParam(defaultValue = "10") long pageSize,
                                      @RequestParam(required = false) Long productId) {
        LambdaQueryWrapper<Release> wrapper = new LambdaQueryWrapper<Release>()
                .eq(productId != null, Release::getProductId, productId)
                .orderByDesc(Release::getId);
        return Result.ok(releaseMapper.selectPage(new Page<>(pageNum, pageSize), wrapper));
    }

    @GetMapping("/{id}")
    public Result<Release> detail(@PathVariable Long id) {
        return Result.ok(releaseMapper.selectById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('release:list')")
    @LogOperation(objectType = "release", action = "创建发布", objectId = "#result.data")
    public Result<Long> create(@RequestBody Release release) {
        release.setId(null);
        releaseMapper.insert(release);
        return Result.ok(release.getId());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('release:list')")
    public Result<Void> update(@PathVariable Long id, @RequestBody Release release) {
        release.setId(id);
        releaseMapper.updateById(release);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('release:list')")
    public Result<Void> delete(@PathVariable Long id) {
        releaseMapper.deleteById(id);
        return Result.ok();
    }
}
