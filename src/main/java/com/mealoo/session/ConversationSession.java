package com.mealoo.session;

import com.mealoo.enums.ConversationState;
import com.mealoo.enums.PaymentMethod;
import com.mealoo.model.Cart;
import com.mealoo.model.Category;
import com.mealoo.model.Restaurant;
import lombok.Data;

@Data
public class ConversationSession {
    private String phoneNumber;
    private ConversationState state = ConversationState.SELECT_RESTAURANT;
    private Restaurant selectedRestaurant;
    private Category selectedCategory;
    private Cart cart = new Cart();
    private String deliveryAddress;
    private PaymentMethod paymentMethod;
    private String pendingOrderId;
    private int itemPage = 0;

    public ConversationSession(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void reset() {
        this.state = ConversationState.SELECT_RESTAURANT;
        this.selectedRestaurant = null;
        this.selectedCategory = null;
        this.cart = new Cart();
        this.deliveryAddress = null;
        this.paymentMethod = null;
        this.pendingOrderId = null;
        this.itemPage = 0;
    }
}
