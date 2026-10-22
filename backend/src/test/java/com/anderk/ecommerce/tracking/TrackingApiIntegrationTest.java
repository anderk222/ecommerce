package com.anderk.ecommerce.tracking;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.anderk.ecommerce.TestcontainersConfiguration;
import com.anderk.ecommerce.tracking.domain.model.EventType;
import com.anderk.ecommerce.tracking.domain.port.ActivityEventRepository;
import com.anderk.ecommerce.tracking.domain.port.VisitorRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

/**
 * The record of the activity of the visitor (HU-09).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
@Transactional
class TrackingApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ActivityEventRepository eventRepository;

    @Autowired
    private VisitorRepository visitorRepository;

    @Test
    @DisplayName("an event is accepted and recorded")
    void anEventIsAcceptedAndRecorded() {
        String anonymousId = "3f0c9b52-6a1e-4d8e-9c3a-2b7d5e1f8a10";

        ResponseEntity<Void> response = this.track(anonymousId, EventType.SEARCH, Map.of("query", "internet hogar"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        assertThat(this.eventRepository.findByAnonymousId(anonymousId, 10)).hasSize(1);
        assertThat(this.eventRepository.findByAnonymousId(anonymousId, 10).get(0).metadata())
                .containsEntry("query", "internet hogar");
    }

    @Test
    @DisplayName("the browser is registered the first time it is seen")
    void theBrowserIsRegisteredTheFirstTimeItIsSeen() {
        String anonymousId = "11111111-2222-4333-8444-555555555555";

        this.track(anonymousId, EventType.PRODUCT_VIEW, Map.of("slug", "router-wifi-6-ax3000"));

        assertThat(this.visitorRepository.findByAnonymousId(anonymousId)).isPresent();
    }

    @Test
    @DisplayName("an event without the identifier of the browser is refused")
    void anEventWithoutTheIdentifierOfTheBrowserIsRefused() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> response = this.restTemplate.exchange("/api/public/events", HttpMethod.POST,
                new HttpEntity<>(Map.of("type", "SEARCH"), headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("code", "VALIDATION_ERROR");
    }

    private ResponseEntity<Void> track(String anonymousId, EventType type, Map<String, Object> metadata) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> response = this.restTemplate.exchange("/api/public/events", HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "anonymousId", anonymousId,
                        "type", type.name(),
                        "entityType", "CATALOG",
                        "metadata", metadata), headers), Map.class);
        return new ResponseEntity<>(response.getStatusCode());
    }
}
