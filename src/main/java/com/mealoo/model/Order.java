package com.mealoo.model;

import com.mealoo.enums.OrderStatus;
import com.mealoo.enums.PaymentMethod;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class Order {
    private String orderId;
    private String restaurantName;
    private List<CartItem> items;
    private double totalAmount;
    private String deliveryAddress;
    private PaymentMethod paymentMethod;
    private OrderStatus status;
    private int estimatedDeliveryMinutes;
}
