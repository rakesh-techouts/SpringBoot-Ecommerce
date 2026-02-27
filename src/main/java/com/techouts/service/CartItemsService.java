package com.techouts.service;

import com.techouts.entity.CartItem;

import java.util.List;

public interface CartItemsService {
    void addToCart(Long userId, Long productId, Integer quantity);

    void increase(Long userId, Long cartItemId);

    void decrease(Long userId, Long cartItemId);

    void update(Long userId, Long cartItemId, Integer quantity);

    void remove(Long userId, Long cartItemId);

    List<CartItem> getCartItems(Long userId);

    void clearCart(Long userId);
}
