package net.macecontrol;

import net.macecontrol.managers.PluginDataManager;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

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
            return handleMacecleanCommand(sender, args);
        } else if (command.getName().equalsIgnoreCase("macereset")) {
            return handleMaceresetCommand(sender, args);
        } else if (command.getName().equalsIgnoreCase("macecount")) {
            return handleMacecountCommand(sender, args);
        }
        return false;
    }

    private boolean handleMacefindCommand(CommandSender sender) {
        if (!sender.hasPermission("stupidmacecontrol.macefind")) {
            sender.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        sender.sendMessage("§6Scanning for maces on the server...");
        sender.sendMessage("§7This may take a moment as we scan all loaded chunks...");

        Map<String, MaceInfo> playerMaceDetails = new HashMap<>();
        List<String> playersWithMaces = new ArrayList<>();
        MaceInfo worldMaces = new MaceInfo(); // For chests in the world
        int totalValidMaces = 0;

        // Check online players (inventory + enderchest + shulker boxes)
        for (Player player : Bukkit.getOnlinePlayers()) {
            MaceInfo maceInfo = scanPlayerForMaces(player);
            if (maceInfo.totalValidMaces > 0) {
                playersWithMaces.add(player.getName() + " §a(Online)");
                playerMaceDetails.put(player.getName(), maceInfo);
                totalValidMaces += maceInfo.totalValidMaces;
            }
        }

        // Check offline players' enderchests (only if they've been online recently)
        for (@NotNull OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            if (offlinePlayer.isOnline()) continue; // Already checked above

            // Only check players who have played recently (within last 30 days)
            long lastPlayed = offlinePlayer.getLastPlayed();
            long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24L * 60L * 60L * 1000L);

            if (lastPlayed > thirtyDaysAgo) {
                MaceInfo enderchestInfo = scanEnderchest(offlinePlayer);
                if (enderchestInfo.totalValidMaces > 0) {
                    playersWithMaces.add(offlinePlayer.getName() + " §7(Offline - Enderchest only)");
                    playerMaceDetails.put(offlinePlayer.getName(), enderchestInfo);
                    totalValidMaces += enderchestInfo.totalValidMaces;
                }
            }
        }

        // Check chests and shulker boxes in loaded chunks
        MaceInfo worldInfo = scanWorldContainers();
        totalValidMaces += worldInfo.totalValidMaces;

        // Display results
        sender.sendMessage("§6" + "=".repeat(50));
        sender.sendMessage("§6Mace Status Report:");
        sender.sendMessage("§6" + "=".repeat(50));
        sender.sendMessage("§eTotal valid maces found: §6" + totalValidMaces + "§e/§63");
        sender.sendMessage("§eTotal maces crafted: §6" + dataManager.getTotalMacesCrafted());

        if (playersWithMaces.isEmpty() && worldInfo.totalValidMaces == 0) {
            sender.sendMessage("§cNo maces found anywhere on the server!");
        } else {
            if (!playersWithMaces.isEmpty()) {
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

            if (worldInfo.totalValidMaces > 0) {
                sender.sendMessage("§6World containers (chests/shulkers):");
                sender.sendMessage("§e• Found in loaded chunks: §6" + worldInfo.totalValidMaces + " maces");
                sender.sendMessage("  §7Details: " + worldInfo.getDetailsString());
                sender.sendMessage("  §7Note: Only loaded chunks were scanned");
            }
        }

        sender.sendMessage("§6" + "=".repeat(50));
        return true;
    }

    private boolean handleMacecleanCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("stupidmacecontrol.maceclean")) {
            sender.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("confirm")) {
            sender.sendMessage("§6Cleaning invalid maces from all online players and resetting mace data...");

            // Clean all online players
            plugin.getMaceControl().cleanAllOnlinePlayers();

            // Reset mace data to make maces craftable again
            dataManager.resetMaceData();

            sender.sendMessage("§aClean completed!");
            sender.sendMessage("§a• Invalid maces have been removed from all online players");
            sender.sendMessage("§a• Mace data has been reset - players can now craft maces again!");
            sender.sendMessage("§7Remember: Only 3 maces total, #1 can be enchanted.");

            // Broadcast the reset to all players
            Bukkit.broadcastMessage("§6Server mace data has been reset by an admin!");
            Bukkit.broadcastMessage("§eMace crafting is now available again. Invalid maces have been removed.");

            return true;
        } else {
            sender.sendMessage("§e§lMACECLEAN - Enhanced Version");
            sender.sendMessage("§7This command will:");
            sender.sendMessage("§c• Remove ALL invalid maces from online players");
            sender.sendMessage("§c• Reset mace crafting data (allows new maces to be crafted)");
            sender.sendMessage("§c• Clear the macedata.yml file");
            sender.sendMessage("");
            sender.sendMessage("§eThis is a DESTRUCTIVE operation!");
            sender.sendMessage("§cType '§e/maceclean confirm§c' to proceed.");
            return true;
        }
    }

    private boolean handleMaceresetCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("stupidmacecontrol.macereset")) {
            sender.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("confirm")) {
            dataManager.resetMaceData();
            sender.sendMessage("§aAll mace data has been reset! Players can now craft maces again.");
            sender.sendMessage("§7Remember: Only 3 maces total, #1 can be enchanted.");
            Bukkit.broadcastMessage("§6Server mace data has been reset by an admin! Mace crafting is now available again.");
        } else {
            sender.sendMessage("§cThis will reset ALL mace data and allow new maces to be crafted!");
            sender.sendMessage("§cType '§e/macereset confirm§c' to proceed.");
        }
        return true;
    }

    private boolean handleMacecountCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("stupidmacecontrol.macecount")) {
            sender.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        if (args.length == 0) {
            // Show current count
            sender.sendMessage("§6Current mace count: §e" + dataManager.getTotalMacesCrafted() + "/3");
            return true;
        }

        if (args[0].equalsIgnoreCase("set") && args.length > 1) {
            try {
                int newCount = Integer.parseInt(args[1]);
                if (newCount >= 0 && newCount <= 3) { // Validate range here instead
                    dataManager.setTotalMacesCrafted(newCount);
                    sender.sendMessage("§aMace count set to: §6" + newCount + "/3");
                    Bukkit.broadcastMessage("§6Server mace count has been adjusted by an admin to " + newCount + "/3");
                } else {
                    sender.sendMessage("§cInvalid count! Must be between 0 and 3.");
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§cInvalid number! Usage: /macecount set <0-3>");
            }
        } else {
            sender.sendMessage("§cUsage: /macecount [set <0-3>]");
        }

        return true;
    }

    private MaceInfo scanPlayerForMaces(Player player) {
        MaceInfo info = new MaceInfo();

        // Scan regular inventory
        scanInventoryForMaces(player.getInventory(), info);

        // Scan enderchest
        scanInventoryForMaces(player.getEnderChest(), info);

        // Scan shulker boxes in inventory
        scanShulkerBoxesInInventory(player.getInventory(), info);

        return info;
    }

    private MaceInfo scanEnderchest(org.bukkit.OfflinePlayer offlinePlayer) {
        MaceInfo info = new MaceInfo();

        if (offlinePlayer.isOnline()) {
            // If they're online, use the online player method
            Player onlinePlayer = offlinePlayer.getPlayer();
            if (onlinePlayer != null) {
                scanInventoryForMaces(onlinePlayer.getEnderChest(), info);
            }
        }
        // Note: For truly offline players, we can't access their enderchest
        // without loading their player data, which is more complex

        return info;
    }

    private MaceInfo scanWorldContainers() {
        MaceInfo info = new MaceInfo();

        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (BlockState blockState : chunk.getTileEntities()) {
                    if (blockState instanceof Chest) {
                        Chest chest = (Chest) blockState;
                        scanInventoryForMaces(chest.getInventory(), info);
                    } else if (blockState instanceof ShulkerBox) {
                        ShulkerBox shulkerBox = (ShulkerBox) blockState;
                        scanInventoryForMaces(shulkerBox.getInventory(), info);
                    }
                    // Add other container types if needed (barrel, hopper, etc.)
                    else if (blockState instanceof org.bukkit.block.Barrel) {
                        org.bukkit.block.Barrel barrel = (org.bukkit.block.Barrel) blockState;
                        scanInventoryForMaces(barrel.getInventory(), info);
                    }
                }
            }
        }

        return info;
    }

    private void scanInventoryForMaces(Inventory inventory, MaceInfo info) {
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() == Material.MACE) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    Integer maceNumber = meta.getPersistentDataContainer().get(maceNumberKey, PersistentDataType.INTEGER);
                    if (maceNumber != null && maceNumber >= 1 && maceNumber <= 3) {
                        info.totalValidMaces++;
                        switch (maceNumber) {
                            case 1:
                                info.hasMace1 = true;
                                info.mace1Enchanted = !meta.getEnchants().isEmpty();
                                break;
                            case 2:
                                info.hasMace2 = true;
                                break;
                            case 3:
                                info.hasMace3 = true;
                                break;
                        }
                    } else {
                        info.invalidMaces++;
                    }
                } else {
                    info.invalidMaces++;
                }
            }
        }
    }

    private void scanShulkerBoxesInInventory(PlayerInventory inventory, MaceInfo info) {
        // Check all inventory slots for shulker boxes
        List<ItemStack> allItems = new ArrayList<>();
        Collections.addAll(allItems, inventory.getContents());
        allItems.add(inventory.getHelmet());
        allItems.add(inventory.getChestplate());
        allItems.add(inventory.getLeggings());
        allItems.add(inventory.getBoots());
        allItems.add(inventory.getItemInOffHand());

        for (ItemStack item : allItems) {
            if (item != null && isShulkerBox(item.getType())) {
                ItemMeta meta = item.getItemMeta();
                if (meta instanceof BlockStateMeta) {
                    BlockStateMeta blockStateMeta = (BlockStateMeta) meta;
                    BlockState blockState = blockStateMeta.getBlockState();
                    if (blockState instanceof ShulkerBox) {
                        ShulkerBox shulkerBox = (ShulkerBox) blockState;
                        scanInventoryForMaces(shulkerBox.getInventory(), info);
                    }
                }
            }
        }
    }

    private boolean isShulkerBox(Material material) {
        return material == Material.SHULKER_BOX ||
                material == Material.WHITE_SHULKER_BOX ||
                material == Material.ORANGE_SHULKER_BOX ||
                material == Material.MAGENTA_SHULKER_BOX ||
                material == Material.LIGHT_BLUE_SHULKER_BOX ||
                material == Material.YELLOW_SHULKER_BOX ||
                material == Material.LIME_SHULKER_BOX ||
                material == Material.PINK_SHULKER_BOX ||
                material == Material.GRAY_SHULKER_BOX ||
                material == Material.LIGHT_GRAY_SHULKER_BOX ||
                material == Material.CYAN_SHULKER_BOX ||
                material == Material.PURPLE_SHULKER_BOX ||
                material == Material.BLUE_SHULKER_BOX ||
                material == Material.BROWN_SHULKER_BOX ||
                material == Material.GREEN_SHULKER_BOX ||
                material == Material.RED_SHULKER_BOX ||
                material == Material.BLACK_SHULKER_BOX;
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