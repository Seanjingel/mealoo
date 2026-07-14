package com.mealoo.service;

import com.mealoo.enums.ConversationState;
import com.mealoo.enums.PaymentMethod;
import com.mealoo.model.*;
import com.mealoo.session.ConversationSession;
import com.mealoo.session.SessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class OrderWorkflowService {

    private final SessionManager sessionManager;
    private final RestaurantService restaurantService;
    private final WhatsAppCloudApiService cloudApiService;
    private final InteractivePayloadBuilder payloadBuilder;
    private final AtomicLong orderCounter = new AtomicLong(10000);

    private static final String UPI_ID = "mealoo@upi";
    private static final int ESTIMATED_DELIVERY = 35;
    private static final int PAGE_SIZE = 9;

    public void processMessage(String phoneNumber, String message) {
        ConversationSession session = sessionManager.getOrCreate(phoneNumber);
        String trimmed = message.trim();

        if (isGreeting(trimmed)) {
            session.reset();
            sendRestaurantList(phoneNumber);
            return;
        }

        switch (session.getState()) {
            case SELECT_RESTAURANT -> handleSelectRestaurant(session, phoneNumber, trimmed);
            case SELECT_CATEGORY -> handleSelectCategory(session, phoneNumber, trimmed);
            case SELECT_ITEMS -> handleSelectItems(session, phoneNumber, trimmed);
            case REVIEW_CART -> handleReviewCart(session, phoneNumber, trimmed);
            case COLLECT_ADDRESS -> handleCollectAddress(session, phoneNumber, trimmed);
            case CONFIRM_ORDER -> handleConfirmOrder(session, phoneNumber, trimmed);
            case PAYMENT_SELECTION -> handlePaymentSelection(session, phoneNumber, trimmed);
            case AWAITING_UPI_PAYMENT -> handleUpiPaymentConfirmation(phoneNumber);
            case ORDER_PLACED -> handleOrderPlaced(session, phoneNumber, trimmed);
        }
    }

    public void confirmUpiPayment(String phoneNumber) {
        ConversationSession session = sessionManager.getOrCreate(phoneNumber);
        if (session.getState() == ConversationState.AWAITING_UPI_PAYMENT) {
            placeOrder(session, phoneNumber);
        }
    }

    private boolean isGreeting(String msg) {
        String lower = msg.toLowerCase();
        return lower.equals("hi") || lower.equals("hello") || lower.equals("hey")
                || lower.equals("hii") || lower.equals("helo")
                || lower.startsWith("hi ") || lower.startsWith("hello ") || lower.startsWith("hey ");
    }

    private void sendRestaurantList(String to) {
        List<Restaurant> restaurants = restaurantService.getAllRestaurants();
        List<InteractivePayloadBuilder.Row> rows = new ArrayList<>();
        for (int i = 0; i < restaurants.size(); i++) {
            rows.add(new InteractivePayloadBuilder.Row("rest_" + i, restaurants.get(i).getName(), null));
        }
        cloudApiService.sendMessage(payloadBuilder.buildListMessage(
            to,
            "Welcome to Mealoo",
            "Select a restaurant to start your order.",
            "View Restaurants",
            List.of(new InteractivePayloadBuilder.Section("Restaurants", rows))
        ));
    }

    private void handleSelectRestaurant(ConversationSession session, String to, String input) {
        if (!input.startsWith("rest_")) {
            sendText(to, "Please select a restaurant from the list.");
            sendRestaurantList(to);
            return;
        }
        int index = parseIndex(input, "rest_");
        Restaurant restaurant = restaurantService.getById(index);
        if (restaurant == null) {
            sendText(to, "Invalid selection. Please choose from the list.");
            sendRestaurantList(to);
            return;
        }
        session.setSelectedRestaurant(restaurant);
        session.setState(ConversationState.SELECT_CATEGORY);
        sendCategoryList(to, restaurant);
    }

    private void sendCategoryList(String to, Restaurant restaurant) {
        List<Category> categories = restaurant.getCategories();
        List<InteractivePayloadBuilder.Row> rows = new ArrayList<>();
        for (int i = 0; i < categories.size(); i++) {
            rows.add(new InteractivePayloadBuilder.Row("cat_" + i, categories.get(i).getName(), null));
        }
        cloudApiService.sendMessage(payloadBuilder.buildListMessage(
            to,
            restaurant.getName(),
            "Select a category to browse items.",
            "View Categories",
            List.of(new InteractivePayloadBuilder.Section("Categories", rows))
        ));
    }

    private void handleSelectCategory(ConversationSession session, String to, String input) {
        if (!input.startsWith("cat_")) {
            sendText(to, "Please select a category from the list.");
            sendCategoryList(to, session.getSelectedRestaurant());
            return;
        }
        int index = parseIndex(input, "cat_");
        List<Category> categories = session.getSelectedRestaurant().getCategories();
        if (index < 0 || index >= categories.size()) {
            sendText(to, "Invalid selection. Please choose from the list.");
            sendCategoryList(to, session.getSelectedRestaurant());
            return;
        }
        Category category = categories.get(index);
        session.setSelectedCategory(category);
        session.setItemPage(0);
        session.setState(ConversationState.SELECT_ITEMS);
        sendItemList(to, category, 0);
    }

    private void sendItemList(String to, Category category, int page) {
        List<MenuItem> allItems = category.getMenuItems();
        int fromIndex = page * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, allItems.size());
        List<MenuItem> pageItems = allItems.subList(fromIndex, toIndex);
        boolean hasMore = toIndex < allItems.size();

        List<InteractivePayloadBuilder.Row> rows = new ArrayList<>();
        for (int i = 0; i < pageItems.size(); i++) {
            MenuItem item = pageItems.get(i);
            rows.add(new InteractivePayloadBuilder.Row(
                "item_" + (fromIndex + i),
                item.getName() + " Rs." + (int) item.getPrice(),
                item.getDescription()
            ));
        }
        if (hasMore) {
            rows.add(new InteractivePayloadBuilder.Row("item_more", "Show More", "View next page"));
        }

        String sectionTitle = (hasMore || page > 0)
            ? category.getName() + " (Page " + (page + 1) + ")"
            : category.getName();

        cloudApiService.sendMessage(payloadBuilder.buildListMessage(
            to,
            category.getName(),
            "Tap an item to add it to your cart.",
            "View Items",
            List.of(new InteractivePayloadBuilder.Section(sectionTitle, rows))
        ));
    }

    private void handleSelectItems(ConversationSession session, String to, String input) {
        if (input.equals("item_more")) {
            int nextPage = session.getItemPage() + 1;
            session.setItemPage(nextPage);
            sendItemList(to, session.getSelectedCategory(), nextPage);
            return;
        }
        if (!input.startsWith("item_")) {
            sendText(to, "Please select an item from the list.");
            sendItemList(to, session.getSelectedCategory(), session.getItemPage());
            return;
        }
        int index = parseIndex(input, "item_");
        List<MenuItem> allItems = session.getSelectedCategory().getMenuItems();
        if (index < 0 || index >= allItems.size()) {
            sendText(to, "Invalid selection. Please choose from the list.");
            sendItemList(to, session.getSelectedCategory(), session.getItemPage());
            return;
        }
        session.getCart().addItem(allItems.get(index), 1);
        session.setState(ConversationState.REVIEW_CART);
        sendCartReview(to, session);
    }

    private void sendCartReview(String to, ConversationSession session) {
        Cart cart = session.getCart();
        StringBuilder text = new StringBuilder("Cart Summary\n\n");
        for (CartItem item : cart.getItems()) {
            text.append("- ").append(item.getMenuItem().getName())
                .append(" x").append(item.getQuantity())
                .append(" = Rs.").append((int)(item.getMenuItem().getPrice() * item.getQuantity()))
                .append("\n");
        }
        text.append("\nTotal: Rs.").append((int) cart.getTotal());
        cloudApiService.sendMessage(payloadBuilder.buildButtonMessage(
            to,
            text.toString(),
            List.of(
                new InteractivePayloadBuilder.Button("cart_more", "Add More"),
                new InteractivePayloadBuilder.Button("cart_checkout", "Checkout")
            )
        ));
    }

    private void handleReviewCart(ConversationSession session, String to, String input) {
        switch (input) {
            case "cart_more" -> {
                session.setState(ConversationState.SELECT_CATEGORY);
                sendCategoryList(to, session.getSelectedRestaurant());
            }
            case "cart_checkout" -> {
                session.setState(ConversationState.COLLECT_ADDRESS);
                sendText(to, "Please type your complete delivery address.");
            }
            default -> {
                sendText(to, "Please use the buttons to choose.");
                sendCartReview(to, session);
            }
        }
    }

    private void handleCollectAddress(ConversationSession session, String to, String input) {
        if (input.length() < 10) {
            sendText(to, "Please enter a complete delivery address (at least 10 characters).");
            return;
        }
        session.setDeliveryAddress(input);
        session.setState(ConversationState.CONFIRM_ORDER);
        sendOrderSummary(to, session);
    }

    private void sendOrderSummary(String to, ConversationSession session) {
        Cart cart = session.getCart();
        StringBuilder text = new StringBuilder("Order Summary\n\n");
        text.append("Restaurant: ").append(session.getSelectedRestaurant().getName()).append("\n\nItems:\n");
        for (CartItem item : cart.getItems()) {
            text.append("- ").append(item.getMenuItem().getName())
                .append(" x").append(item.getQuantity()).append("\n");
        }
        text.append("\nTotal: Rs.").append((int) cart.getTotal());
        text.append("\nAddress: ").append(session.getDeliveryAddress());
        cloudApiService.sendMessage(payloadBuilder.buildButtonMessage(
            to,
            text.toString(),
            List.of(
                new InteractivePayloadBuilder.Button("confirm_yes", "Confirm Order"),
                new InteractivePayloadBuilder.Button("confirm_no", "Cancel")
            )
        ));
    }

    private void handleConfirmOrder(ConversationSession session, String to, String input) {
        switch (input) {
            case "confirm_yes" -> {
                session.setState(ConversationState.PAYMENT_SELECTION);
                sendPaymentOptions(to, session);
            }
            case "confirm_no" -> {
                session.reset();
                sendText(to, "Order cancelled.");
                sendRestaurantList(to);
            }
            default -> {
                sendText(to, "Please use the buttons to confirm or cancel.");
                sendOrderSummary(to, session);
            }
        }
    }

    private void sendPaymentOptions(String to, ConversationSession session) {
        cloudApiService.sendMessage(payloadBuilder.buildButtonMessage(
            to,
            "Select Payment Method\n\nTotal: Rs." + (int) session.getCart().getTotal(),
            List.of(
                new InteractivePayloadBuilder.Button("pay_upi", "UPI"),
                new InteractivePayloadBuilder.Button("pay_cod", "Cash on Delivery")
            )
        ));
    }

    private void handlePaymentSelection(ConversationSession session, String to, String input) {
        switch (input) {
            case "pay_upi" -> {
                session.setPaymentMethod(PaymentMethod.UPI);
                session.setState(ConversationState.AWAITING_UPI_PAYMENT);
                sendText(to, "UPI Payment\n\nAmount: Rs." + (int) session.getCart().getTotal()
                    + "\n\nUPI ID: " + UPI_ID
                    + "\n\nComplete the payment. Your order will be confirmed automatically once payment is verified.");
            }
            case "pay_cod" -> {
                session.setPaymentMethod(PaymentMethod.COD);
                placeOrder(session, to);
            }
            default -> {
                sendText(to, "Please use the buttons to select a payment method.");
                sendPaymentOptions(to, session);
            }
        }
    }

    private void handleUpiPaymentConfirmation(String to) {
        sendText(to, "Payment verification in progress. Your order will be placed once payment is confirmed.");
    }

    private void placeOrder(ConversationSession session, String to) {
        String orderId = "M" + orderCounter.incrementAndGet();
        session.setPendingOrderId(orderId);
        session.setState(ConversationState.ORDER_PLACED);
        String paymentMode = session.getPaymentMethod() == PaymentMethod.UPI ? "UPI" : "COD";
        sendText(to, "Order Placed!\n\nOrder ID: " + orderId
            + "\nPayment: " + paymentMode
            + "\nEstimated Delivery: " + ESTIMATED_DELIVERY + " minutes"
            + "\n\nThank you for choosing Mealoo! Send Hi to place a new order.");
    }

    private void handleOrderPlaced(ConversationSession session, String to, String input) {
        if (isGreeting(input)) {
            session.reset();
            sendRestaurantList(to);
        } else {
            sendText(to, "Your order " + session.getPendingOrderId()
                + " is being prepared. Send Hi to place a new order.");
        }
    }

    private void sendText(String to, String text) {
        cloudApiService.sendMessage(payloadBuilder.buildTextMessage(to, text));
    }

    private int parseIndex(String input, String prefix) {
        try {
            return Integer.parseInt(input.substring(prefix.length()));
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
