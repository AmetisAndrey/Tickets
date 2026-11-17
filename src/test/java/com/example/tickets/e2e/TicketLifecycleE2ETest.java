package com.example.tickets.e2e;

import com.example.tickets.e2e.MutableClockConfig.MutableClock;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Import(MutableClockConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TicketLifecycleE2ETest {

    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("tickets")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
        r.add("spring.flyway.enabled", () -> "true");
    }

    @LocalServerPort int port;
    @Autowired TestRestTemplate http;
    @Autowired ObjectMapper om;
    @Autowired MutableClock clock;

    private String base() { return "http://localhost:" + port; }

    // ---------- helpers ----------

    private JsonNode createTicket(String title) {
        var body = Map.of(
                "title", title,
                "description", "e2e description",
                "categoryId", 1,
                "priority", "HIGH",
                "dueAt", Instant.parse("2025-01-15T18:00:00Z").toString()
        );
        ResponseEntity<String> res = http.postForEntity(
                base() + "/api/tickets",
                new HttpEntity<>(body, jsonHeaders()),
                String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return read(res.getBody());
    }

    private ResponseEntity<String> changeStatus(long id, String status) {
        var body = Map.of("status", status);
        return http.exchange(
                base() + "/api/tickets/" + id + "/status",
                HttpMethod.PATCH,
                new HttpEntity<>(body, jsonHeaders()),
                String.class);
    }

    private JsonNode getTicket(long id) {
        ResponseEntity<String> res = http.getForEntity(base() + "/api/tickets/" + id, String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        return read(res.getBody());
    }

    private JsonNode history(long id) {
        ResponseEntity<String> res = http.getForEntity(
                base() + "/api/tickets/" + id + "/history", String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        return read(res.getBody());
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private JsonNode read(String s) {
        try { return om.readTree(s); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    @Order(1)
    @DisplayName("E2E-1: NEW -> IN_PROGRESS -> RESOLVED, история полная, closedAt выставлен")
    void fullLifecycle() {
        clock.set(Instant.parse("2025-01-15T12:00:00Z"));

        JsonNode created = createTicket("lifecycle printer");
        long id = created.get("id").asLong();
        assertThat(created.get("status").asText()).isEqualTo("NEW");
        assertThat(created.get("createdAt").asText()).isEqualTo("2025-01-15T12:00:00Z");
        assertThat(created.hasNonNull("closedAt")).isFalse();

        // 2) NEW -> IN_PROGRESS (через 10 минут)
        clock.set(Instant.parse("2025-01-15T12:10:00Z"));
        ResponseEntity<String> r1 = changeStatus(id, "IN_PROGRESS");
        assertThat(r1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(read(r1.getBody()).get("status").asText()).isEqualTo("IN_PROGRESS");

        clock.set(Instant.parse("2025-01-15T12:40:00Z"));
        ResponseEntity<String> r2 = changeStatus(id, "RESOLVED");
        assertThat(r2.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode resolved = read(r2.getBody());
        assertThat(resolved.get("status").asText()).isEqualTo("RESOLVED");
        assertThat(resolved.get("closedAt").asText()).isEqualTo("2025-01-15T12:40:00Z");

        JsonNode hist = history(id);
        assertThat(hist).hasSize(3);
        assertThat(hist.get(0).get("fromStatus").isNull()).isTrue();
        assertThat(hist.get(0).get("toStatus").asText()).isEqualTo("NEW");
        assertThat(hist.get(1).get("fromStatus").asText()).isEqualTo("NEW");
        assertThat(hist.get(1).get("toStatus").asText()).isEqualTo("IN_PROGRESS");
        assertThat(hist.get(2).get("fromStatus").asText()).isEqualTo("IN_PROGRESS");
        assertThat(hist.get(2).get("toStatus").asText()).isEqualTo("RESOLVED");
    }

    @Test
    @Order(2)
    @DisplayName("E2E-2: RESOLVED -> IN_PROGRESS даёт 409, статус/closedAt/история не меняются")
    void forbiddenReopenKeepsDataIntact() {
        clock.set(Instant.parse("2025-01-15T13:00:00Z"));

        // Доводим заявку до RESOLVED
        JsonNode created = createTicket("lifecycle forbidden");
        long id = created.get("id").asLong();

        clock.set(Instant.parse("2025-01-15T13:05:00Z"));
        assertThat(changeStatus(id, "IN_PROGRESS").getStatusCode()).isEqualTo(HttpStatus.OK);

        clock.set(Instant.parse("2025-01-15T13:20:00Z"));
        ResponseEntity<String> resolvedRes = changeStatus(id, "RESOLVED");
        assertThat(resolvedRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Снимаем "слепок" до запрещённой операции
        JsonNode before = getTicket(id);
        JsonNode histBefore = history(id);
        String closedAtBefore = before.get("closedAt").asText();
        int histSizeBefore = histBefore.size();

        // Пытаемся открыть заново — ожидаем 409
        clock.set(Instant.parse("2025-01-15T13:30:00Z"));
        ResponseEntity<String> forbidden = changeStatus(id, "IN_PROGRESS");
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        JsonNode err = read(forbidden.getBody());
        assertThat(err.get("status").asInt()).isEqualTo(409);
        assertThat(err.get("error").asText()).isEqualTo("Conflict");
        assertThat(err.get("path").asText()).isEqualTo("/api/tickets/" + id + "/status");

        JsonNode after = getTicket(id);
        assertThat(after.get("status").asText()).isEqualTo("RESOLVED");
        assertThat(after.get("closedAt").asText()).isEqualTo(closedAtBefore);
        assertThat(after.get("dueAt").asText()).isEqualTo(before.get("dueAt").asText());

        JsonNode histAfter = history(id);
        assertThat(histAfter).hasSize(histSizeBefore);
    }
}