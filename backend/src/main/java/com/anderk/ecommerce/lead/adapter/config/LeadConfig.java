package com.anderk.ecommerce.lead.adapter.config;

import com.anderk.ecommerce.lead.domain.policy.LeadStatusPolicy;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The explicit wiring of the {@code lead} hexagon.
 */
@Configuration
public class LeadConfig {

    /** The rules of the pipeline of the leads (HU-22). */
    @Bean
    LeadStatusPolicy leadStatusPolicy() {
        return new LeadStatusPolicy();
    }
}
