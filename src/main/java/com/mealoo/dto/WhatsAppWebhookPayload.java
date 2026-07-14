package com.mealoo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WhatsAppWebhookPayload {

    @JsonProperty("object")
    private String object;
    private List<Entry> entry;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Entry {
        private List<Change> changes;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Change {
        private ChangeValue value;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChangeValue {
        private List<Message> messages;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        private String from;
        private String type;
        private TextBody text;
        private Interactive interactive;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TextBody {
        private String body;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Interactive {
        private String type;
        @JsonProperty("list_reply")
        private Reply listReply;
        @JsonProperty("button_reply")
        private Reply buttonReply;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Reply {
        private String id;
        private String title;
    }
}
