package com.example.E_commerce.dto;

import com.example.E_commerce.entity.OrderStatus;
import com.example.E_commerce.entity.PaymentMethod;
import com.example.E_commerce.entity.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// ============= Order Response DTO =============
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDTO {
    private Long id;
    private String orderNumber;
    private Long userId;
    private List<OrderItemResponseDTO> items;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private BigDecimal shippingCost;
    private BigDecimal taxAmount;
    private BigDecimal grandTotal;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private AddressResponseDTO shippingAddress;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime deliveredAt;
}
