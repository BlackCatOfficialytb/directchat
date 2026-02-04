package com.directchat.client;

import com.directchat.DirectChatMod;
import com.directchat.api.ApiClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Background polling service that fetches messages from the DirectChat API.
 */
public class MessagePoller {
    
    private static final long POLL_INTERVAL_MS = 1000; // 1 second
    
    private final ApiClient apiClient;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> pollingTask;
    private long lastMessageTimestamp = 0;
    
    public MessagePoller(ApiClient apiClient) {
        this.apiClient = apiClient;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "DirectChat-Poller");
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * Start polling for messages.
     */
    public void start() {
        if (pollingTask != null && !pollingTask.isCancelled()) {
            return; // Already running
        }
        
        lastMessageTimestamp = System.currentTimeMillis();
        
        pollingTask = scheduler.scheduleAtFixedRate(
                this::pollMessages,
                0,
                POLL_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
        
        DirectChatMod.LOGGER.info("Message poller started");
    }
    
    /**
     * Stop polling for messages.
     */
    public void stop() {
        if (pollingTask != null) {
            pollingTask.cancel(false);
            pollingTask = null;
        }
        DirectChatMod.LOGGER.info("Message poller stopped");
    }
    
    /**
     * Shutdown the scheduler completely.
     */
    public void shutdown() {
        stop();
        scheduler.shutdown();
    }
    
    /**
     * Poll for new messages.
     */
    private void pollMessages() {
        DirectChatMod mod = DirectChatMod.getInstance();
        
        // Don't poll if not connected or direct mode is off
        if (!mod.isConnected() || !mod.isDirectModeEnabled()) {
            return;
        }
        
        // Don't poll if player is not in game
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        
        apiClient.fetchMessages(lastMessageTimestamp)
                .thenAccept(result -> {
                    if (!result.success()) {
                        return;
                    }
                    
                    JsonArray messages = result.messages();
                    for (int i = 0; i < messages.size(); i++) {
                        JsonObject msg = messages.get(i).getAsJsonObject();
                        
                        String sender = msg.has("sender") ? msg.get("sender").getAsString() : "Unknown";
                        String message = msg.has("message") ? msg.get("message").getAsString() : "";
                        long timestamp = msg.has("timestamp") ? msg.get("timestamp").getAsLong() : 0;
                        
                        // Update last timestamp
                        if (timestamp > lastMessageTimestamp) {
                            lastMessageTimestamp = timestamp;
                        }
                        
                        // Display message in chat
                        ChatInterceptor.displayChatMessage(sender, message, timestamp);
                    }
                })
                .exceptionally(e -> {
                    DirectChatMod.LOGGER.error("Error polling messages", e);
                    return null;
                });
    }
    
    /**
     * Reset the last message timestamp (useful when reconnecting).
     */
    public void resetTimestamp() {
        lastMessageTimestamp = System.currentTimeMillis();
    }
}
