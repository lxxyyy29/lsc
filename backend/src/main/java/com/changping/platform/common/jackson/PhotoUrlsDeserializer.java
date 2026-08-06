package com.changping.platform.common.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * photoUrls 字段自定义反序列化器
 * 兼容前端传字符串（"url1,url2"）或数组（["url1","url2"]）两种格式，
 * 统一转换为 JSON 数组字符串存储（数据库 photo_urls 列为 json 类型）
 *
 * @author changping
 */
public class PhotoUrlsDeserializer extends JsonDeserializer<String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        if (node == null || node.isNull()) {
            return null;
        }
        List<String> urls = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode item : node) {
                if (item.isTextual() && !item.asText().isBlank()) {
                    urls.add(item.asText());
                }
            }
        } else {
            String text = node.asText();
            if (text == null || text.isBlank()) {
                return null;
            }
            String trimmed = text.trim();
            if (trimmed.startsWith("[")) {
                // 已是合法 JSON 数组字符串，原样保留
                try {
                    JsonNode parsed = OBJECT_MAPPER.readTree(trimmed);
                    if (parsed.isArray()) {
                        return text;
                    }
                } catch (Exception ignored) {
                    // 不是合法 JSON，按逗号拆分处理
                }
            }
            for (String part : text.split(",")) {
                if (!part.isBlank()) {
                    urls.add(part.trim());
                }
            }
        }
        return urls.isEmpty() ? null : OBJECT_MAPPER.writeValueAsString(urls);
    }
}
