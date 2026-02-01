package net.macecontrol.utils;

import net.macecontrol.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageUtils {

    private static Main plugin;

    public static void init(Main pluginInstance) {
        plugin = pluginInstance;
    }

    public static void sendMessage(CommandSender sender, String message) {
        if (message == null || message.isEmpty()) return;
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    public static void broadcastMessage(String message) {
        if (message == null || message.isEmpty()) return;
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    public static void sendMessages(CommandSender sender, String... messages) {
        for (String message : messages) {
            sendMessage(sender, message);
        }
    }

    public static void sendNoPermission(CommandSender sender) {
        sendConfigMessage(sender, "admin.no-permission", new HashMap<>());
    }

    public static void sendLimitReached(CommandSender sender, int maxMaces, int enchantableMaces, int currentCount) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{max_maces}", String.valueOf(maxMaces));
        placeholders.put("{enchantable_maces}", String.valueOf(enchantableMaces));
        placeholders.put("{current_count}", String.valueOf(currentCount));
        sendConfigMessages(sender, "crafting.limit-reached", placeholders);
    }

    public static void sendAnvilRestricted(CommandSender sender, int enchantableMaces) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{enchantable_maces}", String.valueOf(enchantableMaces));
        sendConfigMessage(sender, "restrictions.anvil-denied", placeholders);
    }

    public static void sendEnchantRestricted(CommandSender sender, int enchantableMaces) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{enchantable_maces}", String.valueOf(enchantableMaces));
        sendConfigMessage(sender, "restrictions.enchant-denied", placeholders);
    }

    public static void sendRenameRestricted(CommandSender sender) {
        sendConfigMessage(sender, "restrictions.rename-denied", new HashMap<>());
    }

    public static void sendMaceCrafted(CommandSender sender, int maceNumber, int maxMaces, int enchantableMaces, int currentCount, String playerName) {
        Map<String, String> placeholders = getCraftingPlaceholders(maceNumber, maxMaces, enchantableMaces, currentCount, playerName);
        sendConfigMessage(sender, "crafting.mace-crafted", placeholders);
    }

    public static void broadcastMaceCrafted(int maceNumber, int maxMaces, int enchantableMaces, int currentCount, String playerName) {
        Map<String, String> placeholders = getCraftingPlaceholders(maceNumber, maxMaces, enchantableMaces, currentCount, playerName);
        String message = getConfigMessage("crafting.mace-broadcast", placeholders);
        if (message != null && !message.isEmpty()) {
            broadcastMessage(message);
        }
    }

    public static void broadcastAllMacesCrafted() {
        String message = getConfigMessage("crafting.all-crafted-broadcast", new HashMap<>());
        if (message != null && !message.isEmpty()) {
            broadcastMessage(message);
        }
    }

    public static void sendJoinMacesAvailable(CommandSender sender, int maxMaces, int enchantableMaces, int currentCount) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{max_maces}", String.valueOf(maxMaces));
        placeholders.put("{enchantable_maces}", String.valueOf(enchantableMaces));
        placeholders.put("{current_count}", String.valueOf(currentCount));
        placeholders.put("{remaining}", String.valueOf(maxMaces - currentCount));
        sendConfigMessages(sender, "join.available", placeholders);
    }

    public static void sendJoinAllCrafted(CommandSender sender, int maxMaces, int currentCount) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{max_maces}", String.valueOf(maxMaces));
        placeholders.put("{current_count}", String.valueOf(currentCount));
        sendConfigMessages(sender, "join.all-crafted", placeholders);
    }

    public static void sendInvalidMacesRemoved(CommandSender sender, int count, int maxMaces) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{count}", String.valueOf(count));
        placeholders.put("{max_maces}", String.valueOf(maxMaces));
        sendConfigMessages(sender, "restrictions.invalid-removed", placeholders);
    }

    public static void sendHeavyCoreRestricted(CommandSender sender) {
        sendConfigMessage(sender, "restrictions.heavy-core", new HashMap<>());
    }

    public static void sendShiftClickDisabled(CommandSender sender) {
        sendConfigMessage(sender, "crafting.shift-click-blocked", new HashMap<>());
    }

    public static void sendBannedEnchantmentRemoved(CommandSender sender) {
        sendConfigMessage(sender, "enchantment-cleanup.removed", new HashMap<>());
    }

    public static void sendBannedEnchantmentRefund(CommandSender sender) {
        sendConfigMessage(sender, "enchantment-cleanup.refund", new HashMap<>());
    }

    public static void broadcastDataReset() {
        String message = getConfigMessage("admin.data-reset", new HashMap<>());
        if (message != null && !message.isEmpty()) {
            broadcastMessage(message);
        }
    }

    public static void broadcastCountAdjusted(int currentCount, int maxMaces) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{current_count}", String.valueOf(currentCount));
        placeholders.put("{max_maces}", String.valueOf(maxMaces));
        String message = getConfigMessage("admin.count-adjusted", placeholders);
        if (message != null && !message.isEmpty()) {
            broadcastMessage(message);
        }
    }

    // ===== HELPER METHODS =====

    private static Map<String, String> getCraftingPlaceholders(int maceNumber, int maxMaces, int enchantableMaces, int currentCount, String playerName) {
        boolean canEnchant = maceNumber <= enchantableMaces;
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{mace_number}", String.valueOf(maceNumber));
        placeholders.put("{max_maces}", String.valueOf(maxMaces));
        placeholders.put("{enchantable_maces}", String.valueOf(enchantableMaces));
        placeholders.put("{current_count}", String.valueOf(currentCount));
        placeholders.put("{player}", playerName);
        placeholders.put("{enchantable_status}", canEnchant ? "&e(Enchantable)" : "&7(Normal)");
        placeholders.put("{enchantable_info}", canEnchant ? "&e(This mace can be enchanted!)" : "");
        return placeholders;
    }

    private static String getConfigMessage(String path, Map<String, String> placeholders) {
        if (plugin == null) return "";
        FileConfiguration config = plugin.getConfig();
        String message = config.getString("messages." + path, "");
        return replacePlaceholders(message, placeholders);
    }

    private static void sendConfigMessage(CommandSender sender, String path, Map<String, String> placeholders) {
        String message = getConfigMessage(path, placeholders);
        if (message != null && !message.isEmpty()) {
            sendMessage(sender, message);
        }
    }

    private static void sendConfigMessages(CommandSender sender, String path, Map<String, String> placeholders) {
        if (plugin == null) return;
        FileConfiguration config = plugin.getConfig();

        if (config.isList("messages." + path)) {
            List<String> messages = config.getStringList("messages." + path);
            for (String message : messages) {
                String replaced = replacePlaceholders(message, placeholders);
                if (replaced != null && !replaced.isEmpty()) {
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
