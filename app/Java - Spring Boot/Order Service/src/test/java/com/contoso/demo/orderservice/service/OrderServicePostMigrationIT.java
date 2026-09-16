package com.contoso.demo.orderservice.service;

import com.contoso.demo.orderservice.model.Order;
import com.contoso.demo.orderservice.model.OrderStatus;
import com.contoso.demo.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 3 (verify-test-baseline) — {@code *PostMigrationIT} test.
 *
 * <p>Re-proves the {@code OrderService} scenarios frozen in
 * {@code src/test/test-cases/test-cases.md} section 2 (originally split
 * across {@code OrderServiceUnitTest} — Mockito-based — and
 * {@code OrderServiceTest} — full Spring context) against the migrated Java
 * 25 / Spring Boot 4.1.1 implementation. This class exercises the real
 * Spring-managed {@code OrderService} bean wired to the real
 * {@code OrderRepository} backed by the embedded H2 in-memory database (the
 * MOCKED/embedded external-dependency strategy frozen in section 7 — no
 * Azure resource or credential is used or required), asserting outcomes by
 * inspecting actual persisted state rather than by verifying mock
 * interactions, which is a strictly stronger proof of the same frozen
 * behavior contract.</p>
 *
 * <p>Append-only: does not modify or replace {@code OrderServiceUnitTest} or
 * {@code OrderServiceTest}.</p>
 */
@SpringBootTest
class OrderServicePostMigrationIT {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void totalForCustomerReturnsZeroWhenNoOrders() {
        BigDecimal total = orderService.totalForCustomer("nobody-postmigration-svc");

        assertEquals(BigDecimal.ZERO, total);
    }

    @Test
    void totalForCustomerSumsAllAmounts() {
        orderRepository.save(new Order("alice-postmigration-svc", new BigDecimal("120.50")));
        orderRepository.save(new Order("alice-postmigration-svc", new BigDecimal("80.00")));

        BigDecimal total = orderService.totalForCustomer("alice-postmigration-svc");

        assertEquals(new BigDecimal("200.50"), total);
    }

    @Test
    void totalForCustomerSumsAmountsAcrossFullSpringContext() {
        orderRepository.save(new Order("charlie-postmigration-svc", new BigDecimal("10.00")));
        orderRepository.save(new Order("charlie-postmigration-svc", new BigDecimal("15.50")));

        BigDecimal total = orderService.totalForCustomer("charlie-postmigration-svc");

        assertEquals(new BigDecimal("25.50"), total);
    }

    @Test
    void findByIdDelegatesToRepository() {
        Order saved = orderRepository.save(new Order("bob-postmigration-svc", new BigDecimal("42.99")));

        Optional<Order> result = orderService.findById(saved.getId());

        assertTrue(result.isPresent());
        assertEquals("bob-postmigration-svc", result.get().getCustomer());
    }

    @Test
    void createPersistsOrderAndForcesPendingStatus() {
        Order order = new Order("carol-postmigration-svc", new BigDecimal("10.00"));
        order.setStatus(OrderStatus.COMPLETED);

        Order created = orderService.create(order);

        assertEquals(OrderStatus.PENDING, created.getStatus());
        Optional<Order> reloaded = orderRepository.findById(created.getId());
        assertTrue(reloaded.isPresent());
        assertEquals(OrderStatus.PENDING, reloaded.get().getStatus());
    }

    @Test
    void updateStatusPersistsTheRequestedStatus() {
        Order saved = orderRepository.save(new Order("dave-postmigration-svc", new BigDecimal("15.00")));

        Optional<Order> result = orderService.updateStatus(saved.getId(), OrderStatus.PROCESSING);

        assertTrue(result.isPresent());
        assertEquals(OrderStatus.PROCESSING, result.get().getStatus());
        assertEquals(OrderStatus.PROCESSING,
                orderRepository.findById(saved.getId()).get().getStatus());
    }

    @Test
    void updateStatusReturnsEmptyWhenOrderIsMissing() {
        Optional<Order> result = orderService.updateStatus(999999L, OrderStatus.COMPLETED);

        assertFalse(result.isPresent());
    }
}
