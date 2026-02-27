package com.techouts.service.impl;

import com.techouts.entity.Cart;
import com.techouts.entity.CartItem;
import com.techouts.entity.Products;
import com.techouts.entity.User;
import com.techouts.repository.CartItemRepository;
import com.techouts.repository.CartRepository;
import com.techouts.repository.ProductRepository;
import com.techouts.repository.UserRepository;
import com.techouts.service.CartItemsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CartItemsServiceImpl implements CartItemsService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartItemsServiceImpl(CartRepository cartRepository, CartItemRepository cartItemRepository, ProductRepository productRepository, UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void addToCart(Long userId, Long productId, Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("Quantity must be >= 1");
        }

        Cart cart = getCartForUser(userId);
        Products product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId).orElseGet(() -> {
            CartItem ci = new CartItem();
            ci.setCart(cart);
            ci.setProduct(product);
            ci.setQuantity(0);
            return ci;
        });

        int finalQty = item.getQuantity() + quantity;
        if (finalQty > product.getStock()) {
            throw new IllegalArgumentException("Not enough stock");
        }

        item.setQuantity(finalQty);
        cartItemRepository.save(item);
    }

    @Override
    @Transactional
    public void increase(Long userId, Long cartItemId) {
        changeQty(userId, cartItemId, 1);
    }

    @Override
    @Transactional
    public void decrease(Long userId, Long cartItemId) {
        changeQty(userId, cartItemId, -1);
    }

    @Override
    @Transactional
    public void update(Long userId, Long cartItemId, Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("Quantity must be >= 1");
        }
        CartItem item = getOwnedItem(userId, cartItemId);
        if (quantity > item.getProduct().getStock()) {
            throw new IllegalArgumentException("Not enough stock");
        }
        item.setQuantity(quantity);
        cartItemRepository.save(item);
    }

    @Override
    @Transactional
    public void remove(Long userId, Long cartItemId) {
        CartItem item = getOwnedItem(userId, cartItemId);
        cartItemRepository.delete(item);
    }

    @Override
    public List<CartItem> getCartItems(Long userId) {
        Cart cart = getCartForUser(userId);
        return cartItemRepository.findByCartId(cart.getId());
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getCartForUser(userId);
        cartItemRepository.deleteByCartId(cart.getId());
    }

    private void changeQty(Long userId, Long cartItemId, int delta) {
        CartItem item = getOwnedItem(userId, cartItemId);
        int updated = item.getQuantity() + delta;
        if (updated < 1) {
            throw new IllegalArgumentException("Quantity must be >= 1");
        }
        if (updated > item.getProduct().getStock()) {
            throw new IllegalArgumentException("Not enough stock");
        }
        item.setQuantity(updated);
        cartItemRepository.save(item);
    }

    private CartItem getOwnedItem(Long userId, Long cartItemId) {
        Cart cart = getCartForUser(userId);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found"));
        if (!item.getCart().getId().equals(cart.getId())) {
            throw new IllegalArgumentException("Item does not belong to user");
        }
        return item;
    }

    private Cart getCartForUser(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    // Create cart if it doesn't exist (for existing users)
                    Cart cart = new Cart();
                    // We need to fetch the user to set the relationship
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("User not found"));
                    cart.setUser(user);
                    return cartRepository.save(cart);
                });
    }
}
