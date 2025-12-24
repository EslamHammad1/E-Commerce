package com.example.E_commerce.service;

import com.example.E_commerce.dto.AddToCartRequestDTO;
import com.example.E_commerce.dto.CartItemResponseDTO;
import com.example.E_commerce.dto.CartResponseDTO;
import com.example.E_commerce.dto.UpdateCartItemRequestDTO;
import com.example.E_commerce.entity.Cart;
import com.example.E_commerce.entity.CartItem;
import com.example.E_commerce.entity.Product;
import com.example.E_commerce.entity.User;
import com.example.E_commerce.exception.ResourceNotFoundException;
import com.example.E_commerce.repository.CartItemRepository;
import com.example.E_commerce.repository.CartRepository;
import com.example.E_commerce.repository.ProductRepository;
import com.example.E_commerce.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Autowired
    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository,
                       ProductRepository productRepository, UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CartResponseDTO addToCart(Long userId, AddToCartRequestDTO request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        if (!product.getIsActive()) {
            throw new IllegalStateException("Product is not available");
        }

        if (product.getStockQuantity() < request.getQuantity()) {
            throw new IllegalStateException("Insufficient stock. Available: " + product.getStockQuantity());
        }

        Cart cart = getOrCreateCart(userId);

        CartItem existingItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())
                .orElse(null);

        if (existingItem != null) {
            int newQuantity = existingItem.getQuantity() + request.getQuantity();
            if (product.getStockQuantity() < newQuantity) {
                throw new IllegalStateException("Insufficient stock. Available: " + product.getStockQuantity());
            }
            existingItem.setQuantity(newQuantity);
            cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = new CartItem();
            newItem.setProduct(product);
            newItem.setQuantity(request.getQuantity());
            newItem.setPriceAtAddition(product.getPrice());
            cart.addItem(newItem);
            cartItemRepository.save(newItem);
        }

        cart = cartRepository.save(cart);
        return convertToResponseDTO(cart);
    }

    public CartResponseDTO getCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user"));
        return convertToResponseDTO(cart);
    }

    @Transactional
    public CartResponseDTO updateCartItem(Long userId, Long cartItemId, UpdateCartItemRequestDTO request) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user"));

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));

        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new IllegalStateException("Cart item does not belong to this user's cart");
        }

        Product product = cartItem.getProduct();
        if (product.getStockQuantity() < request.getQuantity()) {
            throw new IllegalStateException("Insufficient stock. Available: " + product.getStockQuantity());
        }

        cartItem.setQuantity(request.getQuantity());
        cartItemRepository.save(cartItem);

        cart = cartRepository.findById(cart.getId()).get();
        return convertToResponseDTO(cart);
    }

    @Transactional
    public CartResponseDTO removeCartItem(Long userId, Long cartItemId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user"));

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));

        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new IllegalStateException("Cart item does not belong to this user's cart");
        }

        cart.removeItem(cartItem);
        cartItemRepository.delete(cartItem);

        cart = cartRepository.save(cart);
        return convertToResponseDTO(cart);
    }

    @Transactional
    public void clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user"));

        cart.clearItems();
        cartRepository.save(cart);
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

            Cart newCart = new Cart();
            newCart.setUser(user);
            return cartRepository.save(newCart);
        });
    }

    private CartResponseDTO convertToResponseDTO(Cart cart) {
        List<CartItemResponseDTO> itemDTOs = cart.getItems().stream()
                .map(this::convertToCartItemDTO)
                .collect(Collectors.toList());

        CartResponseDTO dto = new CartResponseDTO();
        dto.setId(cart.getId());
        dto.setUserId(cart.getUser().getId());
        dto.setItems(itemDTOs);
        dto.setTotalItems(cart.getTotalItems());
        dto.setTotalAmount(cart.getTotalAmount());
        dto.setUpdatedAt(cart.getUpdatedAt());
        return dto;
    }

    private CartItemResponseDTO convertToCartItemDTO(CartItem item) {
        CartItemResponseDTO dto = new CartItemResponseDTO();
        dto.setId(item.getId());
        dto.setProductId(item.getProduct().getId());
        dto.setProductName(item.getProduct().getName());
        dto.setProductImageUrl(item.getProduct().getImageUrl());
        dto.setPriceAtAddition(item.getPriceAtAddition());
        dto.setQuantity(item.getQuantity());
        dto.setSubtotal(item.getSubtotal());
        dto.setAvailableStock(item.getProduct().getStockQuantity());
        return dto;
    }
}