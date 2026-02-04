package com.directchat.client;

import com.directchat.DirectChatMod;
import com.directchat.api.ApiClient;
import com.directchat.config.DirectChatConfig;
import com.directchat.ui.CaptchaScreen;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Handles /directchat commands.
 */
public class CommandHandler {

    /**
     * Register all DirectChat commands.
     */
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("directchat")
                    // /directchat connect <url> <password>
                    .then(ClientCommandManager.literal("connect")
                            .then(ClientCommandManager.argument("url", StringArgumentType.string())
                                    .then(ClientCommandManager.argument("password", StringArgumentType.string())
                                            .executes(context -> {
                                                String url = StringArgumentType.getString(context, "url");
                                                String password = StringArgumentType.getString(context, "password");
                                                return handleConnect(url, password);
                                            }))))

                    // /directchat disconnect
                    .then(ClientCommandManager.literal("disconnect")
                            .executes(context -> handleDisconnect()))

                    // /directchat toggle
                    .then(ClientCommandManager.literal("toggle")
                            .executes(context -> handleToggle()))

                    // /directchat status
                    .then(ClientCommandManager.literal("status")
                            .executes(context -> handleStatus()))

                    // /directchat help
                    .then(ClientCommandManager.literal("help")
                            .executes(context -> handleHelp()))

                    // Default - show help
                    .executes(context -> handleHelp()));
        });

        DirectChatMod.LOGGER.info("Commands registered");
    }

    private static int handleConnect(String url, String password) {
        DirectChatMod mod = DirectChatMod.getInstance();
        DirectChatConfig config = mod.getConfig();
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null) {
            return 0;
        }

        // Normalize URL
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }

        // Remove trailing slash
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        // Check for HTTPS and warn if not used
        if (!url.toLowerCase().startsWith("https://")) {
            ChatInterceptor.displayWarning("Connection is NOT encrypted! Your password may be visible to others.");
        }

        // Store config
        config.setCurrentUrl(url);
        config.setPassword(password);

        String playerUuid = client.player.getUuidAsString();
        String finalUrl = url;

        sendFeedback("§e[DirectChat] Connecting to " + url + "...");

        // Attempt authentication
        mod.getApiClient().authenticate(url, password, playerUuid)
                .thenAccept(result -> {
                    MinecraftClient.getInstance().execute(() -> {
                        if (result.isSuccess()) {
                            config.setAuthToken(result.token());
                            mod.setConnected(true);
                            mod.setDirectModeEnabled(true);
                            sendFeedback("§a[DirectChat] Connected successfully! Direct Mode is now ON.");
                        } else if (result.requiresCaptcha()) {
                            sendFeedback("§e[DirectChat] Captcha required. Opening captcha screen...");
                            // Open captcha screen
                            MinecraftClient.getInstance().setScreen(
                                    new CaptchaScreen(result.captchaImage(), playerUuid));
                        } else {
                            sendFeedback("§c[DirectChat] Authentication failed: " +
                                    (result.message() != null ? result.message() : "Unknown error"));
                        }
                    });
                });

        return 1;
    }

    private static int handleDisconnect() {
        DirectChatMod mod = DirectChatMod.getInstance();

        if (!mod.isConnected()) {
            sendFeedback("§c[DirectChat] Not connected!");
            return 0;
        }

        mod.disconnect();
        mod.setDirectModeEnabled(false);
        sendFeedback("§e[DirectChat] Disconnected from server.");
        return 1;
    }

    private static int handleToggle() {
        DirectChatMod mod = DirectChatMod.getInstance();

        if (!mod.isConnected()) {
            sendFeedback("§c[DirectChat] Not connected! Use /directchat connect first.");
            return 0;
        }

        boolean newState = !mod.isDirectModeEnabled();
        mod.setDirectModeEnabled(newState);
        mod.getConfig().setDirectModeEnabled(newState);

        if (newState) {
            sendFeedback("§a[DirectChat] Direct Mode §lON§r§a - All chat will be redirected to API.");
        } else {
            sendFeedback("§e[DirectChat] Direct Mode §lOFF§r§e - Chat will be sent normally.");
        }

        return 1;
    }

    private static int handleStatus() {
        DirectChatMod mod = DirectChatMod.getInstance();
        DirectChatConfig config = mod.getConfig();

        sendFeedback("§6=== DirectChat Status ===");
        sendFeedback("§7Connected: " + (mod.isConnected() ? "§aYes" : "§cNo"));
        sendFeedback("§7Direct Mode: " + (mod.isDirectModeEnabled() ? "§aON" : "§cOFF"));

        if (config.getCurrentUrl() != null && !config.getCurrentUrl().isEmpty()) {
            sendFeedback("§7Server: §f" + config.getCurrentUrl());
            sendFeedback("§7Secure: " + (config.isSecureConnection() ? "§aYes (HTTPS)" : "§c⚠ No (HTTP)"));
        }

        return 1;
    }

    private static int handleHelp() {
        sendFeedback("§6=== DirectChat Commands ===");
        sendFeedback("§e/directchat connect <url> <password> §7- Connect to a server");
        sendFeedback("§e/directchat disconnect §7- Disconnect from server");
        sendFeedback("§e/directchat toggle §7- Toggle Direct Mode on/off");
        sendFeedback("§e/directchat status §7- Show connection status");
        sendFeedback("§e/directchat help §7- Show this help");
        return 1;
    }

    private static void sendFeedback(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.literal(message), false);
        }
    }
}
