package com.techouts.controller;

import com.techouts.entity.CartItem;
import com.techouts.service.CartItemsService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class CartController {

    private final CartItemsService cartItemsService;

    public CartController(CartItemsService cartItemsService) {
        this.cartItemsService = cartItemsService;
    }

    @GetMapping("/cart")
    public String cart(HttpSession session, Model model) {
        Long userId = loggedInUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }

        List<CartItem> items = cartItemsService.getCartItems(userId);
        BigDecimal total = items.stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("items", items);
        model.addAttribute("total", total);
        
        // Add cart count
        int cartCount = cartItemsService.getCartItemCount(userId);
        model.addAttribute("cartCount", cartCount);
        
        return "user/cart";
    }

    @PostMapping("/cart/add")
    public ResponseEntity<Map<String, Object>> add(@RequestParam Long productId,
                                                 @RequestParam(defaultValue = "1") Integer quantity,
                                                 HttpSession session) {
        Long userId = loggedInUserId(session);
        if (userId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Please login to add items to cart"));
        }
        
        try {
            cartItemsService.addToCart(userId, productId, quantity);
            int cartCount = cartItemsService.getCartItemCount(userId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Product added to cart!",
                "cartCount", cartCount
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", ex.getMessage()
            ));
        }
    }

    @PostMapping("/cart/add-form")
    public String addFromForm(@RequestParam Long productId,
                              @RequestParam(defaultValue = "1") Integer quantity,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        Long userId = loggedInUserId(session);
        if (userId == null) {
            return "redirect:/login";
        }

        try {
            cartItemsService.addToCart(userId, productId, quantity);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }

        return "redirect:/home";
    }

    @GetMapping("/cart/count")
    public ResponseEntity<Map<String, Object>> getCartCount(HttpSession session) {
        Long userId = loggedInUserId(session);
        if (userId == null) {
            return ResponseEntity.ok(Map.of("cartCount", 0));
        }
        
        int cartCount = cartItemsService.getCartItemCount(userId);
        return ResponseEntity.ok(Map.of("cartCount", cartCount));
    }

    @PostMapping("/cart/increase/{itemId}")
    public String increase(@PathVariable Long itemId, HttpSession session, Model model) {
        return handleAction(session, model, () -> cartItemsService.increase(loggedInUserId(session), itemId));
    }

    @PostMapping("/cart/decrease/{itemId}")
    public String decrease(@PathVariable Long itemId, HttpSession session, Model model) {
        return handleAction(session, model, () -> cartItemsService.decrease(loggedInUserId(session), itemId));
    }

    @PostMapping("/cart/update/{itemId}")
    public String update(@PathVariable Long itemId,
                         @RequestParam Integer quantity,
                         HttpSession session,
                         Model model) {
        return handleAction(session, model, () -> cartItemsService.update(loggedInUserId(session), itemId, quantity));
    }

    @PostMapping("/cart/remove/{itemId}")
    public String remove(@PathVariable Long itemId, HttpSession session, Model model) {
        return handleAction(session, model, () -> cartItemsService.remove(loggedInUserId(session), itemId));
    }

    @GetMapping("/buy-now")
    public String buyNow(@RequestParam Long productId,
                         @RequestParam(defaultValue = "1") Integer quantity,
                         HttpSession session) {
        if (loggedInUserId(session) == null) {
            return "redirect:/login";
        }
        int safeQuantity = (quantity == null || quantity < 1) ? 1 : quantity;
        return "redirect:/checkout?productId=" + productId + "&quantity=" + safeQuantity;
    }

    private String handleAction(HttpSession session, Model model, Runnable action) {
        if (loggedInUserId(session) == null) {
            return "redirect:/login";
        }
        try {
            action.run();
            return "redirect:/cart";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return cart(session, model);
        }
    }

    private Long loggedInUserId(HttpSession session) {
        Object userId = session.getAttribute("USER_ID");
        return userId instanceof Long ? (Long) userId : null;
    }
}
