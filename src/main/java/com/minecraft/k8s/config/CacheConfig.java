package com.minecraft.k8s.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 缓存配置
 * 使用 Caffeine LoadingCache 实现后台异步刷新
 * 
 * 刷新策略：
 * - refreshAfterWrite: 10秒后，下次访问时异步刷新（立即返回旧值，后台更新）
 * - expireAfterWrite: 60秒后强制过期（防止长时间不访问导致数据过旧）
 */
@Configuration
public class CacheConfig {

    // 刷新时间：10秒后触发异步刷新
    public static final long REFRESH_SECONDS = 10;
    // 过期时间：60秒后强制过期
    public static final long EXPIRE_SECONDS = 60;
    // 最大缓存条目数
    public static final long MAX_SIZE = 10000;

    /**
     * 缓存刷新线程池
     */
    @Bean
    public Executor cacheRefreshExecutor() {
        return Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "cache-refresh");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * 创建 Caffeine 构建器（供各 Service 使用）
     */
    @SuppressWarnings("unchecked")
    public static <K, V> Caffeine<K, V> newCacheBuilder() {
        return (Caffeine<K, V>) Caffeine.newBuilder()
                .refreshAfterWrite(REFRESH_SECONDS, TimeUnit.SECONDS)
                .expireAfterWrite(EXPIRE_SECONDS, TimeUnit.SECONDS)
                .maximumSize(MAX_SIZE);
    }
}
