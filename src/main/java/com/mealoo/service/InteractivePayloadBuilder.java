package com.mealoo.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class InteractivePayloadBuilder {

    public Map<String, Object> buildListMessage(String to, String headerText, String bodyText,
                                                 String buttonText, List<Section> sections) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("messaging_product", "whatsapp");
        message.put("to", to);
        message.put("type", "interactive");

        Map<String, Object> interactive = new LinkedHashMap<>();
        interactive.put("type", "list");
        interactive.put("header", Map.of("type", "text", "text", truncate(headerText, 60)));
        interactive.put("body", Map.of("text", truncate(bodyText, 1024)));

        List<Map<String, Object>> sectionsList = new ArrayList<>();
        for (Section section : sections) {
            Map<String, Object> sectionMap = new LinkedHashMap<>();
            sectionMap.put("title", truncate(section.getTitle(), 24));
            List<Map<String, Object>> rows = new ArrayList<>();
            for (Row row : section.getRows()) {
                Map<String, Object> rowMap = new LinkedHashMap<>();
                rowMap.put("id", row.getId());
                rowMap.put("title", truncate(row.getTitle(), 24));
                if (row.getDescription() != null && !row.getDescription().isEmpty()) {
                    rowMap.put("description", truncate(row.getDescription(), 72));
                }
                rows.add(rowMap);
            }
            sectionMap.put("rows", rows);
            sectionsList.add(sectionMap);
        }

        Map<String, Object> action = new LinkedHashMap<>();
        action.put("button", truncate(buttonText, 20));
        action.put("sections", sectionsList);
        interactive.put("action", action);

        message.put("interactive", interactive);
        return message;
    }

    public Map<String, Object> buildButtonMessage(String to, String bodyText, List<Button> buttons) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("messaging_product", "whatsapp");
        message.put("to", to);
        message.put("type", "interactive");

        Map<String, Object> interactive = new LinkedHashMap<>();
        interactive.put("type", "button");
        interactive.put("body", Map.of("text", truncate(bodyText, 1024)));

        List<Map<String, Object>> buttonList = new ArrayList<>();
        for (Button button : buttons) {
            buttonList.add(Map.of(
                "type", "reply",
                "reply", Map.of("id", button.getId(), "title", truncate(button.getTitle(), 20))
            ));
        }

        interactive.put("action", Map.of("buttons", buttonList));
        message.put("interactive", interactive);
        return message;
    }

    public Map<String, Object> buildTextMessage(String to, String text) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("messaging_product", "whatsapp");
        message.put("to", to);
        message.put("type", "text");
        message.put("text", Map.of("body", text));
        return message;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength - 1) + "…" : text;
    }

    @Data
    @AllArgsConstructor
    public static class Section {
        private String title;
        private List<Row> rows;
    }

    @Data
    @AllArgsConstructor
    public static class Row {
        private String id;
        private String title;
        private String description;
    }

    @Data
    @AllArgsConstructor
    public static class Button {
        private String id;
        private String title;
    }
}
