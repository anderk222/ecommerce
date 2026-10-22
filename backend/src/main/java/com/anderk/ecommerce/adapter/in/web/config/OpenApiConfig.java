package com.anderk.ecommerce.adapter.in.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The documentation of the API.
 *
 * <p>The contract of §12.5 asks for the API to be documented with OpenAPI,
 * served at {@code /swagger-ui}.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI xtrimOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Xtrim — Ecommerce + CRM de Leads")
                        .version("1.0.0")
                        .description("""
                                The public catalog, the anonymous tracking, the simulated checkout and \
                                the CRM of leads. The prefixes are `/api/public` (open), `/api/auth` \
                                (login) and `/api/admin` (staff only).""")
                        .license(new License().name("MIT")));
    }
}
