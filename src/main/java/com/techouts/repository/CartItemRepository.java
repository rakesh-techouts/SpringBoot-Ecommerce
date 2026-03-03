package com.techouts.repository;

import com.techouts.entity.CartItem;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

    @EntityGraph(attributePaths = "product")
    List<CartItem> findByCartId(Long cartId);

    int countByCartId(Long cartId);

    @Query("select coalesce(sum(ci.quantity), 0) from CartItem ci where ci.cart.id = :cartId")
    int sumQuantityByCartId(Long cartId);

    void deleteByCartId(Long cartId);
}
