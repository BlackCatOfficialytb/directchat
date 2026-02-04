package com.directchat.client;

import com.directchat.DirectChatMod;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Intercepts chat messages and redirects them to the DirectChat API when Direct
 * Mode is enabled.
 */
public class ChatInterceptor {

    private static final String COMMAND_PREFIX = "/directchat";

    /**
     * Register chat interception events.
     */
    public static void register() {
        // Intercept chat messages
        ClientSendMessageEvents.ALLOW_CHAT.register(ChatInterceptor::onChatMessage);

        // Intercept commands (for blocking in secure mode)
        ClientSendMessageEvents.ALLOW_COMMAND.register(ChatInterceptor::onCommand);

        DirectChatMod.LOGGER.info("Chat interceptor registered");
    }

    /**
     * Called when player tries to send a chat message.
     * 
     * @param message The chat message
     * @return false to cancel sending to server, true to allow
     */
    private static boolean onChatMessage(String message) {
        DirectChatMod mod = DirectChatMod.getInstance();

        // Always allow if not in Direct Mode
        if (!mod.isDirectModeEnabled()) {
            return true;
        }

        // Check if connected
        if (!mod.isConnected()) {
            sendClientMessage("§c[DirectChat] Not connected! Use /directchat connect <url> <password>");
            return false; // Block message
        }

        // Redirect message to API
        mod.getApiClient().sendMessage(message)
                .thenAccept(success -> {
                    if (success) {
                        // Show sent message locally (will be echoed back from server too)
                        MinecraftClient.getInstance().execute(() -> {
                            sendClientMessage("§7[You] §f" + message);
                        });
                    } else {
                        MinecraftClient.getInstance().execute(() -> {
                            sendClientMessage("§c[DirectChat] Failed to send message!");
                        });
                    }
                });

        return false; // Cancel vanilla chat packet
    }

    /**
     * Called when player tries to send a command.
     * 
     * @param command The command (without leading /)
     * @return false to cancel, true to allow
     */
    private static boolean onCommand(String command) {
        DirectChatMod mod = DirectChatMod.getInstance();

        // Always allow /directchat commands
        if (command.startsWith("directchat")) {
            return true;
        }

        // If Direct Mode is ON and connected, block all other commands
        if (mod.isDirectModeEnabled() && mod.isConnected()) {
            // Send command through API instead
            mod.getApiClient().sendMessage("/" + command)
                    .thenAccept(success -> {
                        if (!success) {
                            MinecraftClient.getInstance().execute(() -> {
                                sendClientMessage("§c[DirectChat] Failed to send command!");
                            });
                        }
                    });
            return false; // Block vanilla command
        }

        return true; // Allow command
    }

    /**
     * Send a client-side message to the player.
     */
    public static void sendClientMessage(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.literal(message), false);
        }
    }

    /**
     * Display a message in the chat HUD.
     */
    public static void displayChatMessage(String sender, String message, long timestamp) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.inGameHud != null) {
            String formatted = String.format("§b[DC] §e%s§7: §f%s", sender, message);
            client.execute(() -> {
                client.inGameHud.getChatHud().addMessage(Text.literal(formatted));
            });
        }
    }

    /**
     * Display a warning message (e.g., for insecure connection).
     */
    public static void displayWarning(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.execute(() -> {
                client.player.sendMessage(
                        Text.literal("§6⚠ [DirectChat] " + message).formatted(Formatting.GOLD),
                        false);
            });
        }
    }
}
