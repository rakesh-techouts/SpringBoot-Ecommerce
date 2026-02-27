package com.techouts.controller;

import com.techouts.entity.CartItem;
import com.techouts.service.CartItemsService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

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
        return "cart";
    }

    @PostMapping("/cart/add")
    public String add(@RequestParam Long productId,
                      @RequestParam(defaultValue = "1") Integer quantity,
                      HttpSession session,
                      Model model) {
        return handleAction(session, model, () -> cartItemsService.addToCart(loggedInUserId(session), productId, quantity));
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
    public String buyNow(@RequestParam Long productId, HttpSession session) {
        if (loggedInUserId(session) == null) {
            return "redirect:/login";
        }
        return "redirect:/checkout?productId=" + productId;
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
