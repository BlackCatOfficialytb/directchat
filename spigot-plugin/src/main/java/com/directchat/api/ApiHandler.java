package com.directchat.api;

import com.directchat.DirectChatPlugin;
import com.directchat.auth.TokenManager;
import com.directchat.chat.ChatManager;
import com.directchat.chat.ChatMessage;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * Handles API endpoint logic.
 */
public class ApiHandler {

    private static final Gson GSON = new Gson();

    private final DirectChatPlugin plugin;
    private final TokenManager tokenManager;
    private final ChatManager chatManager;

    public ApiHandler(DirectChatPlugin plugin) {
        this.plugin = plugin;
        this.tokenManager = plugin.getTokenManager();
        this.chatManager = plugin.getChatManager();
    }

    /**
     * Handle /api/auth request.
     * 
     * @param body JSON request body
     * @return JSON response
     */
    public String handleAuth(String body) {
        try {
            JsonObject request = GSON.fromJson(body, JsonObject.class);

            String uuid = request.has("uuid") ? request.get("uuid").getAsString() : null;
            String password = request.has("password") ? request.get("password").getAsString() : null;
            String captchaResponse = request.has("captcha_response") ? request.get("captcha_response").getAsString()
                    : null;

            if (uuid == null || password == null) {
                return errorResponse("Missing uuid or password");
            }

            // Validate password
            if (!plugin.getPassword().equals(password)) {
                plugin.debug("Auth failed for " + uuid + ": invalid password");
                return errorResponse("Invalid password");
            }

            // Check if player is online
            UUID playerUuid;
            try {
                playerUuid = UUID.fromString(uuid);
            } catch (IllegalArgumentException e) {
                return errorResponse("Invalid UUID format");
            }

            Player player = Bukkit.getPlayer(playerUuid);
            if (player == null || !player.isOnline()) {
                return errorResponse("Player not online");
            }

            // Check captcha requirement
            String captchaProvider = plugin.getCaptchaProvider();
            if (!"none".equals(captchaProvider) && captchaResponse == null) {
                // Captcha required but not provided
                return captchaRequiredResponse(captchaProvider, playerUuid);
            }

            // Validate captcha if provided
            if (captchaResponse != null && !"none".equals(captchaProvider)) {
                if (!validateCaptcha(captchaProvider, playerUuid, captchaResponse)) {
                    return captchaRequiredResponse(captchaProvider, playerUuid);
                }
            }

            // Generate token
            String token = tokenManager.generateToken(playerUuid);

            plugin.debug("Auth successful for " + player.getName() + " (" + uuid + ")");
            plugin.getLogger().info("Player " + player.getName() + " authenticated via DirectChat");

            JsonObject response = new JsonObject();
            response.addProperty("status", "OK");
            response.addProperty("token", token);
            response.addProperty("player_name", player.getName());

            return GSON.toJson(response);

        } catch (Exception e) {
            plugin.getLogger().warning("Auth error: " + e.getMessage());
            return errorResponse("Internal error");
        }
    }

    /**
     * Handle /api/send request.
     * 
     * @param token Authorization token
     * @param body  JSON request body
     * @return JSON response
     */
    public String handleSend(String token, String body) {
        try {
            // Validate token
            UUID playerUuid = tokenManager.getPlayerUuid(token);
            if (playerUuid == null) {
                return errorResponse("Invalid or expired token");
            }

            Player player = Bukkit.getPlayer(playerUuid);
            if (player == null || !player.isOnline()) {
                tokenManager.invalidateToken(token);
                return errorResponse("Player not online");
            }

            JsonObject request = GSON.fromJson(body, JsonObject.class);
            String message = request.has("message") ? request.get("message").getAsString() : null;

            if (message == null || message.trim().isEmpty()) {
                return errorResponse("Empty message");
            }

            // Sanitize message
            message = message.trim();
            if (message.length() > 256) {
                message = message.substring(0, 256);
            }

            plugin.debug("Message from " + player.getName() + ": " + message);

            // Check if it's a command
            if (message.startsWith("/")) {
                // Execute command on main thread
                String finalMessage = message;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.performCommand(finalMessage.substring(1));
                });
            } else {
                // Broadcast to authenticated players
                chatManager.broadcastMessage(player, message);
            }

