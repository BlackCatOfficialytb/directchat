package com.directchat.listeners;

import com.directchat.DirectChatPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listens to player chat events.
 * Blocks unauthenticated players from chatting via normal chat.
 * All commands (including /login, /register, etc.) are allowed to pass through.
 */
public class ChatListener implements Listener {

    private static final String ACCESS_DENIED_MESSAGE = "\u00a7cAccess Denied. Please connect via DirectChat Mod to speak.";

    private final DirectChatPlugin plugin;

    public ChatListener(DirectChatPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Handle chat messages.
     * Cancel if player is not authenticated via DirectChat.
     */
    @SuppressWarnings("deprecation") // Using deprecated event for Spigot compatibility
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        // Check if player has bypass permission
        if (player.hasPermission("directchat.bypass")) {
            return;
        }

        // Check if player is authenticated
        if (!plugin.getTokenManager().isAuthenticated(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(ACCESS_DENIED_MESSAGE);
            plugin.debug("Blocked chat from unauthenticated player: " + player.getName());
        }
    }

    /**
     * Handle player quit.
     * Clean up authentication tokens.
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Invalidate token on disconnect
        plugin.getTokenManager().invalidatePlayer(player.getUniqueId());
        plugin.debug("Player " + player.getName() + " disconnected, token invalidated");
    }
}
