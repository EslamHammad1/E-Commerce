package com.example.E_commerce.dto;

import com.example.E_commerce.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryDTO {
    private Long id;
    private String orderNumber;
    private OrderStatus status;
    private BigDecimal grandTotal;
    private Integer itemCount;
    private LocalDateTime createdAt;
}

