package com.directchat.api;

import com.directchat.DirectChatMod;
import com.directchat.config.DirectChatConfig;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP client for communicating with the DirectChat API server.
 */
public class ApiClient {

    private static final Gson GSON = new Gson();
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final DirectChatConfig config;
    private final HttpClient httpClient;

    public ApiClient(DirectChatConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
    }

    /**
     * Authenticate with the DirectChat server.
     * 
     * @param url        Server URL
     * @param password   Authentication password
     * @param playerUuid Player's UUID
     * @return AuthResult containing status and token
     */
    public CompletableFuture<AuthResult> authenticate(String url, String password, String playerUuid) {
        JsonObject body = new JsonObject();
        body.addProperty("uuid", playerUuid);
        body.addProperty("password", password);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url + "/api/auth"))
                .header("Content-Type", "application/json")
                .timeout(TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    JsonObject json = GSON.fromJson(response.body(), JsonObject.class);
                    String status = json.has("status") ? json.get("status").getAsString() : "ERROR";
                    String token = json.has("token") ? json.get("token").getAsString() : null;
                    String captchaImage = json.has("captcha_image") ? json.get("captcha_image").getAsString() : null;
                    String message = json.has("message") ? json.get("message").getAsString() : null;

                    return new AuthResult(status, token, captchaImage, message);
                })
                .exceptionally(e -> {
                    DirectChatMod.LOGGER.error("Authentication failed", e);
                    return new AuthResult("ERROR", null, null, e.getMessage());
                });
    }

    /**
     * Send a message to the DirectChat server.
     * 
     * @param message Message content
     * @return True if sent successfully
     */
    public CompletableFuture<Boolean> sendMessage(String message) {
        String token = config.getAuthToken();
        if (token == null) {
            return CompletableFuture.completedFuture(false);
        }

        JsonObject body = new JsonObject();
        body.addProperty("message", message);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getCurrentUrl() + "/api/send"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .timeout(TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    JsonObject json = GSON.fromJson(response.body(), JsonObject.class);
                    return json.has("status") && "OK".equals(json.get("status").getAsString());
                })
                .exceptionally(e -> {
                    DirectChatMod.LOGGER.error("Failed to send message", e);
                    return false;
                });
    }

    /**
     * Fetch new messages from the DirectChat server.
     * 
     * @param lastTimestamp Only fetch messages after this timestamp
     * @return Array of messages
     */
    public CompletableFuture<FetchResult> fetchMessages(long lastTimestamp) {
        String token = config.getAuthToken();
        if (token == null) {
            return CompletableFuture.completedFuture(new FetchResult(false, new JsonArray()));
        }

        String url = config.getCurrentUrl() + "/api/fetch";
        if (lastTimestamp > 0) {
            url += "?since=" + lastTimestamp;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .timeout(TIMEOUT)
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    JsonObject json = GSON.fromJson(response.body(), JsonObject.class);
                    JsonArray messages = json.has("messages") ? json.getAsJsonArray("messages") : new JsonArray();
                    return new FetchResult(true, messages);
                })
                .exceptionally(e -> {
                    DirectChatMod.LOGGER.error("Failed to fetch messages", e);
                    return new FetchResult(false, new JsonArray());
                });
    }

    /**
     * Submit captcha verification.
     * 
     * @param captchaResponse The captcha answer
     * @return AuthResult with token if successful
     */
    public CompletableFuture<AuthResult> submitCaptcha(String captchaResponse, String playerUuid) {
        JsonObject body = new JsonObject();
        body.addProperty("uuid", playerUuid);
        body.addProperty("captcha_response", captchaResponse);
        body.addProperty("password", config.getPassword());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getCurrentUrl() + "/api/auth"))
                .header("Content-Type", "application/json")
                .timeout(TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    JsonObject json = GSON.fromJson(response.body(), JsonObject.class);
                    String status = json.has("status") ? json.get("status").getAsString() : "ERROR";
                    String token = json.has("token") ? json.get("token").getAsString() : null;
                    String message = json.has("message") ? json.get("message").getAsString() : null;

                    return new AuthResult(status, token, null, message);
                })
                .exceptionally(e -> {
                    DirectChatMod.LOGGER.error("Captcha submission failed", e);
                    return new AuthResult("ERROR", null, null, e.getMessage());
                });
    }

    /**
     * Result of authentication request.
     */
    public record AuthResult(String status, String token, String captchaImage, String message) {
        public boolean isSuccess() {
            return "OK".equals(status);
        }

        public boolean requiresCaptcha() {
            return "CAPTCHA_REQUIRED".equals(status);
        }
    }

    /**
     * Result of fetch request.
     */
    public record FetchResult(boolean success, JsonArray messages) {
    }
}
