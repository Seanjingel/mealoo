package com.mealoo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI mealooOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Mealoo WhatsApp Bot API")
                        .description("""
                                REST API for the Mealoo food ordering chatbot over WhatsApp Business Cloud API (Meta).

                                **Flow summary:**
                                1. Meta sends incoming messages to `POST /webhook/whatsapp`
                                2. Bot replies via outbound REST calls to `graph.facebook.com`
                                3. Use `/api/test/*` endpoints to simulate the full flow locally without a real WhatsApp connection
                                4. Use `POST /webhook/payment/confirm` to acknowledge UPI payments
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Mealoo Team")
                                .email("support@mealoo.in")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local development"),
                        new Server().url("https://your-app.up.railway.app").description("Production (update URL)")
                ));
    }
}
