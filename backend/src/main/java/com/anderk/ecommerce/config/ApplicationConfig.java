package com.anderk.ecommerce.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * The auditing of the persistence entities.
 *
 * <p>{@code @CreatedDate} and {@code @LastModifiedDate} need this switch:
 * without it the columns {@code created_at} and {@code updated_at} arrive
 * null and the database rejects the insert.
 */
@Configuration
@EnableJpaAuditing
class ApplicationConfig {
}
