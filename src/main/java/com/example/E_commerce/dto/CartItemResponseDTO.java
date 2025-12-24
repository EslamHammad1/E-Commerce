package com.example.E_commerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// ============= Cart Item Response DTO =============
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponseDTO {
    private Long id;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private BigDecimal priceAtAddition;
    private Integer quantity;
    private BigDecimal subtotal;
    private Integer availableStock;
}
