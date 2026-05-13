package com.directchat;

import com.directchat.api.WebServer;
import com.directchat.auth.TokenManager;
import com.directchat.chat.ChatManager;
import com.directchat.listeners.ChatListener;
import com.directchat.tunnel.CloudflaredTunnelManager;
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
    private CloudflaredTunnelManager tunnelManager;

    // Configuration values
    private String password;
    private int port;
    private boolean requireHttps;
    private String captchaProvider;
    private int messageHistorySize;
    private int tokenExpiry;
    private boolean debug;
    private boolean cloudflaredTunnelMode;
    private String cloudflaredPath;

    @Override
    public void onEnable() {
        instance = this;

        // Save default config
        saveDefaultConfig();
        loadConfiguration();

        // Initialize managers
        tokenManager = new TokenManager(tokenExpiry);
        chatManager = new ChatManager(messageHistorySize);

        // Initialize cloudflared tunnel manager
        tunnelManager = new CloudflaredTunnelManager(this, cloudflaredPath);

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

        // Start cloudflared tunnel if enabled
        if (cloudflaredTunnelMode) {
            startCloudflaredTunnel();
        }

        // Register commands
        CommandHandler commandHandler = new CommandHandler(this);
        getCommand("directchat").setExecutor(commandHandler);
        getCommand("directchat").setTabCompleter(commandHandler);
        getCommand("chat").setExecutor(commandHandler);
        getCommand("chat").setTabCompleter(commandHandler);

        // Register event listeners
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);

        getLogger().info("DirectChat plugin enabled successfully!");
    }

    @Override
    public void onDisable() {
        // Stop cloudflared tunnel
        if (tunnelManager != null) {
            tunnelManager.stopTunnel();
        }

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

    /**
     * Start the Cloudflare Quick Tunnel.
     * Called asynchronously — tunnel URL is logged once established.
     */
    private void startCloudflaredTunnel() {
        if (!tunnelManager.isCloudflaredAvailable()) {
            getLogger().severe("[Cloudflared] cloudflared-tunnel-mode is enabled but 'cloudflared' binary was not found!");
            getLogger().severe("[Cloudflared] Please install cloudflared: https://github.com/cloudflare/cloudflared/releases");
            getLogger().severe("[Cloudflared] Or set cloudflared-tunnel-mode to false in config.yml");
            return;
        }

        getLogger().info("[Cloudflared] Tunnel mode enabled — starting Quick Tunnel...");

        tunnelManager.startTunnel(port)
                .thenAccept(url -> getLogger().info("[Cloudflared] Clients can connect via: " + url))
                .exceptionally(e -> {
                    getLogger().log(Level.WARNING, "[Cloudflared] Failed to start tunnel: " + e.getMessage(), e);
                    return null;
                });
    }

    public void loadConfiguration() {
        reloadConfig();

        password = getConfig().getString("password", "changeme");
        port = getConfig().getInt("port", 36769);
        requireHttps = getConfig().getBoolean("require-https", false);
        captchaProvider = getConfig().getString("captcha-provider", "none");
        messageHistorySize = getConfig().getInt("message-history-size", 100);
        tokenExpiry = getConfig().getInt("token-expiry", 3600);
        debug = getConfig().getBoolean("debug", false);
        cloudflaredTunnelMode = getConfig().getBoolean("cloudflared-tunnel-mode", false);
        cloudflaredPath = getConfig().getString("cloudflared-path", "");

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

    public WebServer getWebServer() {
        return webServer;
    }

    public CloudflaredTunnelManager getTunnelManager() {
        return tunnelManager;
    }

    public String getPassword() {
        return password;
    }

    public int getPort() {
        return port;
    }

    public String getCaptchaProvider() {
        return captchaProvider;
    }

    public boolean isRequireHttps() {
        return requireHttps;
    }

    public int getMessageHistorySize() {
        return messageHistorySize;
    }

    public int getTokenExpiry() {
        return tokenExpiry;
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean isCloudflaredTunnelMode() {
        return cloudflaredTunnelMode;
    }

    public String getCloudflaredPath() {
        return cloudflaredPath;
    }

    public void debug(String message) {
        if (debug) {
            getLogger().info("[DEBUG] " + message);
        }
    }
}
