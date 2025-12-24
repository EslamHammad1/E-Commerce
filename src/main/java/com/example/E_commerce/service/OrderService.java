package com.example.E_commerce.service;

import com.example.E_commerce.dto.*;
import com.example.E_commerce.entity.*;
import com.example.E_commerce.exception.ResourceNotFoundException;
import com.example.E_commerce.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Autowired
    public OrderService(OrderRepository orderRepository, CartRepository cartRepository,
                        AddressRepository addressRepository, UserRepository userRepository,
                        ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public OrderResponseDTO checkout(Long userId, CheckoutRequestDTO request) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + request.getAddressId()));

        if (!address.getUser().getId().equals(userId)) {
            throw new IllegalStateException("Address does not belong to this user");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setUser(user);
        order.setPaymentMethod(request.getPaymentMethod());
        order.setNotes(request.getNotes());

        // Copy shipping address
        order.setShippingFullName(address.getFullName());
        order.setShippingPhoneNumber(address.getPhoneNumber());
        order.setShippingStreetAddress(address.getStreetAddress());
        order.setShippingCity(address.getCity());
        order.setShippingState(address.getState());
        order.setShippingPostalCode(address.getPostalCode());
        order.setShippingCountry(address.getCountry());

        // Convert cart items to order items
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();

            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new IllegalStateException("Insufficient stock for product: " + product.getName());
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPriceAtPurchase(cartItem.getPriceAtAddition());
            order.addItem(orderItem);

            // Update stock
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);
        }

        // Calculate totals
        BigDecimal itemsTotal = order.calculateItemsTotal();
        BigDecimal shippingCost = calculateShippingCost(itemsTotal);
        BigDecimal taxAmount = calculateTax(itemsTotal);
        BigDecimal grandTotal = itemsTotal.add(shippingCost).add(taxAmount);

        order.setTotalAmount(itemsTotal);
        order.setShippingCost(shippingCost);
        order.setTaxAmount(taxAmount);
        order.setGrandTotal(grandTotal);

        // Set payment status based on payment method
        if (request.getPaymentMethod() == PaymentMethod.CASH_ON_DELIVERY) {
            order.setPaymentStatus(PaymentStatus.PENDING);
            order.setStatus(OrderStatus.CONFIRMED);
        } else {
            order.setPaymentStatus(PaymentStatus.COMPLETED);
            order.setStatus(OrderStatus.CONFIRMED);
        }

        Order savedOrder = orderRepository.save(order);

        // Clear cart
        cart.clearItems();
        cartRepository.save(cart);

        return convertToResponseDTO(savedOrder);
    }

    public OrderResponseDTO getOrderById(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new IllegalStateException("Order does not belong to this user");
        }

        return convertToResponseDTO(order);
    }

    public Page<OrderSummaryDTO> getUserOrders(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return orderRepository.findByUserId(userId, pageable)
                .map(this::convertToSummaryDTO);
    }

    public List<OrderSummaryDTO> getRecentOrders(Long userId) {
        return orderRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::convertToSummaryDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponseDTO updateOrderStatus(Long orderId, UpdateOrderStatusDTO request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        order.setStatus(request.getStatus());

        if (request.getStatus() == OrderStatus.DELIVERED) {
            order.setDeliveredAt(LocalDateTime.now());
            order.setPaymentStatus(PaymentStatus.COMPLETED);
        }

        Order updatedOrder = orderRepository.save(order);
        return convertToResponseDTO(updatedOrder);
    }

    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new IllegalStateException("Order does not belong to this user");
        }

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot cancel order in " + order.getStatus() + " status");
        }

        // Restore stock
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus(PaymentStatus.REFUNDED);
        orderRepository.save(order);
    }

    public Page<OrderSummaryDTO> getAllOrders(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return orderRepository.findAll(pageable)
                .map(this::convertToSummaryDTO);
    }

    public Page<OrderSummaryDTO> getOrdersByStatus(OrderStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return orderRepository.findByStatus(status, pageable)
                .map(this::convertToSummaryDTO);
    }

    private String generateOrderNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "ORD-" + timestamp;
    }

    private BigDecimal calculateShippingCost(BigDecimal itemsTotal) {
        if (itemsTotal.compareTo(BigDecimal.valueOf(500)) >= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(50);
    }

    private BigDecimal calculateTax(BigDecimal itemsTotal) {
        return itemsTotal.multiply(BigDecimal.valueOf(0.14));
    }

    private OrderResponseDTO convertToResponseDTO(Order order) {
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setUserId(order.getUser().getId());
        dto.setStatus(order.getStatus());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setShippingCost(order.getShippingCost());
        dto.setTaxAmount(order.getTaxAmount());
        dto.setGrandTotal(order.getGrandTotal());
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setPaymentStatus(order.getPaymentStatus());
        dto.setNotes(order.getNotes());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setDeliveredAt(order.getDeliveredAt());

        List<OrderItemResponseDTO> itemDTOs = order.getItems().stream()
                .map(this::convertToOrderItemDTO)
                .collect(Collectors.toList());
        dto.setItems(itemDTOs);

        AddressResponseDTO addressDTO = new AddressResponseDTO();
        addressDTO.setFullName(order.getShippingFullName());
        addressDTO.setPhoneNumber(order.getShippingPhoneNumber());
        addressDTO.setStreetAddress(order.getShippingStreetAddress());
        addressDTO.setCity(order.getShippingCity());
        addressDTO.setState(order.getShippingState());
        addressDTO.setPostalCode(order.getShippingPostalCode());
        addressDTO.setCountry(order.getShippingCountry());
        dto.setShippingAddress(addressDTO);

        return dto;
    }

    private OrderItemResponseDTO convertToOrderItemDTO(OrderItem item) {
        OrderItemResponseDTO dto = new OrderItemResponseDTO();
        dto.setId(item.getId());
        dto.setProductId(item.getProduct().getId());
        dto.setProductName(item.getProduct().getName());
        dto.setProductImageUrl(item.getProduct().getImageUrl());
        dto.setPriceAtPurchase(item.getPriceAtPurchase());
        dto.setQuantity(item.getQuantity());
        dto.setSubtotal(item.getSubtotal());
        return dto;
    }

    private OrderSummaryDTO convertToSummaryDTO(Order order) {
        OrderSummaryDTO dto = new OrderSummaryDTO();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setStatus(order.getStatus());
        dto.setGrandTotal(order.getGrandTotal());
        dto.setItemCount(order.getItems().size());
        dto.setCreatedAt(order.getCreatedAt());
        return dto;
    }
}