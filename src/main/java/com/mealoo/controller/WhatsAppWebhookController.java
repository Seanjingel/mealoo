package com.mealoo.controller;

import com.mealoo.dto.WhatsAppWebhookPayload;
import com.mealoo.service.OrderWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WhatsAppWebhookController {

    private final OrderWorkflowService orderWorkflowService;

    @Value("${whatsapp.verify.token}")
    private String verifyToken;

    @GetMapping("/whatsapp")
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.challenge") String challenge,
            @RequestParam("hub.verify_token") String token) {
        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(403).build();
    }

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

    @PostMapping("/payment/confirm")
    public ResponseEntity<String> confirmPayment(@RequestParam("phone") String phone) {
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
