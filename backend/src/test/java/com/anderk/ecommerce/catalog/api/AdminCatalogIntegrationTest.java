package com.anderk.ecommerce.catalog.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import com.anderk.ecommerce.TestcontainersConfiguration;

import jakarta.persistence.EntityManager;

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
 * The administration of the catalogue (HU-14 to HU-19).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
class AdminCatalogIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private EntityManager entityManager;

    /** The token of the administrator: the routes of the panel need it. */
    private String token;

    @org.junit.jupiter.api.BeforeEach
    void signIn() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> login = this.restTemplate.exchange("/api/auth/login", HttpMethod.POST,
                new HttpEntity<>(Map.of("email", "admin@xtrim.ec", "password", "admin123"), headers),
                Map.class);
        this.token = (String) login.getBody().get("token");
    }

    @Test
    @DisplayName("creating an item produces a slug")
    void creatingAnItemProducesASlug() {
        ResponseEntity<Map> created = this.post("/api/admin/catalog", Map.of(
                "type", "PRODUCT",
                "name", "Ítem de administración",
                "description", "Creado por la prueba.",
                "price", 25.5,
                "stock", 3,
                "images", List.of("https://example.test/one.png"),
                "specifications", List.of(Map.of("name", "Color", "value", "Azul")),
                "categoryIds", List.of(),
                "featured", true));

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> body = created.getBody();
        assertThat(body).containsEntry("slug", "item-de-administracion");
        assertThat(body).containsEntry("name", "Ítem de administración");
    }

    @Test
    @DisplayName("editing an item changes its price")
    void editingAnItemChangesItsPrice() {
        long id = this.idOf(this.createItem("Ítem editable"));

        ResponseEntity<Map> edited = this.put("/api/admin/catalog/" + id, Map.of(
                "type", "PRODUCT",
                "name", "Ítem editable",
                "description", "Otro precio.",
                "price", 99.99,
                "stock", 3,
                "images", List.of(),
                "specifications", List.of(),
                "categoryIds", List.of(),
                "featured", false));

        assertThat(edited.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> price = (Map<String, Object>) edited.getBody().get("price");
        assertThat(price).containsEntry("basePrice", 99.99);
    }

    @Test
    @DisplayName("an inactive item does not answer in the public API")
    void anInactiveItemDoesNotAnswerInThePublicApi() {
        long id = this.idOf(this.createItem("Ítem que se apaga"));
        String slug = this.slugOf(id);

        ResponseEntity<Void> deactivated = this.patch("/api/admin/catalog/" + id + "/active?active=false");
        assertThat(deactivated.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<Map> published =
                this.restTemplate.getForEntity("/api/public/catalog/" + slug, Map.class);
        assertThat(published.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        this.patch("/api/admin/catalog/" + id + "/active?active=true");
        ResponseEntity<Map> reactivated =
                this.restTemplate.getForEntity("/api/public/catalog/" + slug, Map.class);
        assertThat(reactivated.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("a deleted item is not published, but the row survives")
    void aDeletedItemIsNotPublishedButTheRowSurvives() {
        long id = this.idOf(this.createItem("Ítem que se elimina"));
        String slug = this.slugOf(id);

        ResponseEntity<Void> deleted = this.delete("/api/admin/catalog/" + id);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<Map> published =
                this.restTemplate.getForEntity("/api/public/catalog/" + slug, Map.class);
        assertThat(published.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // The row survives: the deletion is logical.
        assertThat(this.slugOf(id)).isEqualTo(slug);
    }

    @Test
    @DisplayName("the listing of the administration filters by state")
    void theListingOfTheAdministrationFiltersByState() {
        long id = this.idOf(this.createItem("Ítem inactivo"));
        this.patch("/api/admin/catalog/" + id + "/active?active=false");

        ResponseEntity<Map> inactive = this.restTemplate.exchange(
                "/api/admin/catalog?state=INACTIVE&size=50", HttpMethod.GET, this.authorized(), Map.class);
        assertThat(inactive.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(this.rowsOf(inactive)).anySatisfy(row -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> item = (Map<String, Object>) row;
            assertThat(item.get("active")).isEqualTo(false);
        });

        ResponseEntity<Map> active = this.restTemplate.exchange(
                "/api/admin/catalog?state=ACTIVE&size=50", HttpMethod.GET, this.authorized(), Map.class);
        assertThat(this.rowsOf(active)).noneSatisfy(row -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> item = (Map<String, Object>) row;
            assertThat(((Number) item.get("id")).longValue()).isEqualTo(id);
        });
    }

    @Test
    @DisplayName("a discount is applied and revoked")
    void aDiscountIsAppliedAndRevoked() {
        long id = this.idOf(this.createItem("Ítem con descuento"));

        java.util.Map<String, Object> discount = new java.util.HashMap<>();
        discount.put("percentage", 30);
        discount.put("startsAt", null);
        discount.put("endsAt", null);
        ResponseEntity<Map> applied = this.put("/api/admin/catalog/" + id + "/discount", discount);
        assertThat(applied.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<Map> onSale =
                this.restTemplate.getForEntity("/api/public/catalog?onSale=true&size=50", Map.class);
        assertThat(this.rowsOf(onSale)).anySatisfy(row -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> item = (Map<String, Object>) row;
            assertThat(((Number) item.get("id")).longValue()).isEqualTo(id);
        });

        ResponseEntity<Void> revoked = this.delete("/api/admin/catalog/" + id + "/discount");
        assertThat(revoked.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("the edition reads the item back with every field the form edits")
    void theEditionReadsTheItemBackWithEveryField() {
        ResponseEntity<Map> category = this.post("/api/admin/categories", Map.of("name", "Redes"));
        assertThat(category.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        long categoryId = ((Number) category.getBody().get("id")).longValue();

        ResponseEntity<Map> created = this.post("/api/admin/catalog", java.util.Map.of(
                "type", "PRODUCT",
                "name", "Ítem de lectura completa",
                "description", "Toda la descripción.",
                "price", 25.5,
                "stock", 3,
                "images", List.of("https://example.test/one.png", "https://example.test/two.png"),
                "specifications", List.of(Map.of("name", "Color", "value", "Azul")),
                "categoryIds", List.of(categoryId),
                "featured", true));
        long id = this.idOf(created.getBody());

        ResponseEntity<Map> read = this.restTemplate.exchange(
                "/api/admin/catalog/" + id, HttpMethod.GET, this.authorized(), Map.class);
        assertThat(read.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = read.getBody();
        assertThat(body).containsEntry("name", "Ítem de lectura completa")
                .containsEntry("description", "Toda la descripción.")
                .containsEntry("type", "PRODUCT")
                .containsEntry("featured", true);
        assertThat(((Number) body.get("stock")).intValue()).isEqualTo(3);
        assertThat(body.get("images"))
                .isEqualTo(List.of("https://example.test/one.png", "https://example.test/two.png"));
        assertThat(body.get("specifications")).isEqualTo(List.of(Map.of("name", "Color", "value", "Azul")));
        Number firstCategoryId = (Number) ((List<?>) body.get("categoryIds")).get(0);
        assertThat(firstCategoryId.longValue()).isEqualTo(categoryId);

        ResponseEntity<Map> missing = this.restTemplate.exchange(
                "/api/admin/catalog/999999", HttpMethod.GET, this.authorized(), Map.class);
        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // --- the helpers --------------------------------------------------------

    private Map<String, Object> createItem(String name) {
        ResponseEntity<Map> created = this.post("/api/admin/catalog", Map.of(
                "type", "PRODUCT",
                "name", name,
                "description", "Creado por la prueba.",
                "price", 10.0,
                "stock", 2,
                "images", List.of(),
                "specifications", List.of(),
                "categoryIds", List.of(),
                "featured", false));
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return created.getBody();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> rowsOf(ResponseEntity<Map> response) {
        Object content = response.getBody().get("content");
        return (List<Map<String, Object>>) content;
    }

    private long idOf(Map<String, Object> body) {
        return ((Number) body.get("id")).longValue();
    }

    private String slugOf(long id) {
        return (String) this.entityManager
                .createQuery("select i.slug from CatalogItemJpaEntity i where i.id = :id", String.class)
                .setParameter("id", id)
                .getSingleResult();
    }

    private HttpEntity<Map<String, Object>> json(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(this.token);
        return new HttpEntity<>(body, headers);
    }

    private ResponseEntity<Map> post(String path, Map<String, Object> body) {
        return this.restTemplate.exchange(path, HttpMethod.POST, this.json(body), Map.class);
    }

    private ResponseEntity<Map> put(String path, Map<String, Object> body) {
        return this.restTemplate.exchange(path, HttpMethod.PUT, this.json(body), Map.class);
    }

    private ResponseEntity<Void> patch(String path) {
        ResponseEntity<Map> response = this.restTemplate.exchange(path, HttpMethod.PATCH,
                this.authorized(), Map.class);
        return new ResponseEntity<>(response.getStatusCode());
    }

    private ResponseEntity<Void> delete(String path) {
        ResponseEntity<Map> response = this.restTemplate.exchange(path, HttpMethod.DELETE,
                this.authorized(), Map.class);
        return new ResponseEntity<>(response.getStatusCode());
    }

    private HttpEntity<Void> authorized() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(this.token);
        return new HttpEntity<>(headers);
    }
}
