package com.anderk.ecommerce.adapter.out.cache.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Configuration;

/**
 * The behaviour of the cache when it fails.
 *
 * <p>The cache is **never** a reason for a request to fail: if Redis is down,
 * the query goes to the database. A cache that turns an outage into a black
 * screen is worse than no cache at all.
 *
 * <p>How the cache is built is in {@link RedisCacheConfig}.
 */
@Configuration
public class CacheConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {

            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("The cache '{}' could not be read: {}", cache.getName(), exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("The cache '{}' could not be written: {}", cache.getName(), exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("The cache '{}' could not be emptied: {}", cache.getName(), exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("The cache '{}' could not be emptied: {}", cache.getName(), exception.getMessage());
            }
        };
    }
}
