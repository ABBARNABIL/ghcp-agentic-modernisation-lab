package com.contoso.demo.orderservice.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 3 (verify-test-baseline) — {@code *PostMigrationIT} test.
 *
 * <p>Materializes the Order API contract frozen in
 * {@code src/test/test-cases/test-cases.md} (sections 2 and 3, table rows for
 * {@code OrderControllerTest}) against the migrated Java 25 / Spring Boot
 * 4.1.1 implementation. Unlike the pre-existing {@code OrderControllerTest}
 * ({@code @WebMvcTest} with a mocked {@code OrderService}), this test boots
 * the full application on a random port and exercises the real HTTP
 * pipeline, real {@code OrderService}, and the real embedded H2 database
 * end-to-end — the MOCKED/embedded external-dependency strategy frozen in
 * section 7 of the baseline (no live Azure resource, credential, or
 * connection is used or required anywhere in this class).</p>
 *
 * <p>This file is append-only per the Phase 3 contract: it does not modify,
 * replace, or shadow {@code OrderControllerTest} or any other frozen/existing
 * test; it adds new full-stack coverage of the same frozen scenarios.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class OrderApiPostMigrationIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String url(String path) {
        return "http://127.0.0.1:" + port + "/api/orders" + path;
    }

    // --- Frozen scenario: listReturnsAllOrders --------------------------------------------

    @Test
    void listReturnsAllOrders_includesSeededAliceOrder() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity(url(""), String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertTrue(body.isArray());
        assertTrue(body.size() >= 3, "expected at least the 3 DataSeeder rows to be present");

        boolean hasSeededAlicePending = false;
        for (JsonNode order : body) {
            if ("alice".equals(order.get("customer").asText())
                    && "PENDING".equals(order.get("status").asText())
                    && new java.math.BigDecimal(order.get("amount").asText()).compareTo(new java.math.BigDecimal("120.50")) == 0) {
                hasSeededAlicePending = true;
            }
        }
        assertTrue(hasSeededAlicePending, "expected the seeded alice/PENDING/120.50 DataSeeder row from testdata/seeded-orders.json");
    }

    // --- Frozen scenario: getByIdReturns404WhenMissing -------------------------------------

    @Test
    void getByIdReturns404WhenMissing() {
        ResponseEntity<String> response = restTemplate.getForEntity(url("/999999"), String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // --- Frozen scenario: totalForCustomerReturnsValue -------------------------------------
    // NOTE: unlike the frozen OrderControllerTest (which mocks OrderService and asserts a
    // stubbed "200.50"), this full-stack IT creates its own orders for a dedicated customer
    // rather than asserting against the DataSeeder-seeded "alice" total. This avoids a latent,
    // pre-existing (not migration-related) fragility of the app's named in-memory database
    // (jdbc:h2:mem:orders;DB_CLOSE_DELAY=-1): when more than one full @SpringBootTest context
    // configuration boots in the same test JVM (e.g. this RANDOM_PORT context alongside the
    // default-web-environment context used elsewhere), each context's DataSeeder CommandLineRunner
    // re-inserts the same 3 seed rows into the same shared named database, so the seeded
    // "alice" total is not deterministic across the full multi-class test run. Creating a
    // dedicated customer keeps this scenario deterministic while still proving the real sum
    // is computed end-to-end via the real repository/H2, exactly as the frozen contract requires.
    @Test
    void totalForCustomerSumsAmountsOfOwnOrders() throws Exception {
        createOrder("kate-postmigration-total", "120.50");
        createOrder("kate-postmigration-total", "80.00");

        ResponseEntity<String> response = restTemplate.getForEntity(
                url("/customer/kate-postmigration-total/total"), String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("200.50", response.getBody());
    }

    // --- Frozen contract: total endpoint returns 0 (not an error) for unknown customer ----

    @Test
    void totalForCustomerReturnsZeroWhenNoOrders() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                url("/customer/no-such-customer-postmigration/total"), String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("0", response.getBody());
    }

    // --- Frozen scenario: createReturns201 (status always forced to PENDING) --------------

    @Test
    void createReturns201AndForcesPendingStatusRegardlessOfRequestBody() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Requests an initial status of COMPLETED; the frozen contract requires the
        // server to force PENDING regardless of the caller-supplied value.
        HttpEntity<String> request = new HttpEntity<>(
                "{\"customer\":\"dave-postmigration\",\"amount\":15.00,\"status\":\"COMPLETED\"}", headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url(""), request, String.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals("dave-postmigration", body.get("customer").asText());
        assertEquals("PENDING", body.get("status").asText());
        assertNotNull(body.get("createdAt").asText(null), "createdAt must be populated via @PrePersist");
        assertNotNull(body.get("id"), "id must be assigned via IDENTITY generation");
    }

    // --- Frozen scenario: createRejectsInvalidPayload --------------------------------------

    @Test
    void createRejectsInvalidEmptyPayload() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>("{}", headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url(""), request, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // --- Frozen scenario: createRejectsBlankCustomerAndNonPositiveAmount -------------------

    @Test
    void createRejectsBlankCustomerAndNonPositiveAmount() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>("{\"customer\":\"   \",\"amount\":0}", headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url(""), request, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // --- Frozen scenario: updateStatusReturnsUpdatedOrder ----------------------------------

    @Test
    void updateStatusReturnsUpdatedOrder() throws Exception {
        Long createdId = createOrder("erin-postmigration", "22.00");

        ResponseEntity<String> response = restTemplate.exchange(
                url("/" + createdId + "/status?status=PROCESSING"),
                HttpMethod.PATCH, HttpEntity.EMPTY, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals("PROCESSING", body.get("status").asText());
    }

    // --- Frozen scenario: updateStatusReturns404WhenOrderIsMissing ------------------------

    @Test
    void updateStatusReturns404WhenOrderIsMissing() {
        ResponseEntity<String> response = restTemplate.exchange(
                url("/999999/status?status=COMPLETED"),
                HttpMethod.PATCH, HttpEntity.EMPTY, String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // --- Frozen scenario: updateStatusRejectsUnknownStatus (enum bind failure -> 400) -----

    @Test
    void updateStatusRejectsUnknownStatus() throws Exception {
        Long createdId = createOrder("frank-postmigration", "9.00");

        ResponseEntity<String> response = restTemplate.exchange(
                url("/" + createdId + "/status?status=UNKNOWN"),
                HttpMethod.PATCH, HttpEntity.EMPTY, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // --- Frozen scenario (section 5): CORS registered for /api/** with GET/POST/PATCH ----

    @Test
    void corsPreflightAllowsConfiguredOriginAndMethodsForApiRoutes() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Origin", "https://example-postmigration-client.test");
        headers.set("Access-Control-Request-Method", "PATCH");
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url(""), HttpMethod.OPTIONS, request, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("*", response.getHeaders().getFirst("Access-Control-Allow-Origin"));
        String allowedMethods = response.getHeaders().getFirst("Access-Control-Allow-Methods");
        assertNotNull(allowedMethods);
        assertTrue(allowedMethods.contains("PATCH"));
    }

    private Long createOrder(String customer, String amount) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(
                "{\"customer\":\"" + customer + "\",\"amount\":" + amount + "}", headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url(""), request, String.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        JsonNode body = objectMapper.readTree(response.getBody());
        return body.get("id").asLong();
    }
}
