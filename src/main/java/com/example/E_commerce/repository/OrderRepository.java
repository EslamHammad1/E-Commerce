package com.example.E_commerce.repository;

import com.example.E_commerce.entity.Address;
import com.example.E_commerce.entity.Order;
import com.example.E_commerce.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// ============= Order Repository =============
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByUserId(Long userId, Pageable pageable);
    Optional<Order> findByOrderNumber(String orderNumber);
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
    List<Order> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);
}