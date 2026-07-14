package com.mealoo.session;

import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionManager {
    private final ConcurrentHashMap<String, ConversationSession> sessions = new ConcurrentHashMap<>();

    public ConversationSession getOrCreate(String phoneNumber) {
        return sessions.computeIfAbsent(phoneNumber, ConversationSession::new);
    }

    public void remove(String phoneNumber) {
        sessions.remove(phoneNumber);
    }
}
