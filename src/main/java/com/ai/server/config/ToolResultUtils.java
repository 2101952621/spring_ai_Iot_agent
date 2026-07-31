package com.ai.server.config;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 用于存储 Tool 调用过程中产生的结果数据，以 requestId 为外层 key，内层为 field-value 键值对。
 */
@Slf4j
public class ToolResultUtils {

    /**
     * 默认 TTL（分钟）
     */
    private static final long DEFAULT_TTL_MINUTES = 5;

    /**
     * 最大 key 数量上限
     */
    private static final int MAX_ENTRIES = 10000;

    /**
     * 清理间隔（分钟）
     */
    private static final long CLEANUP_INTERVAL_MINUTES = 1;

    /**
     * key -> (field -> value, expireTimestamp)
     */
    private static final Map<String, CacheEntry> STORE = new ConcurrentHashMap<>();

    static {
        var cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            var t = new Thread(r, "tool-result-cleaner");
            t.setDaemon(true);
            return t;
        });
        cleaner.scheduleAtFixedRate(
                ToolResultUtils::evictExpired,
                CLEANUP_INTERVAL_MINUTES,
                CLEANUP_INTERVAL_MINUTES,
                TimeUnit.MINUTES);
    }

    private ToolResultUtils() {
    }

    /**
     * 存入单个 field-value
     */
    public static void put(String key, String field, Object value) {
        if (key == null || field == null) {
            log.warn("put 失败: key 或 field 为空");
            return;
        }
        if (STORE.size() >= MAX_ENTRIES) {
            log.warn("put 失败: 存储已达容量上限 [{}]", MAX_ENTRIES);
            return;
        }
        STORE.compute(key, (k, entry) -> {
            if (entry == null) {
                entry = new CacheEntry(new ConcurrentHashMap<>());
            }
            entry.expireAt = currentExpireAt();
            return entry;
        });
        STORE.get(key).data.put(field, value);
        log.debug("ToolResultUtils.put: key={}, field={}", key, field);
    }

    /**
     * 批量存入
     */
    public static void putAll(String key, Map<String, Object> fields) {
        if (key == null || fields == null || fields.isEmpty()) {
            log.warn("putAll 失败: 参数无效");
            return;
        }
        if (STORE.size() >= MAX_ENTRIES) {
            log.warn("putAll 失败: 存储已达容量上限 [{}]", MAX_ENTRIES);
            return;
        }
        STORE.compute(key, (k, entry) -> {
            if (entry == null) {
                entry = new CacheEntry(new ConcurrentHashMap<>());
            }
            entry.expireAt = currentExpireAt();
            return entry;
        });
        STORE.get(key).data.putAll(fields);
        log.debug("ToolResultUtils.putAll: key={}, fieldsCount={}", key, fields.size());
    }

    // ==================== 读操作 ====================

    /**
     * 获取指定 key 下的所有 field-value
     */
    public static Map<String, Object> get(String key) {
        var entry = lookup(key);
        return entry != null ? Collections.unmodifiableMap(entry.data) : null;
    }

    /**
     * 获取指定 field 的值
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String key, String field) {
        if (key == null || field == null) {
            return null;
        }
        var entry = lookup(key);
        return entry != null ? (T) entry.data.get(field) : null;
    }

    /**
     * 获取指定 field 的值，带类型转换
     */
    public static <T> T get(String key, String field, Class<T> type) {
        return type.cast(get(key, field));
    }

    // ==================== 移除与清理 ====================

    /**
     * 移除指定 key 下的所有数据
     */
    public static void remove(String key) {
        if (key == null) {
            return;
        }
        STORE.remove(key);
        log.debug("ToolResultUtils.remove: key={}", key);
    }

    /**
     * 移除指定 key 下的某个 field
     */
    public static void remove(String key, String field) {
        if (key == null || field == null) {
            return;
        }
        Optional.ofNullable(STORE.get(key))
                .ifPresent(entry -> entry.data.remove(field));
    }

    // ==================== 状态查询 ====================

    /**
     * 当前 key 数量
     */
    public static int size() {
        return STORE.size();
    }

    /**
     * 清空所有
     */
    public static void clear() {
        STORE.clear();
        log.info("ToolResultUtils 已清空");
    }


    /**
     * 查找未过期的 entry
     */
    private static CacheEntry lookup(String key) {
        if (key == null) {
            return null;
        }
        var entry = STORE.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.expireAt < System.currentTimeMillis()) {
            STORE.remove(key);
            log.debug("key={} 已过期，已移除", key);
            return null;
        }
        return entry;
    }

    /**
     * 清理所有过期条目
     */
    private static void evictExpired() {
        long now = System.currentTimeMillis();
        int evicted = 0;
        for (var it = STORE.entrySet().iterator(); it.hasNext(); ) {
            var entry = it.next();
            if (entry.getValue().expireAt < now) {
                it.remove();
                evicted++;
            }
        }
        if (evicted > 0) {
            log.info("ToolResultUtil 清理过期条目: {} 条, 当前剩余: {} 条", evicted, STORE.size());
        }
    }

    private static long currentExpireAt() {
        return System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(DEFAULT_TTL_MINUTES);
    }

    // ==================== 内部类 ====================

    private static class CacheEntry {
        final Map<String, Object> data;
        volatile long expireAt;

        CacheEntry(Map<String, Object> data) {
            this.data = data;
            this.expireAt = currentExpireAt();
        }
    }
}
