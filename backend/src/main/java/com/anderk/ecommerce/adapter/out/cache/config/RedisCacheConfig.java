package com.anderk.ecommerce.adapter.out.cache.config;

import java.time.Duration;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * The cache of the read-mostly catalog queries.
 *
 * <p>Tech-stack §2.6: Redis is the cache of the home content, the listings
 * and the detail pages, and it is emptied whenever the catalog is written.
 *
 * <p>**The values are stored as JSON**, not with the Java serialization: the
 * values are records ({@code HomeContent}, {@code PageResponse}), and the
 * default serializer cannot write them — it fails with *"without a
 * serializer"*, and the cache silently stays empty, which turns every cached
 * query into a query to the database.
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {

    /** How long an entry lives. */
    public static final Duration TTL = Duration.ofMinutes(5);

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(TTL)
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.json()));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(configuration)
                .withInitialCacheConfigurations(java.util.Map.of(
                        "catalog-home", configuration,
                        "catalog-listings", configuration,
                        "catalog-detail", configuration))
                .build();
    }
}
