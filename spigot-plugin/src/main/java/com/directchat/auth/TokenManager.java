package com.directchat.auth;

import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages authentication tokens for DirectChat users.
 */
public class TokenManager {

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int TOKEN_LENGTH = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final int tokenExpirySeconds;

    // Token -> PlayerUUID
    private final Map<String, TokenData> tokens = new ConcurrentHashMap<>();
    // PlayerUUID -> Token (for lookup)
    private final Map<UUID, String> playerTokens = new ConcurrentHashMap<>();

    public TokenManager(int tokenExpirySeconds) {
        this.tokenExpirySeconds = tokenExpirySeconds;
    }

    /**
     * Generate a new authentication token for a player.
     * Invalidates any existing token for that player.
     */
    public String generateToken(UUID playerUuid) {
        // Invalidate existing token
        String existingToken = playerTokens.get(playerUuid);
        if (existingToken != null) {
            tokens.remove(existingToken);
        }

        // Generate new token
        String token = generateRandomToken();
        long expiry = tokenExpirySeconds > 0
                ? System.currentTimeMillis() + (tokenExpirySeconds * 1000L)
                : 0; // 0 = never expires

        tokens.put(token, new TokenData(playerUuid, expiry));
        playerTokens.put(playerUuid, token);

        return token;
    }

    /**
     * Get the player UUID for a token.
     * Returns null if token is invalid or expired.
     */
    public UUID getPlayerUuid(String token) {
        TokenData data = tokens.get(token);
        if (data == null) {
            return null;
        }

        // Check expiry
        if (data.expiry > 0 && System.currentTimeMillis() > data.expiry) {
            invalidateToken(token);
            return null;
        }

        return data.playerUuid;
    }

    /**
     * Check if a player is authenticated.
     */
    public boolean isAuthenticated(UUID playerUuid) {
        String token = playerTokens.get(playerUuid);
        if (token == null) {
            return false;
        }

        TokenData data = tokens.get(token);
        if (data == null) {
            return false;
        }

        // Check expiry
        if (data.expiry > 0 && System.currentTimeMillis() > data.expiry) {
            invalidateToken(token);
            return false;
        }

        return true;
    }

    /**
     * Invalidate a token.
     */
    public void invalidateToken(String token) {
        TokenData data = tokens.remove(token);
        if (data != null) {
            playerTokens.remove(data.playerUuid);
        }
    }

    /**
     * Invalidate all tokens for a player.
     */
    public void invalidatePlayer(UUID playerUuid) {
        String token = playerTokens.remove(playerUuid);
        if (token != null) {
            tokens.remove(token);
        }
    }

    /**
     * Clear all tokens.
     */
    public void clearAll() {
        tokens.clear();
        playerTokens.clear();
    }

    /**
     * Get all authenticated player UUIDs.
     */
    public java.util.Set<UUID> getAuthenticatedPlayers() {
        return new java.util.HashSet<>(playerTokens.keySet());
    }

    /**
     * Generate a random 16-character token.
     */
    private String generateRandomToken() {
        StringBuilder sb = new StringBuilder(TOKEN_LENGTH);
        for (int i = 0; i < TOKEN_LENGTH; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * Token data record.
     */
    private record TokenData(UUID playerUuid, long expiry) {
    }
}
