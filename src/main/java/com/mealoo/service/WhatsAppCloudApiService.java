package com.mealoo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppCloudApiService {

    @Value("${whatsapp.cloud.api.url}")
    private String apiUrl;

    @Value("${whatsapp.cloud.access.token}")
    private String accessToken;

    private final RestTemplate restTemplate;

    public void sendMessage(Map<String, Object> payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        try {
            restTemplate.postForObject(apiUrl, request, String.class);
        } catch (Exception e) {
            log.error("Failed to send WhatsApp message to {}: {}", payload.get("to"), e.getMessage());
        }
    }
}
