package com.example.tickets.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

import java.time.Instant;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TicketApiIT {

    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("tickets").withUsername("postgres").withPassword("postgres");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
        r.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    private long createTicket() throws Exception {
        var body = om.writeValueAsString(Map.of(
                "title", "printer",
                "description", "no paper",
                "categoryId", 1,
                "priority", "HIGH",
                "dueAt", Instant.now().plusSeconds(3600).toString()
        ));
        String res = mvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return om.readTree(res).get("id").asLong();
    }

    @Test void migrationAndSeedApplied() throws Exception {
        mvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));
    }

    @Test void createReturnsNewWithHistory() throws Exception {
        long id = createTicket();
        mvc.perform(get("/api/tickets/" + id + "/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].toStatus").value("NEW"));
    }

    @Test void unknownIdReturns404() throws Exception {
        mvc.perform(get("/api/tickets/999999")).andExpect(status().isNotFound());
    }

    @Test void illegalTransitionReturns409AndKeepsData() throws Exception {
        long id = createTicket();
        mvc.perform(patch("/api/tickets/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/tickets/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test void filterByStatusAndPriority() throws Exception {
        createTicket();
        mvc.perform(get("/api/tickets?status=NEW&priority=HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test void reportEndpointReturnsSummary() throws Exception {
        createTicket();
        mvc.perform(get("/api/reports/summary")
                        .param("from", Instant.now().minusSeconds(3600).toString())
                        .param("to", Instant.now().plusSeconds(3600).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCreated").exists())
                .andExpect(jsonPath("$.byStatus").exists());
    }
}