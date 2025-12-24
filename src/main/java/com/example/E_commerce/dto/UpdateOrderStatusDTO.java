package com.example.E_commerce.dto;

import com.example.E_commerce.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// ============= Update Order Status DTO =============
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderStatusDTO {
    @NotNull(message = "Order status is required")
    private OrderStatus status;
}
