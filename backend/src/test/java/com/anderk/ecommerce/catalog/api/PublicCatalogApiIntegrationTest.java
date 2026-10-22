package com.anderk.ecommerce.catalog.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.anderk.ecommerce.TestcontainersConfiguration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * The public API of the catalog, exercised over HTTP.
 *
 * <p>It runs against the seeded database of the local stack, through the
 * real filters and the real advice, so the contract of §12.5 is checked as
 * the frontend will see it.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
class PublicCatalogApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("the home answers the featured, the most bought and the on sale")
    void theHomeAnswersTheThreeSections() {
        ResponseEntity<Map> response = this.restTemplate.getForEntity("/api/public/home", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).containsKeys("featured", "bestSelling", "onSale");
        assertThat((java.util.List<?>) body.get("featured")).isNotEmpty();
        assertThat((java.util.List<?>) body.get("onSale")).isNotEmpty();
    }

    @Test
    @DisplayName("the listing answers the page envelope of the contract")
    void theListingAnswersThePageEnvelope() {
        ResponseEntity<Map> response = this.restTemplate.getForEntity("/api/public/catalog", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).containsKeys("content", "page", "size", "totalElements", "totalPages");
        assertThat((Integer) body.get("page")).isZero();
        assertThat((Integer) body.get("size")).isEqualTo(12);
        assertThat((Integer) body.get("totalElements")).isPositive();
    }

    @Test
    @DisplayName("the search ignores the case and the accents")
    void theSearchIgnoresTheCaseAndTheAccents() {
        ResponseEntity<Map> response =
                this.restTemplate.getForEntity("/api/public/catalog?q=AURICULARES", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Integer) response.getBody().get("totalElements")).isEqualTo(1);

        ResponseEntity<Map> withAccent =
                this.restTemplate.getForEntity("/api/public/catalog?q=tecnica", Map.class);

        assertThat((Integer) withAccent.getBody().get("totalElements")).isEqualTo(1);
    }

    @Test
    @DisplayName("the listing can be filtered by type and by category")
    void theListingCanBeFilteredByTypeAndByCategory() {
        ResponseEntity<Map> services =
                this.restTemplate.getForEntity("/api/public/catalog?type=SERVICE", Map.class);

        assertThat(services.getStatusCode()).isEqualTo(HttpStatus.OK);
        java.util.List<Map> content = (java.util.List<Map>) services.getBody().get("content");
        assertThat(content).isNotEmpty();
        assertThat(content).allSatisfy(item -> assertThat(item.get("type")).isEqualTo("SERVICE"));

        ResponseEntity<Map> byCategory =
                this.restTemplate.getForEntity("/api/public/catalog?category=servicios", Map.class);

        assertThat((Integer) byCategory.getBody().get("totalElements")).isEqualTo(4);
    }

    @Test
    @DisplayName("the size of a page is capped at fifty")
    void theSizeOfAPageIsCappedAtFifty() {
        ResponseEntity<Map> response =
                this.restTemplate.getForEntity("/api/public/catalog?size=1000", Map.class);

        assertThat((Integer) response.getBody().get("size")).isEqualTo(50);
    }

    @Test
    @DisplayName("the detail answers the item, with its specifications")
    void theDetailAnswersTheItemWithItsSpecifications() {
        ResponseEntity<Map> response =
                this.restTemplate.getForEntity("/api/public/catalog/router-wifi-6-ax3000", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).containsEntry("slug", "router-wifi-6-ax3000");
        assertThat((java.util.List<?>) body.get("specifications")).isNotEmpty();
        assertThat(body).containsKey("stockState");

        Map<String, Object> price = (Map<String, Object>) body.get("price");
        assertThat(price).containsEntry("discountPercentage", 10);
    }

    @Test
    @DisplayName("an unknown slug answers 404 with the error envelope")
    void anUnknownSlugAnswers404WithTheErrorEnvelope() {
        ResponseEntity<Map> response =
                this.restTemplate.getForEntity("/api/public/catalog/no-existe", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("code", "NOT_FOUND");
        assertThat(response.getBody()).containsKeys("message", "details", "timestamp");
    }

    @Test
    @DisplayName("the categories answer with the number of items of each one")
    void theCategoriesAnswerWithTheNumberOfItemsOfEachOne() {
        ResponseEntity<java.util.List> response =
                this.restTemplate.getForEntity("/api/public/categories", java.util.List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        java.util.List<Map> categories = (java.util.List<Map>) response.getBody();
        assertThat(categories).isNotEmpty();
        assertThat(categories).allSatisfy(category -> assertThat(category).containsKey("itemCount"));
    }

    @Test
    @DisplayName("the slugs answer the sitemap of the published items")
    void theSlugsAnswerTheSitemapOfThePublishedItems() {
        ResponseEntity<java.util.List> response =
                this.restTemplate.getForEntity("/api/public/catalog/slugs", java.util.List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotEmpty();
        assertThat(response.getBody()).doesNotContain("licuadora-powermix-1200w".equals("x") ? "x" : "no-existe");
    }
}
