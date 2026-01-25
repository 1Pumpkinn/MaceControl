package net.macecontrol.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import java.util.List;

public class MessageUtils {

    /**
     * Translates alternate color codes (using &) and sends a message to the sender.
     */
    public static void sendMessage(CommandSender sender, String message) {
        if (message == null || message.isEmpty()) return;
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    /**
     * Broadcasts a color-coded message to all players and the console.
     * @param message The message to broadcast
     */
    public static void broadcastMessage(String message) {
        if (message == null || message.isEmpty()) return;
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    /**
     * Sends multiple messages to the sender, translating color codes.
     */
    public static void sendMessages(CommandSender sender, String... messages) {
        for (String message : messages) {
            sendMessage(sender, message);
        }
    }

    /**
     * Sends a list of messages to the sender, translating color codes.
     */
    public static void sendMessages(CommandSender sender, List<String> messages) {
        for (String message : messages) {
            sendMessage(sender, message);
        }
    }

    /**
     * Sends a permission error message.
     */
    public static void sendNoPermission(CommandSender sender) {
        sendMessage(sender, "&cYou don't have permission to use this command!");
    }

    /**
     * Sends the mace limit reached message.
     */
    public static void sendLimitReached(CommandSender sender, int maxMaces, int enchantableMaces, int currentCount) {
        sendMessages(sender,
            "&cAll " + maxMaces + " maces have already been crafted on this server!",
            "&7" + enchantableMaces + " can be enchanted, " + (maxMaces - enchantableMaces) + " are normal.",
            "&7Current maces crafted: " + currentCount + "/" + maxMaces
        );
    }

    /**
     * Sends the anvil restricted message.
     */
    public static void sendAnvilRestricted(CommandSender sender, int enchantableMaces) {
        if (enchantableMaces == 0) {
            sendMessage(sender, "&cNo maces can be used in anvils on this server!");
        } else if (enchantableMaces == 1) {
            sendMessage(sender, "&cOnly Mace #1 can be used in anvils!");
        } else {
            sendMessage(sender, "&cOnly Maces #1-" + enchantableMaces + " can be used in anvils!");
        }
    }

    /**
     * Sends the enchant restricted message.
     */
    public static void sendEnchantRestricted(CommandSender sender, int enchantableMaces) {
        if (enchantableMaces == 0) {
            sendMessage(sender, "&cNo maces can be enchanted on this server!");
        } else if (enchantableMaces == 1) {
            sendMessage(sender, "&cOnly Mace #1 can be enchanted!");
        } else {
            sendMessage(sender, "&cOnly Maces #1-" + enchantableMaces + " can be enchanted!");
        }
    }

    /**
     * Sends the rename restricted message.
     */
    public static void sendRenameRestricted(CommandSender sender) {
        sendMessage(sender, "&cMaces cannot be renamed on this server!");
    }
}
