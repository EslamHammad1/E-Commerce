package com.example.E_commerce.controller;

import com.example.E_commerce.dto.AddToCartRequestDTO;
import com.example.E_commerce.dto.CartResponseDTO;
import com.example.E_commerce.dto.UpdateCartItemRequestDTO;
import com.example.E_commerce.service.CartService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.example.E_commerce.repository.UserRepository;
import com.example.E_commerce.entity.User;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;
    private final UserRepository userRepository;

    @Autowired
    public CartController(CartService cartService, UserRepository userRepository) {
        this.cartService = cartService;
        this.userRepository = userRepository;
    }

    @PostMapping("/add")
    public ResponseEntity<CartResponseDTO> addToCart(
            @Valid @RequestBody AddToCartRequestDTO request,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        CartResponseDTO response = cartService.addToCart(userId, request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<CartResponseDTO> getCart(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        CartResponseDTO response = cartService.getCart(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponseDTO> updateCartItem(
            @PathVariable Long cartItemId,
            @Valid @RequestBody UpdateCartItemRequestDTO request,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        CartResponseDTO response = cartService.updateCartItem(userId, cartItemId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponseDTO> removeCartItem(
            @PathVariable Long cartItemId,
            Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        CartResponseDTO response = cartService.removeCartItem(userId, cartItemId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearCart(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    private Long getCurrentUserId(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getId();
    }
}