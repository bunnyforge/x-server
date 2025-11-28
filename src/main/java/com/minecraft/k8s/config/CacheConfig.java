package com.minecraft.k8s.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cache.CacheManager;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 缓存配置
 * 用于服务器指标缓存，使用随机过期时间防止缓存雪崩
 */
@Configuration
@EnableCaching
public class CacheConfig {

    // 基础过期时间 20 秒
    private static final long BASE_EXPIRE_SECONDS = 20;
    // 随机浮动范围 ±10 秒（即 10-30 秒）
    private static final long RANDOM_RANGE_SECONDS = 10;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("serverMetrics");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfter(new Expiry<Object, Object>() {
                    @Override
                    public long expireAfterCreate(Object key, Object value, long currentTime) {
                        // 随机过期时间：20 ± 10 秒（10-30 秒）
                        long randomSeconds = BASE_EXPIRE_SECONDS + 
                                ThreadLocalRandom.current().nextLong(-RANDOM_RANGE_SECONDS, RANDOM_RANGE_SECONDS + 1);
                        return TimeUnit.SECONDS.toNanos(randomSeconds);
                    }

                    @Override
                    public long expireAfterUpdate(Object key, Object value, long currentTime, long currentDuration) {
                        // 更新时重新计算随机过期时间
                        long randomSeconds = BASE_EXPIRE_SECONDS + 
                                ThreadLocalRandom.current().nextLong(-RANDOM_RANGE_SECONDS, RANDOM_RANGE_SECONDS + 1);
                        return TimeUnit.SECONDS.toNanos(randomSeconds);
                    }

                    @Override
                    public long expireAfterRead(Object key, Object value, long currentTime, long currentDuration) {
                        // 读取不改变过期时间
                        return currentDuration;
                    }
                })
                .maximumSize(1000));
        return cacheManager;
    }
}
