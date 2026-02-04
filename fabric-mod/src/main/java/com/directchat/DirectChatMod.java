package com.directchat;

import com.directchat.client.ChatInterceptor;
import com.directchat.client.CommandHandler;
import com.directchat.client.MessagePoller;
import com.directchat.config.DirectChatConfig;
import com.directchat.api.ApiClient;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for DirectChat Fabric mod.
 * Intercepts local chat and redirects traffic to an external HTTP/HTTPS API.
 */
public class DirectChatMod implements ClientModInitializer {
    
    public static final String MOD_ID = "directchat";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    private static DirectChatMod instance;
    private DirectChatConfig config;
    private ApiClient apiClient;
    private MessagePoller messagePoller;
    private boolean directModeEnabled = false;
    private boolean connected = false;
    
    @Override
    public void onInitializeClient() {
        instance = this;
        LOGGER.info("DirectChat mod initializing...");
        
        // Load configuration
        config = new DirectChatConfig();
        config.load();
        
        // Initialize API client
        apiClient = new ApiClient(config);
        
        // Initialize message poller (starts when connected)
        messagePoller = new MessagePoller(apiClient);
        
        // Register chat interceptor
        ChatInterceptor.register();
        
        // Register commands
        CommandHandler.register();
        
        LOGGER.info("DirectChat mod initialized successfully!");
    }
    
    public static DirectChatMod getInstance() {
        return instance;
    }
    
    public DirectChatConfig getConfig() {
        return config;
    }
    
    public ApiClient getApiClient() {
        return apiClient;
    }
    
    public MessagePoller getMessagePoller() {
        return messagePoller;
    }
    
    public boolean isDirectModeEnabled() {
        return directModeEnabled;
    }
    
    public void setDirectModeEnabled(boolean enabled) {
        this.directModeEnabled = enabled;
        if (enabled) {
            LOGGER.info("Direct Mode enabled - all chat will be redirected to API");
        } else {
            LOGGER.info("Direct Mode disabled - chat will be sent normally");
        }
    }
    
    public boolean isConnected() {
        return connected;
    }
    
    public void setConnected(boolean connected) {
        this.connected = connected;
        if (connected) {
            messagePoller.start();
        } else {
            messagePoller.stop();
        }
    }
    
    public void disconnect() {
        setConnected(false);
        config.setAuthToken(null);
        config.save();
        LOGGER.info("Disconnected from DirectChat server");
    }
}
