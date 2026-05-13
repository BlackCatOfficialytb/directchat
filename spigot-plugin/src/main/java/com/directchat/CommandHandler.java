package com.directchat;

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
                sender.sendMessage("§cOnly players can use this command.");
                return true;
            }
            if (args.length == 0) {
                sender.sendMessage("§cUsage: /" + label + " <message>");
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
                    sender.sendMessage("§cOnly players can use this command.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /" + label + " chat <message>");
                    return true;
                }
                String[] messageArgs = new String[args.length - 1];
                System.arraycopy(args, 1, messageArgs, 0, args.length - 1);
                String message = String.join(" ", messageArgs);
                plugin.getChatManager().broadcastMessage((org.bukkit.entity.Player) sender, message);
                return true;

            case "reload":
                if (!sender.hasPermission("directchat.admin")) {
                    sender.sendMessage("§cYou don't have permission to do this.");
                    return true;
                }
                plugin.loadConfiguration();
                sender.sendMessage("§aDirectChat configuration reloaded!");
                return true;

            case "stats":
                if (!sender.hasPermission("directchat.admin")) {
                    sender.sendMessage("§cYou don't have permission to do this.");
                    return true;
                }
                sendStats(sender);
                return true;

            default:
                sendHelp(sender);
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== DirectChat Commands ===");
        sender.sendMessage("§e/directchat chat <msg> §7- Send message to DirectChat");
        if (sender.hasPermission("directchat.admin")) {
            sender.sendMessage("§e/directchat reload §7- Reload configuration");
            sender.sendMessage("§e/directchat stats §7- Show server statistics");
        }
    }

    private void sendStats(CommandSender sender) {
        sender.sendMessage("§6=== DirectChat Statistics ===");
        sender.sendMessage("§7Authenticated Players: §f" + plugin.getTokenManager().getAuthenticatedCount());
        sender.sendMessage("§7Message History: §f" + plugin.getChatManager().getHistorySize() + " messages");
        sender.sendMessage("§7API Port: §f" + plugin.getPort());
        sender.sendMessage("§7Secure (HTTPS): §f" + (plugin.isRequireHttps() ? "§aYes" : "§cNo"));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("chat");
            if (sender.hasPermission("directchat.admin")) {
                options.add("reload");
                options.add("stats");
            }
            return options.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
