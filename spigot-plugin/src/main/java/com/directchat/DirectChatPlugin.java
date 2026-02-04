package com.directchat;

import com.directchat.api.WebServer;
import com.directchat.auth.TokenManager;
import com.directchat.chat.ChatManager;
import com.directchat.listeners.ChatListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * Main plugin class for DirectChat.
 * Hosts an internal API server for DirectChat mod communication.
 */
public class DirectChatPlugin extends JavaPlugin {

    private static DirectChatPlugin instance;

    private WebServer webServer;
    private TokenManager tokenManager;
    private ChatManager chatManager;

    // Configuration values
    private String password;
    private int port;
    private boolean requireHttps;
    private String captchaProvider;
    private int messageHistorySize;
    private int tokenExpiry;
    private boolean debug;

    @Override
    public void onEnable() {
        instance = this;

        // Save default config
        saveDefaultConfig();
        loadConfiguration();

        // Initialize managers
        tokenManager = new TokenManager(tokenExpiry);
        chatManager = new ChatManager(messageHistorySize);

        // Start web server
        webServer = new WebServer(this, port);
        try {
            webServer.start();
            getLogger().info("DirectChat API server started on port " + port);
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to start API server", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register event listeners
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);

        getLogger().info("DirectChat plugin enabled successfully!");
    }

    @Override
    public void onDisable() {
        // Stop web server
        if (webServer != null) {
            webServer.stop();
            getLogger().info("DirectChat API server stopped");
        }

        // Clear tokens
        if (tokenManager != null) {
            tokenManager.clearAll();
        }

        getLogger().info("DirectChat plugin disabled");
    }

    private void loadConfiguration() {
        reloadConfig();

        password = getConfig().getString("password", "changeme");
        port = getConfig().getInt("port", 36679);
        requireHttps = getConfig().getBoolean("require-https", false);
        captchaProvider = getConfig().getString("captcha-provider", "none");
        messageHistorySize = getConfig().getInt("message-history-size", 100);
        tokenExpiry = getConfig().getInt("token-expiry", 3600);
        debug = getConfig().getBoolean("debug", false);

        if ("changeme".equals(password)) {
            getLogger().warning("Using default password! Please change it in config.yml");
        }
    }

    public static DirectChatPlugin getInstance() {
        return instance;
    }

    public TokenManager getTokenManager() {
        return tokenManager;
    }

    public ChatManager getChatManager() {
        return chatManager;
    }

    public String getPassword() {
        return password;
    }

    public String getCaptchaProvider() {
        return captchaProvider;
    }

    public boolean isDebug() {
        return debug;
    }

    public void debug(String message) {
        if (debug) {
            getLogger().info("[DEBUG] " + message);
        }
    }
}
