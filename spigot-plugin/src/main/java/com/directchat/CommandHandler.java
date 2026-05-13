package com.directchat;

import com.directchat.tunnel.CloudflaredTunnelManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles server-side /directchat commands.
 */
public class CommandHandler implements CommandExecutor, TabCompleter {

    private final DirectChatPlugin plugin;

    public CommandHandler(DirectChatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Handle /chat and /c directly
        if (label.equalsIgnoreCase("chat") || label.equalsIgnoreCase("c")) {
            if (!(sender instanceof org.bukkit.entity.Player)) {
                sender.sendMessage("\u00a7cOnly players can use this command.");
                return true;
            }
            if (args.length == 0) {
                sender.sendMessage("\u00a7cUsage: /" + label + " <message>");
                return true;
            }
            String message = String.join(" ", args);
            plugin.getChatManager().broadcastMessage((org.bukkit.entity.Player) sender, message);
            return true;
        }

        // Handle /directchat and /dchat
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "chat":
                if (!(sender instanceof org.bukkit.entity.Player)) {
                    sender.sendMessage("\u00a7cOnly players can use this command.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("\u00a7cUsage: /" + label + " chat <message>");
                    return true;
                }
                String[] messageArgs = new String[args.length - 1];
                System.arraycopy(args, 1, messageArgs, 0, args.length - 1);
                String message = String.join(" ", messageArgs);
                plugin.getChatManager().broadcastMessage((org.bukkit.entity.Player) sender, message);
                return true;

            case "reload":
                if (!sender.hasPermission("directchat.admin")) {
                    sender.sendMessage("\u00a7cYou don't have permission to do this.");
                    return true;
                }
                plugin.loadConfiguration();
                sender.sendMessage("\u00a7aDirectChat configuration reloaded!");
                if (plugin.isCloudflaredTunnelMode()) {
                    sender.sendMessage("\u00a76Note: Cloudflared tunnel changes require a server restart.");
                }
                return true;

            case "stats":
                if (!sender.hasPermission("directchat.admin")) {
                    sender.sendMessage("\u00a7cYou don't have permission to do this.");
                    return true;
                }
                sendStats(sender);
                return true;

            case "tunnel":
                if (!sender.hasPermission("directchat.admin")) {
                    sender.sendMessage("\u00a7cYou don't have permission to do this.");
                    return true;
                }
                sendTunnelInfo(sender);
                return true;

            default:
                sendHelp(sender);
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("\u00a76=== DirectChat Commands ===");
        sender.sendMessage("\u00a7e/directchat chat <msg> \u00a77- Send message to DirectChat");
        if (sender.hasPermission("directchat.admin")) {
            sender.sendMessage("\u00a7e/directchat reload \u00a77- Reload configuration");
            sender.sendMessage("\u00a7e/directchat stats \u00a77- Show server statistics");
            sender.sendMessage("\u00a7e/directchat tunnel \u00a77- Show tunnel status & URL");
        }
    }

    private void sendStats(CommandSender sender) {
        sender.sendMessage("\u00a76=== DirectChat Statistics ===");
        sender.sendMessage("\u00a77Authenticated Players: \u00a7f" + plugin.getTokenManager().getAuthenticatedCount());
        sender.sendMessage("\u00a77Message History: \u00a7f" + plugin.getChatManager().getHistorySize() + " messages");
        sender.sendMessage("\u00a77API Port: \u00a7f" + plugin.getPort());
        sender.sendMessage("\u00a77Secure (HTTPS): \u00a7f" + (plugin.isRequireHttps() ? "\u00a7aYes" : "\u00a7cNo"));
        sender.sendMessage("\u00a77Cloudflared Tunnel: \u00a7f" + (plugin.isCloudflaredTunnelMode() ? "\u00a7aEnabled" : "\u00a7cDisabled"));
    }

    private void sendTunnelInfo(CommandSender sender) {
        CloudflaredTunnelManager tm = plugin.getTunnelManager();
        sender.sendMessage("\u00a76=== Cloudflared Tunnel ===");
        sender.sendMessage("\u00a77Tunnel Mode: \u00a7f" + (plugin.isCloudflaredTunnelMode() ? "\u00a7aEnabled" : "\u00a7cDisabled"));

        if (tm.isRunning() && tm.getTunnelUrl() != null) {
            sender.sendMessage("\u00a77Status: \u00a7aRunning");
            sender.sendMessage("\u00a77Public URL: \u00a7a" + tm.getTunnelUrl());
            sender.sendMessage("\u00a77Local Port: \u00a7f" + plugin.getPort());
            sender.sendMessage("\u00a77Connect Command: \u00a7e/directchat connect " + tm.getTunnelUrl() + " <password>");
        } else if (plugin.isCloudflaredTunnelMode()) {
            sender.sendMessage("\u00a77Status: \u00a7cNot running");
            sender.sendMessage("\u00a7cTunnel mode is enabled but no tunnel is active.");
            sender.sendMessage("\u00a7cCheck console for errors or restart the server.");
        } else {
            sender.sendMessage("\u00a77Status: \u00a77Disabled");
            sender.sendMessage("\u00a77To enable, set \u00a7fcloudflared-tunnel-mode: true\u00a77 in config.yml");
        }

        if (tm.isCloudflaredAvailable()) {
            sender.sendMessage("\u00a77cloudflared binary: \u00a7aFound");
        } else {
            sender.sendMessage("\u00a77cloudflared binary: \u00a7cNot found");
            sender.sendMessage("\u00a7cInstall from: \u00a7fhttps://github.com/cloudflare/cloudflared/releases");
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("chat");
            if (sender.hasPermission("directchat.admin")) {
                options.add("reload");
                options.add("stats");
                options.add("tunnel");
            }
            return options.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
