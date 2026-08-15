package com.pms.modules.product.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pms.common.annotation.LogOperation;
import com.pms.common.result.Result;
import com.pms.modules.product.entity.Product;
import com.pms.modules.product.entity.ProductPlan;
import com.pms.modules.product.mapper.ProductMapper;
import com.pms.modules.product.mapper.ProductPlanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductMapper productMapper;
    private final ProductPlanMapper planMapper;

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('product:list')")
    public Result<Page<Product>> page(@RequestParam(defaultValue = "1") long pageNum,
                                      @RequestParam(defaultValue = "10") long pageSize,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) String status) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .like(keyword != null && !keyword.isBlank(), Product::getName, keyword)
                .eq(status != null && !status.isBlank(), Product::getStatus, status)
                .orderByDesc(Product::getId);
        return Result.ok(productMapper.selectPage(new Page<>(pageNum, pageSize), wrapper));
    }

    @GetMapping("/options")
    public Result<List<Product>> options() {
        return Result.ok(productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, "normal").orderByDesc(Product::getId)));
    }

    @GetMapping("/{id}")
    public Result<Product> detail(@PathVariable Long id) {
        return Result.ok(productMapper.selectById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('product:add')")
    @LogOperation(objectType = "product", action = "创建产品", objectId = "#result.data")
    public Result<Long> create(@RequestBody Product product) {
        product.setId(null);
        product.setStatus("normal");
        productMapper.insert(product);
        return Result.ok(product.getId());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('product:add')")
    @LogOperation(objectType = "product", action = "编辑产品", objectId = "#id")
    public Result<Void> update(@PathVariable Long id, @RequestBody Product product) {
        product.setId(id);
        productMapper.updateById(product);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('product:add')")
    public Result<Void> delete(@PathVariable Long id) {
        productMapper.deleteById(id);
        return Result.ok();
    }

    // ---------- 产品计划 ----------

    @GetMapping("/{id}/plans")
    public Result<List<ProductPlan>> plans(@PathVariable Long id) {
        return Result.ok(planMapper.selectList(new LambdaQueryWrapper<ProductPlan>()
                .eq(ProductPlan::getProductId, id).orderByDesc(ProductPlan::getId)));
    }

    @PostMapping("/{id}/plans")
    @PreAuthorize("hasAuthority('product:add')")
    public Result<Void> createPlan(@PathVariable Long id, @RequestBody ProductPlan plan) {
        plan.setId(null);
        plan.setProductId(id);
        planMapper.insert(plan);
        return Result.ok();
    }

    @DeleteMapping("/plans/{planId}")
    @PreAuthorize("hasAuthority('product:add')")
    public Result<Void> deletePlan(@PathVariable Long planId) {
        planMapper.deleteById(planId);
        return Result.ok();
    }
}
