package com.techouts.controller;

import com.techouts.entity.Order;
import com.techouts.entity.CartItem;
import com.techouts.entity.Products;
import com.techouts.entity.User;
import com.techouts.service.CartItemsService;
import com.techouts.service.OrderService;
import com.techouts.service.ProductService;
import com.techouts.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Controller
public class OrderController {
    private static final Set<String> PAYMENT_MODES = Set.of(
            "Upi",
            "Debit Card",
            "Credit Card",
            "Netbanking",
            "Cash on Delivery"
    );

    private final OrderService orderService;
    private final ProductService productService;
    private final CartItemsService cartItemsService;
    private final UserService userService;

    public OrderController(OrderService orderService, ProductService productService, CartItemsService cartItemsService, UserService userService) {
        this.orderService = orderService;
        this.productService = productService;
        this.cartItemsService = cartItemsService;
        this.userService = userService;
    }

    @GetMapping("/checkout")
    public String checkout(@RequestParam(required = false) Long productId, HttpSession session, Model model) {
        Long userId = loggedInUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }
        
        User user = userService.findById(userId);
        model.addAttribute("productId", productId);
        model.addAttribute("paymentModes", PAYMENT_MODES);
        model.addAttribute("userAddress", user.getAddress());
        populateCheckoutSummary(userId, productId, model);
        return "checkout";
    }

    @PostMapping("/checkout/place")
    public String placeOrder(@RequestParam(required = false) Long productId,
                             @RequestParam String shippingAddress,
                             @RequestParam String paymentMode,
                             HttpSession session,
                             Model model) {
        Long userId = loggedInUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }
        try {
            orderService.placeOrder(userId, productId, shippingAddress, paymentMode);
            return "redirect:/orders";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("productId", productId);
            model.addAttribute("shippingAddress", shippingAddress);
            model.addAttribute("selectedPaymentMode", paymentMode);
            model.addAttribute("paymentModes", PAYMENT_MODES);
            // Add user address for display
            User user = userService.findById(userId);
            model.addAttribute("userAddress", user.getAddress());
            populateCheckoutSummary(userId, productId, model);
            return "checkout";
        }
    }

    @GetMapping("/orders")
    public String orders(HttpSession session, Model model) {
        Long userId = loggedInUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }
        List<Order> orders = orderService.getOrderHistory(userId);
        model.addAttribute("orders", orders);
        return "orders";
    }

    private Long loggedInUserId(HttpSession session) {
        Object userId = session.getAttribute("USER_ID");
        return userId instanceof Long ? (Long) userId : null;
    }

    private void populateCheckoutSummary(Long userId, Long productId, Model model) {
        List<CheckoutItemView> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        if (productId != null) {
            Products product = productService.getById(productId);
            BigDecimal subtotal = product.getPrice();
            items.add(new CheckoutItemView(product.getImageUrl(), product.getName(), product.getPrice(), 1, subtotal));
            total = total.add(subtotal);
        } else {
            List<CartItem> cartItems = cartItemsService.getCartItems(userId);
            for (CartItem item : cartItems) {
                BigDecimal subtotal = item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                items.add(new CheckoutItemView(
                        item.getProduct().getImageUrl(),
                        item.getProduct().getName(),
                        item.getProduct().getPrice(),
                        item.getQuantity(),
                        subtotal
                ));
                total = total.add(subtotal);
            }
        }

        model.addAttribute("checkoutItems", items);
        model.addAttribute("checkoutTotal", total);
    }

    public static class CheckoutItemView {
        private final String imageUrl;
        private final String name;
        private final BigDecimal price;
        private final Integer quantity;
        private final BigDecimal subtotal;

        public CheckoutItemView(String imageUrl, String name, BigDecimal price, Integer quantity, BigDecimal subtotal) {
            this.imageUrl = imageUrl;
            this.name = name;
            this.price = price;
            this.quantity = quantity;
            this.subtotal = subtotal;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public String getName() {
            return name;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public BigDecimal getSubtotal() {
            return subtotal;
        }
    }
}
