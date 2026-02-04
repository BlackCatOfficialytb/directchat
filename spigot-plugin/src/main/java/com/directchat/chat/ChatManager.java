package com.directchat.chat;

import com.directchat.DirectChatPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * Manages chat messages and broadcasting for DirectChat.
 */
public class ChatManager {

    private final int maxHistorySize;
    private final LinkedList<ChatMessage> messageHistory = new LinkedList<>();

    public ChatManager(int maxHistorySize) {
        this.maxHistorySize = maxHistorySize;
    }

    /**
     * Broadcast a message from a player to all authenticated DirectChat users.
     */
    public void broadcastMessage(Player sender, String message) {
        DirectChatPlugin plugin = DirectChatPlugin.getInstance();

        // Create message record
        ChatMessage chatMessage = new ChatMessage(
                sender.getUniqueId().toString(),
                sender.getName(),
                message,
                System.currentTimeMillis());

        // Store in history
        synchronized (messageHistory) {
            messageHistory.addLast(chatMessage);
            while (messageHistory.size() > maxHistorySize) {
                messageHistory.removeFirst();
            }
        }

        // Format message
        String formattedMessage = "§b[DC] §e" + sender.getName() + "§7: §f" + message;

        // Broadcast to authenticated players
        for (UUID playerUuid : plugin.getTokenManager().getAuthenticatedPlayers()) {
            Player player = Bukkit.getPlayer(playerUuid);
            if (player != null && player.isOnline()) {
                player.sendMessage(formattedMessage);
            }
        }

        // Also log to console
        plugin.getLogger().info("[DirectChat] " + sender.getName() + ": " + message);
    }

    /**
     * Broadcast a system message to all authenticated users.
     */
    public void broadcastSystemMessage(String message) {
        DirectChatPlugin plugin = DirectChatPlugin.getInstance();

        String formattedMessage = "§6[DC System] §f" + message;

        for (UUID playerUuid : plugin.getTokenManager().getAuthenticatedPlayers()) {
            Player player = Bukkit.getPlayer(playerUuid);
            if (player != null && player.isOnline()) {
                player.sendMessage(formattedMessage);
            }
        }
    }

    /**
     * Get messages since a specific timestamp.
     */
    public List<ChatMessage> getMessagesSince(long since) {
        List<ChatMessage> result = new ArrayList<>();

        synchronized (messageHistory) {
            for (ChatMessage msg : messageHistory) {
                if (msg.timestamp() > since) {
                    result.add(msg);
                }
            }
        }

        return result;
    }

    /**
     * Get all messages in history.
     */
    public List<ChatMessage> getAllMessages() {
        synchronized (messageHistory) {
            return new ArrayList<>(messageHistory);
        }
    }

    /**
     * Clear message history.
     */
    public void clearHistory() {
        synchronized (messageHistory) {
            messageHistory.clear();
        }
    }
}
