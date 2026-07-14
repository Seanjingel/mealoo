package com.mealoo.controller;

import com.mealoo.session.ConversationSession;
import com.mealoo.session.SessionManager;
import com.mealoo.service.OrderWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Tag(name = "Test / Debug", description = "Local testing shortcuts — simulate WhatsApp messages without a real device or Meta webhook. Remove or secure in production.")
public class TestController {

    private final OrderWorkflowService orderWorkflowService;
    private final SessionManager sessionManager;

    @Operation(
        summary = "Send a simulated message",
        description = """
                Directly invokes `processMessage(phone, text)` — skips the full Meta webhook JSON envelope.

                **Button IDs to use as `text`:**
                | State | Valid text values |
                |---|---|
                | SELECT_RESTAURANT | `Hi`, `rest_0` … `rest_4` |
                | SELECT_CATEGORY | `cat_0` … `cat_3` |
                | SELECT_ITEMS | `item_0` … `item_N`, `item_more` |
                | REVIEW_CART | `cart_more`, `cart_checkout` |
                | COLLECT_ADDRESS | any string ≥ 10 chars |
                | CONFIRM_ORDER | `confirm_yes`, `confirm_no` |
                | PAYMENT_SELECTION | `pay_upi`, `pay_cod` |
                """
    )
    @ApiResponse(responseCode = "200", description = "Message processed — check app logs for outbound WhatsApp API call")
    @PostMapping("/message")
    public ResponseEntity<String> sendMessage(
            @Parameter(description = "WhatsApp phone number (country code + number)", example = "919876543210")
            @RequestParam String phone,
            @Parameter(description = "Message text or button ID", example = "Hi")
            @RequestParam String text) {
        orderWorkflowService.processMessage(phone, text);
        return ResponseEntity.ok("Message processed. Check WhatsApp Cloud API logs for outbound call.");
    }

    @Operation(
        summary = "Confirm UPI payment (test shortcut)",
        description = "Mirrors `POST /webhook/payment/confirm`. Moves the session from AWAITING_UPI_PAYMENT to ORDER_PLACED."
    )
    @ApiResponse(responseCode = "200", description = "UPI payment confirmed and order placed")
    @PostMapping("/payment/confirm")
    public ResponseEntity<String> confirmPayment(
            @Parameter(description = "Customer phone number", example = "919876543210")
            @RequestParam String phone) {
        orderWorkflowService.confirmUpiPayment(phone);
        return ResponseEntity.ok("UPI payment confirmed for " + phone);
    }

    @Operation(
        summary = "Inspect session state",
        description = "Returns the full in-memory session for a phone number: current conversation state, cart, address, payment method, order ID."
    )
    @ApiResponse(responseCode = "200", description = "Session info (or 'no active session' if not started)")
    @GetMapping("/session/{phone}")
    public ResponseEntity<?> getSession(
            @Parameter(description = "Customer phone number", example = "919876543210")
            @PathVariable String phone) {
        ConversationSession session = sessionManager.get(phone);
        if (session == null) {
            return ResponseEntity.ok(Map.of("phone", phone, "status", "no active session"));
        }
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("phone", session.getPhoneNumber());
        info.put("state", session.getState());
        info.put("itemPage", session.getItemPage());
        info.put("restaurant", session.getSelectedRestaurant() != null
                ? session.getSelectedRestaurant().getName() : null);
        info.put("category", session.getSelectedCategory() != null
                ? session.getSelectedCategory().getName() : null);
        info.put("cartItems", session.getCart().getItems().size());
        info.put("cartTotal", session.getCart().getTotal());
        info.put("deliveryAddress", session.getDeliveryAddress());
        info.put("paymentMethod", session.getPaymentMethod());
        info.put("pendingOrderId", session.getPendingOrderId());
        return ResponseEntity.ok(info);
    }

    @Operation(
        summary = "Reset session",
        description = "Deletes the in-memory session — customer starts fresh from SELECT_RESTAURANT on next message."
    )
    @ApiResponse(responseCode = "200", description = "Session cleared")
    @DeleteMapping("/session/{phone}")
    public ResponseEntity<String> resetSession(
            @Parameter(description = "Customer phone number", example = "919876543210")
            @PathVariable String phone) {
        sessionManager.remove(phone);
        return ResponseEntity.ok("Session cleared for " + phone);
    }

    @Operation(
        summary = "List all active sessions",
        description = "Returns the count and phone numbers of all customers with an in-memory session."
    )
    @ApiResponse(responseCode = "200", description = "Active session list")
    @GetMapping("/sessions")
    public ResponseEntity<Map<String, Object>> listSessions() {
        Set<String> phones = sessionManager.activePhones();
        return ResponseEntity.ok(Map.of("activeSessions", phones.size(), "phones", phones));
    }
}
