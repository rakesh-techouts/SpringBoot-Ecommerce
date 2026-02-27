package com.techouts.repository;

import com.techouts.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @EntityGraph(attributePaths = {"orderItems", "orderItems.product"})
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
}
