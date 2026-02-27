package com.techouts.service;

import com.techouts.entity.Order;
import java.util.List;

public interface OrderService {
    Order placeOrder(Long userId, Long directProductId, String shippingAddress, String paymentMode);

    List<Order> getOrderHistory(Long userId);
}
