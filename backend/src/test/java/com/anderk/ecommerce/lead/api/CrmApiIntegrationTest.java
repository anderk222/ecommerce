package com.anderk.ecommerce.lead.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.anderk.ecommerce.TestcontainersConfiguration;

import org.junit.jupiter.api.BeforeEach;
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
 * The CRM of the leads (HU-21 to HU-25).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
class CrmApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private String token;

    @BeforeEach
    void signIn() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> login = this.restTemplate.exchange("/api/auth/login", HttpMethod.POST,
                new HttpEntity<>(Map.of("email", "admin@xtrim.ec", "password", "admin123"), headers),
                Map.class);
        this.token = (String) login.getBody().get("token");
    }

    @Test
    @DisplayName("Nuevo → Cerrado is rejected by the backend")
    void nuevoACerradoIsRejectedByTheBackend() {
        long id = this.idOf(this.createLead("Lead nuevo"));

        ResponseEntity<Map> response = this.patch("/api/admin/leads/" + id + "/status",
                Map.of("status", "CLOSED", "outcome", "WON"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("code", "INVALID_STATUS_TRANSITION");
    }

    @Test
    @DisplayName("the allowed transitions go through and are recorded")
    void theAllowedTransitionsGoThroughAndAreRecorded() {
        long id = this.idOf(this.createLead("Lead que avanza"));

        assertThat(this.patch("/api/admin/leads/" + id + "/status",
                Map.of("status", "IN_PROGRESS")).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(this.patch("/api/admin/leads/" + id + "/status",
                Map.of("status", "CLOSED", "outcome", "WON", "saleAmount", 120.5))
                .getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, Object> detail = this.get("/api/admin/leads/" + id);
        assertThat((Map<String, Object>) detail.get("lead")).containsEntry("status", "CLOSED");
        assertThat((List<?>) detail.get("statusHistory")).hasSize(2);
    }

    @Test
    @DisplayName("a lead can be created, edited and deleted")
    void aLeadCanBeCreatedEditedAndDeleted() {
        long id = this.idOf(this.createLead("Lead manual"));

        Map<String, Object> body = new HashMap<>();
        body.put("name", "Lead editado");
        body.put("phone", "0999999999");
        body.put("consent", true);
        ResponseEntity<Map> updated = this.put("/api/admin/leads/" + id, body);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody()).containsEntry("name", "Lead editado");

        ResponseEntity<Map> deleted = this.delete("/api/admin/leads/" + id);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        ResponseEntity<Map> gone = this.restTemplate.exchange(
                "/api/admin/leads/" + id, HttpMethod.GET, this.authorized(), Map.class);
        assertThat(gone.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("a note is written and read back")
    void aNoteIsWrittenAndReadBack() {
        long id = this.idOf(this.createLead("Lead con notas"));

        ResponseEntity<Map> note = this.post("/api/admin/leads/" + id + "/notes",
                Map.of("text", "Llamé al cliente."));
        assertThat(note.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Map<String, Object> detail = this.get("/api/admin/leads/" + id);
        assertThat((List<?>) detail.get("notes")).hasSize(1);
    }

    @Test
    @DisplayName("the timeline of a lead names the product of every event")
    void theTimelineNamesTheProducts() {
        ResponseEntity<Map> item = this.post("/api/admin/catalog", Map.of(
                "type", "PRODUCT",
                "name", "Térmico Alpina P35",
                "description", "Tester de la línea de tiempo",
                "price", 10.50,
                "stock", 5,
                "images", List.of("https://example.test/termico.png"),
                "specifications", List.of(),
                "categoryIds", List.of(),
                "featured", false));
        assertThat(item.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        long itemId = this.idOf(item.getBody());

        // The visitor browses, adds the item to the cart and asks for
        // information from the same browser: the capture links the whole
        // activity to the lead (HU-10) and the detail must read the product.
        String anonymousId = java.util.UUID.randomUUID().toString();

        ResponseEntity<Map> event = this.post("/api/public/events", Map.of(
                "anonymousId", anonymousId,
                "type", "ADD_TO_CART",
                "entityType", "CATALOG_ITEM",
                "entityId", itemId,
                "metadata", Map.of("quantity", 1, "via", "BUTTON")));
        assertThat(event.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        ResponseEntity<Map> capture = this.post("/api/public/info-requests", Map.of(
                "name", "Ana Términos",
                "email", "termino-" + System.nanoTime() + "@xtrim.ec",
                "province", "PICHINCHA",
                "consent", true,
                "anonymousId", anonymousId,
                "itemIds", List.of(itemId)));
        assertThat(capture.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        long leadId = ((Number) capture.getBody().get("leadId")).longValue();

        Map<String, Object> detail = this.get("/api/admin/leads/" + leadId);
        List<Map<String, Object>> timeline = (List<Map<String, Object>>) detail.get("timeline");
        assertThat(timeline).isNotEmpty();
        assertThat(timeline).anySatisfy(entry -> {
            assertThat(entry).containsEntry("type", "ADD_TO_CART");
            assertThat(entry).containsEntry("productName", "Térmico Alpina P35");
        });
    }

    @Test
    @DisplayName("the board has one column per state, with the count and the total")
    void theBoardHasOneColumnPerState() {
        long id = this.idOf(this.createLead("Lead del tablero"));
        this.patch("/api/admin/leads/" + id + "/status", Map.of("status", "IN_PROGRESS"));

        Map<String, Object> board = this.get("/api/admin/leads/kanban");
        List<Map<String, Object>> columns = (List<Map<String, Object>>) board.get("columns");

        assertThat(columns).hasSize(3);
        assertThat(columns).extracting(column -> column.get("status"))
                .containsExactly("NEW", "IN_PROGRESS", "CLOSED");
        assertThat(columns).allSatisfy(column -> assertThat(column).containsKey("count"));
    }

    @Test
    @DisplayName("the indicators answer the sales, the conversion and the potential")
    void theIndicatorsAnswerTheSalesTheConversionAndThePotential() {
        long id = this.idOf(this.createLead("Lead de los indicadores"));
        this.patch("/api/admin/leads/" + id + "/status", Map.of("status", "IN_PROGRESS"));
        this.patch("/api/admin/leads/" + id + "/status",
                Map.of("status", "CLOSED", "outcome", "WON", "saleAmount", 200));

        Map<String, Object> kpis = this.get("/api/admin/leads/kpis");

        assertThat(kpis).containsKey("closedSales");
        assertThat(kpis).containsKey("conversionRate");
        assertThat(kpis).containsKey("potentialAmount");
        assertThat(((Number) kpis.get("wonCount")).longValue()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("the listing filters by status and by the free text")
    void theListingFiltersByStatusAndByTheFreeText() {
        long id = this.idOf(this.createLead("Lead filtrable"));
        this.patch("/api/admin/leads/" + id + "/status", Map.of("status", "IN_PROGRESS"));

        Map<String, Object> inProgress = this.get("/api/admin/leads?status=IN_PROGRESS");
        List<Map<String, Object>> content = (List<Map<String, Object>>) inProgress.get("content");
        assertThat(content).allSatisfy(lead -> assertThat(lead).containsEntry("status", "IN_PROGRESS"));

        Map<String, Object> byText = this.get("/api/admin/leads?q=filtrable");
        assertThat(((Number) byText.get("totalElements")).longValue()).isGreaterThanOrEqualTo(1);
    }

    // --- the helpers --------------------------------------------------------

    private long idOf(Map<String, Object> body) {
        return ((Number) body.get("id")).longValue();
    }

    private Map<String, Object> createLead(String name) {
        ResponseEntity<Map> response = this.post("/api/admin/leads", Map.of(
                "name", name,
                "email", "lead-" + name.replace(' ', '-') + "-" + System.nanoTime() + "@xtrim.ec",
                "phone", "0999999999",
                "province", "PICHINCHA",
                "consent", true));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private Map<String, Object> get(String path) {
        return this.restTemplate.exchange(path, HttpMethod.GET, this.authorized(), Map.class).getBody();
    }

    private ResponseEntity<Map> post(String path, Map<String, Object> body) {
        return this.restTemplate.exchange(path, HttpMethod.POST, this.json(body), Map.class);
    }

    private ResponseEntity<Map> put(String path, Map<String, Object> body) {
        return this.restTemplate.exchange(path, HttpMethod.PUT, this.json(body), Map.class);
    }

    private ResponseEntity<Map> patch(String path, Map<String, Object> body) {
        return this.restTemplate.exchange(path, HttpMethod.PATCH, this.json(body), Map.class);
    }

    private ResponseEntity<Map> delete(String path) {
        return this.restTemplate.exchange(path, HttpMethod.DELETE, this.authorized(), Map.class);
    }

    private HttpEntity<Map<String, Object>> json(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(this.token);
        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<Void> authorized() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(this.token);
        return new HttpEntity<>(headers);
    }
}
