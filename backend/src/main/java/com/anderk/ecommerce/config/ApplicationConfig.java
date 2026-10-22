package com.anderk.ecommerce.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * The auditing of the timestamps: {@code @CreatedDate} and {@code @LastModifiedDate}
 * are filled by Spring Data, not by the application.
 *
 * <p>It is not the audit trail of the versions — that one (HU-31) is retired.
 */
@Configuration
@EnableJpaAuditing
class ApplicationConfig {
}