package com.techouts.service;

import com.techouts.entity.Order;
import com.techouts.entity.OrderItems;
import java.util.List;

public interface OrderService {
    Order placeOrder(Long userId, Long directProductId, Integer directQuantity, String shippingAddress, String paymentMode);

    List<Order> getOrderHistory(Long userId);
    
    Order getOrderById(Long orderId);
    
    List<OrderItems> getOrderItems(Long orderId);

    void cancelOrder(Long userId, Long orderId);
}
