package com.techouts.service.impl;

import com.techouts.entity.CartItem;
import com.techouts.entity.Order;
import com.techouts.entity.OrderStatus;
import com.techouts.entity.OrderItems;
import com.techouts.entity.Products;
import com.techouts.entity.User;
import com.techouts.repository.OrderItemsRepository;
import com.techouts.repository.OrderRepository;
import com.techouts.repository.ProductRepository;
import com.techouts.repository.UserRepository;
import com.techouts.service.CartItemsService;
import com.techouts.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class OrderServiceImpl implements OrderService {
    private static final Set<String> PAYMENT_MODES = Set.of(
            "Upi",
            "Debit Card",
            "Credit Card",
            "Netbanking",
            "Cash on Delivery"
    );

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemsRepository orderItemsRepository;
    private final CartItemsService cartItemsService;

    public OrderServiceImpl(UserRepository userRepository,
                            ProductRepository productRepository,
                            OrderRepository orderRepository,
                            OrderItemsRepository orderItemsRepository,
                            CartItemsService cartItemsService) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.orderItemsRepository = orderItemsRepository;
        this.cartItemsService = cartItemsService;
    }

    @Override
    @Transactional
    public Order placeOrder(Long userId, Long directProductId, Integer directQuantity, String shippingAddress, String paymentMode) {
        if (shippingAddress == null || shippingAddress.isBlank()) {
            throw new IllegalArgumentException("Address is required");
        }
        if (!PAYMENT_MODES.contains(paymentMode)) {
            throw new IllegalArgumentException("Please select a valid payment mode");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<OrderItems> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        if (directProductId != null) {
            int quantity = (directQuantity == null || directQuantity < 1) ? 1 : directQuantity;
            Products product = productRepository.findById(directProductId)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found"));
            if (product.getStock() < quantity) {
                throw new IllegalArgumentException("Out of stock");
            }
            product.setStock(product.getStock() - quantity);
            productRepository.save(product);

            OrderItems oi = new OrderItems();
            oi.setProduct(product);
            oi.setQuantity(quantity);
            oi.setPrice(product.getPrice());
            items.add(oi);
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        } else {
            List<CartItem> cartItems = cartItemsService.getCartItems(userId);
            if (cartItems.isEmpty()) {
                throw new IllegalArgumentException("Cart is empty");
            }

            for (CartItem cartItem : cartItems) {
                Products product = cartItem.getProduct();
                if (product.getStock() < cartItem.getQuantity()) {
                    throw new IllegalArgumentException("Insufficient stock for " + product.getName());
                }
            }

            for (CartItem cartItem : cartItems) {
                Products product = cartItem.getProduct();
                product.setStock(product.getStock() - cartItem.getQuantity());
                productRepository.save(product);

                OrderItems oi = new OrderItems();
                oi.setProduct(product);
                oi.setQuantity(cartItem.getQuantity());
                oi.setPrice(product.getPrice());
                items.add(oi);

                total = total.add(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            }
        }

        Order order = new Order();
        order.setUser(user);
        order.setTotalAmount(total);
        order.setShippingAddress(shippingAddress.trim());
        order.setPaymentMode(paymentMode);
        order.setStatus(OrderStatus.PLACED);
        Order savedOrder = orderRepository.save(order);

        for (OrderItems item : items) {
            item.setOrder(savedOrder);
        }
        orderItemsRepository.saveAll(items);

        if (directProductId == null) {
            cartItemsService.clearCart(userId);
        }
        return savedOrder;
    }

    @Override
    public List<Order> getOrderHistory(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
    }

    @Override
    public List<OrderItems> getOrderItems(Long orderId) {
        return orderItemsRepository.findByOrderId(orderId);
    }

    @Override
    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Order does not belong to user");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Order already cancelled");
        }

        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalArgumentException("Only placed or packed orders can be cancelled");
        }

        List<OrderItems> items = orderItemsRepository.findByOrderId(orderId);
        for (OrderItems item : items) {
            Products product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }
}
