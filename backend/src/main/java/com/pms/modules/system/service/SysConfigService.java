package com.pms.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pms.common.exception.BizException;
import com.pms.common.utils.SecurityUtil;
import com.pms.modules.system.entity.SysConfig;
import com.pms.modules.system.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 系统配置服务: 数据库持久化 + Redis 缓存
 * 修改配置后清除缓存, 下次读取自动重新加载, 实现"修改后自动更新"
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysConfigService {

    private static final String CACHE_KEY = "config:all";
    private static final long CACHE_TTL_HOURS = 24;

    private final SysConfigMapper configMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    /** 获取全部配置(带缓存) */
    @SuppressWarnings("unchecked")
    public Map<String, String> getAllConfig() {
        Object cached = redisTemplate.opsForValue().get(CACHE_KEY);
        if (cached instanceof Map) {
            return (Map<String, String>) cached;
        }
        List<SysConfig> list = configMapper.selectList(
                new LambdaQueryWrapper<SysConfig>().orderByAsc(SysConfig::getSort));
        Map<String, String> map = list.stream()
                .collect(Collectors.toMap(SysConfig::getConfigKey,
                        c -> c.getConfigValue() == null ? "" : c.getConfigValue(),
                        (a, b) -> b));
        redisTemplate.opsForValue().set(CACHE_KEY, map, CACHE_TTL_HOURS, TimeUnit.HOURS);
        return map;
    }

    /** 获取全部配置详情列表(管理页面用, 不走缓存以保证实时) */
    public List<SysConfig> listAll() {
        return configMapper.selectList(
                new LambdaQueryWrapper<SysConfig>().orderByAsc(SysConfig::getSort));
    }

    /** 获取单个配置值 */
    public String getValue(String key) {
        return getAllConfig().getOrDefault(key, "");
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(getValue(key));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String v = getValue(key);
        if (v == null || v.isBlank()) return defaultValue;
        return "true".equalsIgnoreCase(v) || "1".equals(v);
    }

    /** 批量更新配置 */
    @Transactional
    public void updateBatch(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        Long userId = SecurityUtil.getUserId();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            SysConfig existing = configMapper.selectOne(
                    new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, key));
            if (existing == null) {
                throw new BizException("配置项不存在: " + key);
            }
            existing.setConfigValue(value);
            existing.setUpdatedBy(userId);
            configMapper.updateById(existing);
            log.info("配置更新: {} = {}", key, value);
        }
        // 清除缓存, 下次读取自动重新加载
        redisTemplate.delete(CACHE_KEY);
        log.info("配置缓存已清除, 下次请求自动重新加载");
    }

    /** 刷新缓存(手动触发) */
    public void refreshCache() {
        redisTemplate.delete(CACHE_KEY);
        getAllConfig();
        log.info("配置缓存已手动刷新");
    }
}
