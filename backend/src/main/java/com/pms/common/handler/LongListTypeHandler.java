package com.pms.common.handler;

import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * List<Long> <-> JSON 字段处理器
 */
public class LongListTypeHandler extends AbstractJsonTypeHandler<List<Long>> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public LongListTypeHandler(Class<?> type) {
        super(type);
    }

    @Override
    public List<Long> parse(String json) {
        try {
            if (json == null || json.isBlank()) {
                return List.of();
            }
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public String toJson(List<Long> obj) {
        try {
            return MAPPER.writeValueAsString(obj == null ? List.of() : obj);
        } catch (Exception e) {
            return "[]";
        }
    }
}
