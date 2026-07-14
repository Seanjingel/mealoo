package com.mealoo.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MenuItem {
    private Long id;
    private String name;
    private double price;
    private String description;
}
