package com.mealoo.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class Restaurant {
    private Long id;
    private String name;
    private List<Category> categories;
}
