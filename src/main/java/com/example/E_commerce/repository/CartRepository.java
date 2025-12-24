package com.example.E_commerce.repository;

import com.example.E_commerce.entity.Cart;
import com.example.E_commerce.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserId(Long userId);
    Boolean existsByUserId(Long userId);
}

