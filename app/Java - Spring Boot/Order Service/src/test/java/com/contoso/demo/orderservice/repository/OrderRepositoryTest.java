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

@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void findByCustomerReturnsOnlyMatchingOrders() {
        orderRepository.save(new Order("erin", new BigDecimal("5.00")));
        orderRepository.save(new Order("erin", new BigDecimal("7.00")));
        orderRepository.save(new Order("frank", new BigDecimal("9.00")));

        List<Order> erinOrders = orderRepository.findByCustomer("erin");

        assertEquals(2, erinOrders.size());
    }

    @Test
    void createdAtIsPopulatedOnSave() {
        Order saved = orderRepository.save(new Order("grace", new BigDecimal("3.50")));

        assertEquals("grace", saved.getCustomer());
        assertNotNull(saved.getCreatedAt());
        assertEquals(OrderStatus.PENDING, saved.getStatus());
    }

    @Test
    void createdAtIsPopulatedForRestStyleOrderOnSave() {
        Order order = new Order();
        order.setCustomer("harry");
        order.setAmount(new BigDecimal("11.25"));

        Order saved = orderRepository.saveAndFlush(order);

        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void statusIsPersisted() {
        Order order = new Order("helen", new BigDecimal("18.25"));
        order.setStatus(OrderStatus.COMPLETED);

        Order saved = orderRepository.saveAndFlush(order);

        assertEquals(OrderStatus.COMPLETED,
                orderRepository.findById(saved.getId()).get().getStatus());
    }
}
