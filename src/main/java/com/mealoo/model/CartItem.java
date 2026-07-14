package com.mealoo.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CartItem {
    private MenuItem menuItem;
    private int quantity;
}
