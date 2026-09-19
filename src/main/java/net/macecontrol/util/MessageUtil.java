package net.macecontrol.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * All player-facing text goes through here. Gameplay messages/broadcasts are
 * config-driven and gated behind "messages-enabled" / "broadcasts-enabled" so
 * server owners can run fully silent (the default) or fully verbose.
 */
public final class MessageUtil {

    private static JavaPlugin plugin;

    private MessageUtil() {
    }

    public static void init(JavaPlugin pluginInstance) {
        plugin = pluginInstance;
    }

    // ===== Direct sends (always shown - command feedback, admin errors, etc.) =====

    public static void sendMessage(CommandSender sender, String message) {
        if (message == null || message.isEmpty()) return;
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    public static void sendMessages(CommandSender sender, String... messages) {
        for (String message : messages) {
            sendMessage(sender, message);
        }
    }

    public static void sendNoPermission(CommandSender sender) {
        sendMessage(sender, "&cYou do not have permission to use this command.");
    }

    public static void broadcastMessage(String message) {
        if (message == null || message.isEmpty()) return;
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    // ===== Config-driven gameplay messages (silent unless enabled) =====

    public static void sendLimitReached(CommandSender sender, int maxMaces, int enchantableMaces, int currentCount) {
        if (!messagesEnabled()) return;
        sendConfigMessages(sender, "crafting.limit-reached", placeholders(
                "{max_maces}", maxMaces, "{enchantable_maces}", enchantableMaces, "{current_count}", currentCount));
    }

    public static void sendAnvilDenied(CommandSender sender, int enchantableMaces) {
        if (!messagesEnabled()) return;
        sendConfigMessage(sender, "restrictions.anvil-denied", placeholders("{enchantable_maces}", enchantableMaces));
    }

    public static void sendEnchantDenied(CommandSender sender, int enchantableMaces) {
        if (!messagesEnabled()) return;
        sendConfigMessage(sender, "restrictions.enchant-denied", placeholders("{enchantable_maces}", enchantableMaces));
    }

    public static void sendHeavyCoreRestricted(CommandSender sender) {
        if (!messagesEnabled()) return;
        sendConfigMessage(sender, "restrictions.heavy-core", Map.of());
    }

    public static void sendBannedEnchantmentRemoved(CommandSender sender) {
        if (!messagesEnabled()) return;
        sendConfigMessage(sender, "enchantment-cleanup.removed", Map.of());
    }

    public static void sendBannedEnchantmentRefund(CommandSender sender) {
        if (!messagesEnabled()) return;
        sendConfigMessage(sender, "enchantment-cleanup.refund", Map.of());
    }

    public static void broadcastDataReset() {
        if (!broadcastsEnabled()) return;
        broadcastMessage(getConfigMessage("admin.data-reset", Map.of()));
    }

    public static void broadcastCountAdjusted(int currentCount, int maxMaces) {
        if (!broadcastsEnabled()) return;
        broadcastMessage(getConfigMessage("admin.count-adjusted",
                placeholders("{current_count}", currentCount, "{max_maces}", maxMaces)));
    }

    // ===== Helpers =====

    private static boolean messagesEnabled() {
        return plugin != null && plugin.getConfig().getBoolean("messages-enabled", false);
    }

    private static boolean broadcastsEnabled() {
        return plugin != null && plugin.getConfig().getBoolean("broadcasts-enabled", false);
    }

    private static Map<String, String> placeholders(Object... keyValuePairs) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            map.put((String) keyValuePairs[i], String.valueOf(keyValuePairs[i + 1]));
        }
        return map;
    }

    private static String getConfigMessage(String path, Map<String, String> placeholders) {
        if (plugin == null) return "";
        FileConfiguration config = plugin.getConfig();
        String message = config.getString("messages." + path, "");
        return replacePlaceholders(message, placeholders);
    }

    private static void sendConfigMessage(CommandSender sender, String path, Map<String, String> placeholders) {
        String message = getConfigMessage(path, placeholders);
        if (!message.isEmpty()) {
            sendMessage(sender, message);
        }
    }

    private static void sendConfigMessages(CommandSender sender, String path, Map<String, String> placeholders) {
        if (plugin == null) return;
        FileConfiguration config = plugin.getConfig();

        if (config.isList("messages." + path)) {
            for (String message : config.getStringList("messages." + path)) {
                String replaced = replacePlaceholders(message, placeholders);
                if (!replaced.isEmpty()) {
                    sendMessage(sender, replaced);
                }
            }
        } else {
            sendConfigMessage(sender, path, placeholders);
        }
    }

    private static String replacePlaceholders(String message, Map<String, String> placeholders) {
        if (message == null) return "";
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace(entry.getKey(), entry.getValue());
        }
        return message;
    }
}
