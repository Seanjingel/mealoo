package com.mealoo.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class Category {
    private Long id;
    private String name;
    private List<MenuItem> menuItems;
}
