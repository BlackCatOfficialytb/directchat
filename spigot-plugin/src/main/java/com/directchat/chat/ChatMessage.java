package com.directchat.chat;

/**
 * Represents a chat message in the DirectChat system.
 */
public record ChatMessage(
        String senderUuid,
        String senderName,
        String message,
        long timestamp) {
}
