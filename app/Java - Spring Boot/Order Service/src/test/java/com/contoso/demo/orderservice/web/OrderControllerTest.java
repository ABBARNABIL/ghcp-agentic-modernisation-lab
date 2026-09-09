package com.contoso.demo.orderservice.web;

import com.contoso.demo.orderservice.model.Order;
import com.contoso.demo.orderservice.model.OrderStatus;
import com.contoso.demo.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    void listReturnsAllOrders() throws Exception {
        when(orderService.findAll()).thenReturn(Arrays.asList(
                new Order("alice", new BigDecimal("120.50")),
                new Order("bob", new BigDecimal("42.99"))
        ));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].customer").value("alice"));
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        when(orderService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/orders/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void totalForCustomerReturnsValue() throws Exception {
        when(orderService.totalForCustomer("alice")).thenReturn(new BigDecimal("200.50"));

        mockMvc.perform(get("/api/orders/customer/alice/total"))
                .andExpect(status().isOk())
                .andExpect(content().string("200.50"));
    }

    @Test
    void createReturns201() throws Exception {
        when(orderService.create(any(Order.class)))
                .thenReturn(new Order("dave", new BigDecimal("15.00")));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customer\":\"dave\",\"amount\":15.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customer").value("dave"));
    }

    @Test
    void createRejectsInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRejectsBlankCustomerAndNonPositiveAmount() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customer\":\"   \",\"amount\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateStatusReturnsUpdatedOrder() throws Exception {
        Order order = new Order("dave", new BigDecimal("15.00"));
        order.setStatus(OrderStatus.PROCESSING);
        when(orderService.updateStatus(4L, OrderStatus.PROCESSING))
                .thenReturn(Optional.of(order));

        mockMvc.perform(patch("/api/orders/4/status")
                        .param("status", "PROCESSING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"));
    }

    @Test
    void updateStatusReturns404WhenOrderIsMissing() throws Exception {
        when(orderService.updateStatus(99L, OrderStatus.COMPLETED))
                .thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/orders/99/status")
                        .param("status", "COMPLETED"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateStatusRejectsUnknownStatus() throws Exception {
        mockMvc.perform(patch("/api/orders/1/status")
                        .param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest());
    }
}
