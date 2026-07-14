package com.mealoo.service;

import com.mealoo.model.Category;
import com.mealoo.model.MenuItem;
import com.mealoo.model.Restaurant;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.List;

@Service
public class RestaurantService {

    private final List<Restaurant> restaurants;

    public RestaurantService() {
        this.restaurants = buildMockData();
    }

    public List<Restaurant> getAllRestaurants() {
        return restaurants;
    }

    public Restaurant getById(int index) {
        if (index < 0 || index >= restaurants.size()) return null;
        return restaurants.get(index);
    }

    private List<Restaurant> buildMockData() {
        MenuItem chickenBiryani = MenuItem.builder().id(1L).name("Chicken Biryani").price(250).description("Aromatic biryani with tender chicken").build();
        MenuItem chickenCurry = MenuItem.builder().id(2L).name("Chicken Curry").price(220).description("Rich spicy chicken curry").build();
        MenuItem tandooriChicken = MenuItem.builder().id(3L).name("Tandoori Chicken").price(300).description("Smoky grilled chicken").build();
        MenuItem dalMakhani = MenuItem.builder().id(4L).name("Dal Makhani").price(180).description("Creamy black lentils").build();
        MenuItem paneerButter = MenuItem.builder().id(5L).name("Paneer Butter Masala").price(200).description("Rich tomato gravy with paneer").build();
        MenuItem vegBiryani = MenuItem.builder().id(6L).name("Veg Biryani").price(200).description("Fragrant vegetable biryani").build();
        MenuItem samosa = MenuItem.builder().id(7L).name("Samosa").price(30).description("Crispy fried pastry with spiced filling").build();
        MenuItem onionRings = MenuItem.builder().id(8L).name("Onion Rings").price(80).description("Crispy golden onion rings").build();
        MenuItem masalaChai = MenuItem.builder().id(9L).name("Masala Chai").price(40).description("Spiced Indian tea").build();
        MenuItem lassi = MenuItem.builder().id(10L).name("Sweet Lassi").price(60).description("Chilled yogurt drink").build();

        Category nonVeg = Category.builder().id(1L).name("Non-Veg")
                .menuItems(Arrays.asList(chickenBiryani, chickenCurry, tandooriChicken)).build();
        Category veg = Category.builder().id(2L).name("Veg")
                .menuItems(Arrays.asList(dalMakhani, paneerButter, vegBiryani)).build();
        Category snacks = Category.builder().id(3L).name("Snacks")
                .menuItems(Arrays.asList(samosa, onionRings)).build();
        Category beverages = Category.builder().id(4L).name("Beverages")
                .menuItems(Arrays.asList(masalaChai, lassi)).build();

        List<Category> allCategories = Arrays.asList(nonVeg, veg, snacks, beverages);

        return Arrays.asList(
                Restaurant.builder().id(1L).name("Patratu Lake Resort").categories(allCategories).build(),
                Restaurant.builder().id(2L).name("Alexa Resort").categories(allCategories).build(),
                Restaurant.builder().id(3L).name("Midwaay").categories(Arrays.asList(veg, snacks, beverages)).build(),
                Restaurant.builder().id(4L).name("The Eternity Resorts").categories(Arrays.asList(nonVeg, veg, beverages)).build(),
                Restaurant.builder().id(5L).name("The Royal Lush").categories(allCategories).build()
        );
    }
}
