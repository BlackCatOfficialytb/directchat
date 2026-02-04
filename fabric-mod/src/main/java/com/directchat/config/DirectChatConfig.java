package com.directchat.config;

import com.directchat.DirectChatMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuration manager for DirectChat mod.
 * Stores connection URL, password, and authentication token.
 */
public class DirectChatConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private transient Path configPath;

    // Configuration fields
    private String currentUrl = "";
    private String password = "";
    private String authToken = null;
    private boolean directModeEnabled = false;

    public DirectChatConfig() {
        this.configPath = FabricLoader.getInstance()
                .getConfigDir()
                .resolve("directchat.json");
    }

    /**
     * Load configuration from disk.
     */
    public void load() {
        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                DirectChatConfig loaded = GSON.fromJson(json, DirectChatConfig.class);
                if (loaded != null) {
                    this.currentUrl = loaded.currentUrl;
                    this.password = loaded.password;
                    this.authToken = loaded.authToken;
                    this.directModeEnabled = loaded.directModeEnabled;
                }
                DirectChatMod.LOGGER.info("Configuration loaded from {}", configPath);
            } catch (IOException e) {
                DirectChatMod.LOGGER.error("Failed to load config", e);
            }
        } else {
            save(); // Create default config
            DirectChatMod.LOGGER.info("Created default configuration at {}", configPath);
        }
    }

    /**
     * Save configuration to disk.
     */
    public void save() {
        try {
            Files.createDirectories(configPath.getParent());
            String json = GSON.toJson(this);
            Files.writeString(configPath, json);
        } catch (IOException e) {
            DirectChatMod.LOGGER.error("Failed to save config", e);
        }
    }

    // Getters and setters

    public String getCurrentUrl() {
        return currentUrl;
    }

    public void setCurrentUrl(String url) {
        this.currentUrl = url;
        save();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
        save();
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String token) {
        this.authToken = token;
        save();
    }

    public boolean isDirectModeEnabled() {
        return directModeEnabled;
    }

    public void setDirectModeEnabled(boolean enabled) {
        this.directModeEnabled = enabled;
        save();
    }

    /**
     * Check if using HTTPS and warn if not.
     */
    public boolean isSecureConnection() {
        return currentUrl != null && currentUrl.toLowerCase().startsWith("https://");
    }
}
