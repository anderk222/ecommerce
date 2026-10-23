package com.anderk.ecommerce.auth.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.anderk.ecommerce.TestcontainersConfiguration;
import com.anderk.ecommerce.auth.application.out.TokenService;
import com.anderk.ecommerce.auth.domain.model.AuthenticatedUser;
import com.anderk.ecommerce.domain.model.vo.Email;

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
 * The login of the staff and the rules of the routes (HU-13).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
class AuthApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TokenService tokenService;

    @Test
    @DisplayName("the administrator signs in and receives a token with the role")
    void theAdministratorSignsInAndReceivesATokenWithTheRole() {
        ResponseEntity<Map> response = this.login("admin@xtrim.ec", "admin123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).containsEntry("role", "ADMIN");
        assertThat(body).containsEntry("email", "admin@xtrim.ec");
        assertThat((String) body.get("token")).isNotBlank();
    }

    @Test
    @DisplayName("a wrong password answers 401")
    void aWrongPasswordAnswers401() {
        ResponseEntity<Map> response = this.login("admin@xtrim.ec", "no-es-la-clave");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).containsEntry("code", "UNAUTHORIZED");
    }

    @Test
    @DisplayName("an unknown user answers 401")
    void anUnknownUserAnswers401() {
        ResponseEntity<Map> response = this.login("nadie@xtrim.ec", "admin123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("a route of the panel without a token answers 401")
    void aRouteOfThePanelWithoutATokenAnswers401() {
        ResponseEntity<Map> response =
                this.restTemplate.getForEntity("/api/admin/categories", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).containsEntry("code", "UNAUTHORIZED");
    }

    @Test
    @DisplayName("the administrator reaches the routes of the panel")
    void theAdministratorReachesTheRoutesOfThePanel() {
        String token = (String) this.login("admin@xtrim.ec", "admin123").getBody().get("token");

        ResponseEntity<java.util.List> response = this.restTemplate.exchange(
                "/api/admin/categories", HttpMethod.GET, authorized(token), java.util.List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("a user without the role ADMIN does not reach the routes of the administration")
    void aUserWithoutTheRoleAdminDoesNotReachTheRoutesOfTheAdministration() {
        // The role AUDITOR is retired with the audit trail (HU-31), but the
        // rule of the routes still needs a witness: a token that carries any
        // other role is refused.
        String token = this.tokenService.issue(
                new AuthenticatedUser(1L, new Email("sin-rol@xtrim.ec"), "Sin rol", "LECTOR"));

        ResponseEntity<Map> response = this.restTemplate.exchange(
                "/api/admin/categories", HttpMethod.GET, authorized(token), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("code", "FORBIDDEN");
    }

    @Test
    @DisplayName("a token that is not valid answers 401")
    void aTokenThatIsNotValidAnswers401() {
        ResponseEntity<Map> response = this.restTemplate.exchange(
                "/api/admin/categories", HttpMethod.GET, authorized("no-es-un-token"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("the public API does not need a token")
    void thePublicApiDoesNotNeedAToken() {
        ResponseEntity<Map> response = this.restTemplate.getForEntity("/api/public/home", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private ResponseEntity<Map> login(String email, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return this.restTemplate.exchange("/api/auth/login", HttpMethod.POST,
                new HttpEntity<>(Map.of("email", email, "password", password), headers), Map.class);
    }

    private HttpEntity<Void> authorized(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }
}
