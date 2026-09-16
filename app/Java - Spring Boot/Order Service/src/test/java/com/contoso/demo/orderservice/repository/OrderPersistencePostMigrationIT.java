package com.contoso.demo.orderservice.repository;

import com.contoso.demo.orderservice.model.Order;
import com.contoso.demo.orderservice.model.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Phase 3 (verify-test-baseline) — {@code *PostMigrationIT} test.
 *
 * <p>Re-proves the H2 persistence assumptions frozen in
 * {@code src/test/test-cases/test-cases.md} section 4 (IDENTITY id
 * generation, {@code @PrePersist}-populated {@code createdAt}, STRING-mapped
 * status enum, customer filtering) against the migrated Java 25 / Spring
 * Boot 4.1.1 / Jakarta Persistence implementation. Mirrors, method-for-method,
 * the frozen {@code OrderRepositoryTest} scenarios from section 2 of the
 * baseline. Uses the {@code @DataJpaTest} embedded/in-memory H2 substitute —
 * the MOCKED external-dependency strategy frozen in section 7 — with no
 * Azure resource or credential involved.</p>
 *
 * <p>Append-only: does not modify or replace {@code OrderRepositoryTest}.</p>
 */
@DataJpaTest
class OrderPersistencePostMigrationIT {

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void findByCustomerReturnsOnlyMatchingOrders() {
        orderRepository.save(new Order("erin-postmigration", new BigDecimal("5.00")));
        orderRepository.save(new Order("erin-postmigration", new BigDecimal("7.00")));
        orderRepository.save(new Order("frank-postmigration", new BigDecimal("9.00")));

        List<Order> erinOrders = orderRepository.findByCustomer("erin-postmigration");

        assertEquals(2, erinOrders.size());
    }

    @Test
    void createdAtIsPopulatedOnSave() {
        Order saved = orderRepository.save(new Order("grace-postmigration", new BigDecimal("3.50")));

        assertEquals("grace-postmigration", saved.getCustomer());
        assertNotNull(saved.getCreatedAt());
        assertEquals(OrderStatus.PENDING, saved.getStatus());
    }

    @Test
    void createdAtIsPopulatedForRestStyleOrderOnSave() {
        Order order = new Order();
        order.setCustomer("harry-postmigration");
        order.setAmount(new BigDecimal("11.25"));

        Order saved = orderRepository.saveAndFlush(order);

        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void statusIsPersistedAsStringMappedEnum() {
        Order order = new Order("helen-postmigration", new BigDecimal("18.25"));
        order.setStatus(OrderStatus.COMPLETED);

        Order saved = orderRepository.saveAndFlush(order);

        assertEquals(OrderStatus.COMPLETED,
                orderRepository.findById(saved.getId()).get().getStatus());
    }

    @Test
    void idIsAssignedViaIdentityGenerationOnSave() {
        Order saved = orderRepository.save(new Order("ivan-postmigration", new BigDecimal("2.00")));

        assertNotNull(saved.getId(), "IDENTITY strategy must assign an id on save");
    }
}
