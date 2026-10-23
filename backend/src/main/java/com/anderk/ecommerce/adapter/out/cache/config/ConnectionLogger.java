package com.anderk.ecommerce.adapter.out.cache.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

/**
 * Reports the state of the infrastructure the application talks to.
 *
 * <p>Lettuce opens its connection lazily, so without this the startup log
 * would never mention Redis and a half-started stack would go unnoticed. The
 * check never fails the boot: the application starts either way, but the log
 * says what is reachable.
 */
@Component
public class ConnectionLogger implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ConnectionLogger.class);

    private final RedisConnectionFactory redisConnectionFactory;
    private final Environment environment;

    public ConnectionLogger(RedisConnectionFactory redisConnectionFactory,
                                          Environment environment) {
        this.redisConnectionFactory = redisConnectionFactory;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            log.info("Connected to Redis at {}:{} — {}",
                    environment.getProperty("spring.data.redis.host", "localhost"),
                    environment.getProperty("spring.data.redis.port", "6379"),
                    connection.ping());
        } catch (RuntimeException ex) {
            log.warn("Redis is not reachable: {}", ex.getMessage());
        }
    }
}
