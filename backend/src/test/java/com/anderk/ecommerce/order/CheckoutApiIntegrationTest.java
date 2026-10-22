package com.anderk.ecommerce.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import com.anderk.ecommerce.TestcontainersConfiguration;

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

/**
 * The purchase, over HTTP.
 *
 * <p>The use case is covered by {@link CheckoutIntegrationTest}; this one
 * covers the **contract**: the body that the frontend sends, and the answer
 * that it reads.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
class CheckoutApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("the purchase answers with the order, the lead and the total")
    void thePurchaseAnswersWithTheOrderTheLeadAndTheTotal() {
        ResponseEntity<Map> response = this.restTemplate.exchange("/api/public/orders", HttpMethod.POST,
                this.json(Map.of(
                        "anonymousId", "dddddddd-bbbb-4ccc-8ddd-eeeeeeeeeeee",
                        "customer", Map.of(
                                "name", "Ana Pérez",
                                "email", "ana.api@ejemplo.ec",
                                "phone", "0999999999",
                                "province", "PICHINCHA",
                                "consent", true),
                        "items", List.of(Map.of("itemId", 1, "quantity", 1)))),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> body = response.getBody();
        assertThat(body).containsKey("orderId");
        assertThat(body).containsKey("leadId");
        assertThat(body).containsEntry("currency", "USD");
        assertThat(((Number) body.get("total")).doubleValue()).isPositive();
        assertThat((List<?>) body.get("lines")).hasSize(1);
    }

    @Test
    @DisplayName("a purchase without items is refused")
    void aPurchaseWithoutItemsIsRefused() {
        ResponseEntity<Map> response = this.restTemplate.exchange("/api/public/orders", HttpMethod.POST,
                this.json(Map.of(
                        "customer", Map.of("name", "Ana", "email", "vacio@ejemplo.ec", "consent", true),
                        "items", List.of())),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("code", "VALIDATION_ERROR");
    }

    @Test
    @DisplayName("a purchase without the consent is refused")
    void aPurchaseWithoutTheConsentIsRefused() {
        ResponseEntity<Map> response = this.restTemplate.exchange("/api/public/orders", HttpMethod.POST,
                this.json(Map.of(
                        "customer", Map.of("name", "Ana", "email", "sin-consentimiento@ejemplo.ec",
                                "consent", false),
                        "items", List.of(Map.of("itemId", 1, "quantity", 1)))),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("code", "VALIDATION_ERROR");
    }

    private HttpEntity<Map<String, Object>> json(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
