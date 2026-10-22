package com.anderk.ecommerce.architecture.fixture.domain.model;

import org.springframework.stereotype.Component;

/**
 * A deliberate violation, used only by the test that proves the framework
 * rule has teeth. It lives in a test source tree and is never loaded by the
 * application.
 */
@Component
public class LeakyDomainClass {

    public String name() {
        return "leaky";
    }
}
