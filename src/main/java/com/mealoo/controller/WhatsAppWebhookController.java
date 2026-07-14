package com.mealoo.controller;

import com.mealoo.dto.WhatsAppWebhookPayload;
import com.mealoo.service.OrderWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
@Tag(name = "WhatsApp Webhook", description = "Endpoints consumed by Meta / WhatsApp Business Cloud API")
public class WhatsAppWebhookController {

    private final OrderWorkflowService orderWorkflowService;

    @Value("${whatsapp.verify.token}")
    private String verifyToken;

    @Operation(
        summary = "Meta webhook verification",
        description = "Meta calls this GET endpoint once when you register the webhook URL in the Developer Console. "
                + "Returns the hub.challenge value when the verify token matches."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token matched — returns hub.challenge"),
        @ApiResponse(responseCode = "403", description = "Token mismatch")
    })
    @GetMapping("/whatsapp")
    public ResponseEntity<String> verifyWebhook(
            @Parameter(description = "Must be 'subscribe'", example = "subscribe")
            @RequestParam("hub.mode") String mode,
            @Parameter(description = "Random string Meta will expect back in the response body", example = "CHALLENGE_ACCEPTED")
            @RequestParam("hub.challenge") String challenge,
            @Parameter(description = "Must match whatsapp.verify.token in application.properties", example = "mealoo_verify_token_2024")
            @RequestParam("hub.verify_token") String token) {
        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(403).build();
    }

    @Operation(
        summary = "Receive incoming WhatsApp message",
        description = "Meta POSTs all incoming messages here. Handles both plain text messages and "
                + "interactive replies (list_reply from List Messages, button_reply from Reply Buttons). "
                + "Always returns 200 OK — Meta retries on any non-2xx response."
    )
    @ApiResponse(responseCode = "200", description = "Accepted — bot processes message asynchronously")
    @PostMapping("/whatsapp")
    public ResponseEntity<String> handleIncomingMessage(@RequestBody WhatsAppWebhookPayload payload) {
        if (payload.getEntry() == null) {
            return ResponseEntity.ok("OK");
        }
        payload.getEntry().stream()
            .filter(e -> e.getChanges() != null)
            .flatMap(e -> e.getChanges().stream())
            .filter(c -> c.getValue() != null && c.getValue().getMessages() != null)
            .flatMap(c -> c.getValue().getMessages().stream())
            .forEach(msg -> {
                String from = msg.getFrom();
                String text = extractText(msg);
                if (from != null && text != null && !text.isBlank()) {
                    orderWorkflowService.processMessage(from, text);
                }
            });
        return ResponseEntity.ok("OK");
    }

    @Operation(
        summary = "UPI payment confirmation webhook",
        description = "Called by your payment gateway (or manually via Postman) after a UPI payment is received. "
                + "Moves the session from AWAITING_UPI_PAYMENT → ORDER_PLACED and sends confirmation to the customer."
    )
    @ApiResponse(responseCode = "200", description = "Payment confirmed and order placed")
    @PostMapping("/payment/confirm")
    public ResponseEntity<String> confirmPayment(
            @Parameter(description = "Customer's WhatsApp phone number (country code + number, no +)", example = "919876543210")
            @RequestParam("phone") String phone) {
        orderWorkflowService.confirmUpiPayment(phone);
        return ResponseEntity.ok("Payment confirmed");
    }

    private String extractText(WhatsAppWebhookPayload.Message msg) {
        if ("text".equals(msg.getType()) && msg.getText() != null) {
            return msg.getText().getBody();
        }
        if ("interactive".equals(msg.getType()) && msg.getInteractive() != null) {
            WhatsAppWebhookPayload.Interactive ia = msg.getInteractive();
            if ("list_reply".equals(ia.getType()) && ia.getListReply() != null) {
                return ia.getListReply().getId();
            }
            if ("button_reply".equals(ia.getType()) && ia.getButtonReply() != null) {
                return ia.getButtonReply().getId();
            }
        }
        return null;
    }
}
