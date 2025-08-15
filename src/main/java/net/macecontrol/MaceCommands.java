package net.macecontrol;

import net.macecontrol.Main;
import net.macecontrol.PluginDataManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class MaceCommands implements CommandExecutor {

    private final Main plugin;
    private final PluginDataManager dataManager;
    private final NamespacedKey maceNumberKey;

    public MaceCommands(Main plugin, PluginDataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.maceNumberKey = new NamespacedKey(plugin, "mace_number");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("macefind")) {
            return handleMacefindCommand(sender);
        } else if (command.getName().equalsIgnoreCase("maceclean")) {
            return handleMacecleanCommand(sender);
        }
        return false;
    }

    private boolean handleMacefindCommand(CommandSender sender) {
        if (!sender.hasPermission("stupidmacecontrol.macefind")) {
            sender.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        sender.sendMessage("§6Scanning for maces on the server...");

        List<String> playersWithMaces = new ArrayList<>();
        Map<String, MaceInfo> playerMaceDetails = new HashMap<>();
        int totalValidMaces = 0;

        // Check online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            MaceInfo maceInfo = scanPlayerForMaces(player);
            if (maceInfo.totalValidMaces > 0) {
                playersWithMaces.add(player.getName() + " §a(Online)");
                playerMaceDetails.put(player.getName(), maceInfo);
                totalValidMaces += maceInfo.totalValidMaces;
            }
        }

        sender.sendMessage("§6Mace Status Report:");
        sender.sendMessage("§6" + "=".repeat(40));
        sender.sendMessage("§eTotal valid maces on server: §6" + totalValidMaces + "§e/§63");
        sender.sendMessage("§eTotal maces crafted: §6" + dataManager.getTotalMacesCrafted());

        if (playersWithMaces.isEmpty()) {
            sender.sendMessage("§cNo players currently have valid maces!");
        } else {
            sender.sendMessage("§6Players with maces:");

            for (String playerInfo : playersWithMaces) {
                sender.sendMessage("§e• " + playerInfo);

                String playerName = playerInfo.split(" ")[0];
                MaceInfo details = playerMaceDetails.get(playerName);
                if (details != null) {
                    sender.sendMessage("  §7Details: " + details.getDetailsString());
                }
            }
        }

        sender.sendMessage("§6" + "=".repeat(40));
        return true;
    }

    private boolean handleMacecleanCommand(CommandSender sender) {
        if (!sender.hasPermission("stupidmacecontrol.maceclean")) {
            sender.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        sender.sendMessage("§6Cleaning invalid maces from all online players...");
        plugin.getMaceControl().cleanAllOnlinePlayers();
        sender.sendMessage("§aClean completed! Invalid maces have been removed from all online players.");

        return true;
    }

    private MaceInfo scanPlayerForMaces(Player player) {
        PlayerInventory inventory = player.getInventory();
        MaceInfo info = new MaceInfo();

        List<ItemStack> allItems = new ArrayList<>();
        Collections.addAll(allItems, inventory.getContents());
        allItems.add(inventory.getHelmet());
        allItems.add(inventory.getChestplate());
        allItems.add(inventory.getLeggings());
        allItems.add(inventory.getBoots());
        allItems.add(inventory.getItemInOffHand());

        for (ItemStack item : allItems) {
            if (item != null && item.getType() == Material.MACE) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    Integer maceNumber = meta.getPersistentDataContainer().get(maceNumberKey, PersistentDataType.INTEGER);
                    if (maceNumber != null && maceNumber >= 1 && maceNumber <= 3) {
                        info.totalValidMaces++;
                        switch (maceNumber) {
                            case 1 -> {
                                info.hasMace1 = true;
                                info.mace1Enchanted = !meta.getEnchants().isEmpty();
                            }
                            case 2 -> info.hasMace2 = true;
                            case 3 -> info.hasMace3 = true;
                        }
                    } else {
                        info.invalidMaces++;
                    }
                } else {
                    info.invalidMaces++;
                }
            }
        }
        return info;
    }

    private static class MaceInfo {
        int totalValidMaces = 0;
        int invalidMaces = 0;
        boolean hasMace1 = false;
        boolean hasMace2 = false;
        boolean hasMace3 = false;
        boolean mace1Enchanted = false;

        String getDetailsString() {
            StringBuilder sb = new StringBuilder();
            sb.append(totalValidMaces).append(" valid maces");

            if (hasMace1 || hasMace2 || hasMace3) {
                sb.append(" (");
                List<String> maces = new ArrayList<>();
                if (hasMace1) maces.add("#1" + (mace1Enchanted ? " enchanted" : ""));
                if (hasMace2) maces.add("#2");
                if (hasMace3) maces.add("#3");
                sb.append(String.join(", ", maces)).append(")");
            }

            if (invalidMaces > 0) {
                sb.append(", ").append(invalidMaces).append(" invalid");
            }

            return sb.toString();
        }
    }
}