            JsonObject response = new JsonObject();
            response.addProperty("status", "OK");
            return GSON.toJson(response);

        } catch (Exception e) {
            plugin.getLogger().warning("Send error: " + e.getMessage());
            return errorResponse("Internal error");
        }
    }

    /**
     * Handle /api/fetch request.
     * 
     * @param token Authorization token
     * @param since Timestamp to fetch messages after
     * @return JSON response
     */
    public String handleFetch(String token, long since) {
        try {
            // Validate token
            UUID playerUuid = tokenManager.getPlayerUuid(token);
            if (playerUuid == null) {
                return errorResponse("Invalid or expired token");
            }

            // Get messages since timestamp
            List<ChatMessage> messages = chatManager.getMessagesSince(since);

            JsonArray messagesArray = new JsonArray();
            for (ChatMessage msg : messages) {
                JsonObject msgObj = new JsonObject();
                msgObj.addProperty("sender", msg.senderName());
                msgObj.addProperty("message", msg.message());
                msgObj.addProperty("timestamp", msg.timestamp());
                messagesArray.add(msgObj);
            }

            JsonObject response = new JsonObject();
            response.addProperty("status", "OK");
            response.add("messages", messagesArray);

            return GSON.toJson(response);

        } catch (Exception e) {
            plugin.getLogger().warning("Fetch error: " + e.getMessage());
            return errorResponse("Internal error");
        }
    }

    /**
     * Generate error response.
     */
    private String errorResponse(String message) {
        JsonObject response = new JsonObject();
        response.addProperty("status", "ERROR");
        response.addProperty("message", message);
        return GSON.toJson(response);
    }

    /**
     * Generate captcha required response.
     */
    private String captchaRequiredResponse(String provider, UUID playerUuid) {
        JsonObject response = new JsonObject();
        response.addProperty("status", "CAPTCHA_REQUIRED");

        // Generate captcha based on provider
        String captchaData = generateCaptcha(provider, playerUuid);
        if (captchaData != null) {
            response.addProperty("captcha_image", captchaData);
        }

        return GSON.toJson(response);
    }

    /**
     * Generate captcha for player.
     */
    private String generateCaptcha(String provider, UUID playerUuid) {
        switch (provider.toLowerCase()) {
            case "nantibot":
                // Try to integrate with nAntiBot
                return generateNAntiBotCaptcha(playerUuid);
            case "captcha-api":
                // Try to use Captcha API plugin
                return generateCaptchaApi(playerUuid);
            default:
                // Simple math captcha as fallback
                return generateSimpleCaptcha(playerUuid);
        }
    }

    /**
     * Validate captcha response.
     */
    private boolean validateCaptcha(String provider, UUID playerUuid, String response) {
        switch (provider.toLowerCase()) {
            case "nantibot":
                return validateNAntiBotCaptcha(playerUuid, response);
            case "captcha-api":
                return validateCaptchaApi(playerUuid, response);
            default:
                return validateSimpleCaptcha(playerUuid, response);
        }
    }

    // Simple captcha storage (for basic implementation)
    private final java.util.Map<UUID, String> simpleCaptchas = new java.util.concurrent.ConcurrentHashMap<>();

    private String generateSimpleCaptcha(UUID playerUuid) {
        int a = (int) (Math.random() * 10) + 1;
        int b = (int) (Math.random() * 10) + 1;
        String answer = String.valueOf(a + b);
        simpleCaptchas.put(playerUuid, answer);
        return "What is " + a + " + " + b + "?";
    }

    private boolean validateSimpleCaptcha(UUID playerUuid, String response) {
        String expected = simpleCaptchas.get(playerUuid);
        if (expected != null && expected.equals(response.trim())) {
            simpleCaptchas.remove(playerUuid);
            return true;
        }
        return false;
    }

    // nAntiBot integration stubs
    private String generateNAntiBotCaptcha(UUID playerUuid) {
        // TODO: Integrate with nAntiBot API when available
        plugin.debug("nAntiBot integration not implemented, using simple captcha");
        return generateSimpleCaptcha(playerUuid);
    }

    private boolean validateNAntiBotCaptcha(UUID playerUuid, String response) {
        // TODO: Integrate with nAntiBot API when available
        return validateSimpleCaptcha(playerUuid, response);
    }

    // Captcha API integration stubs
    private String generateCaptchaApi(UUID playerUuid) {
        // TODO: Integrate with Captcha API plugin when available
        plugin.debug("Captcha API integration not implemented, using simple captcha");
        return generateSimpleCaptcha(playerUuid);
    }

    private boolean validateCaptchaApi(UUID playerUuid, String response) {
        // TODO: Integrate with Captcha API plugin when available
        return validateSimpleCaptcha(playerUuid, response);
    }
}
