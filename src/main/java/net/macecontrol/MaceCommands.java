package net.macecontrol;

import net.macecontrol.managers.PluginDataManager;
import net.macecontrol.utils.MaceUtils;
import net.macecontrol.utils.MessageUtils;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MaceCommands implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final PluginDataManager dataManager;

    public MaceCommands(Main plugin, PluginDataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("maceset")) {
            if (args.length == 1) {
                List<String> options = Arrays.asList("max", "enchantable");
                return filterTab(options, args[0]);
            }
        } else if (command.getName().equalsIgnoreCase("macecount")) {
            if (args.length == 1) {
                return filterTab(Collections.singletonList("set"), args[0]);
            }
        } else if (command.getName().equalsIgnoreCase("maceclean") || command.getName().equalsIgnoreCase("macereset")) {
            if (args.length == 1) {
                return filterTab(Collections.singletonList("confirm"), args[0]);
            }
        }
        return Collections.emptyList();
    }

    private List<String> filterTab(List<String> list, String input) {
        List<String> result = new ArrayList<>();
        for (String str : list) {
            if (str.toLowerCase().startsWith(input.toLowerCase())) {
                result.add(str);
            }
        }
        return result;
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
        } else if (command.getName().equalsIgnoreCase("maceset")) {
            return handleMacesetCommand(sender, args);
        }
        return false;
    }

    private boolean handleMacesetCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("macecontrol.maceset")) {
            MessageUtils.sendNoPermission(sender);
            return true;
        }

        if (args.length < 2) {
            MessageUtils.sendMessages(sender,
                "&6Mace Configuration Commands:",
                "&e/maceset max <number> &7- Set maximum craftable maces",
                "&e/maceset enchantable <number> &7- Set number of enchantable maces",
                "&7Current Settings: Max: " + plugin.getMaxMaces() + ", Enchantable: " + plugin.getEnchantableMaces()
            );
            return true;
        }

        String type = args[0].toLowerCase();
        try {
            int value = Integer.parseInt(args[1]);
            if (value < 0) {
                MessageUtils.sendMessage(sender, "&cValue must be 0 or greater.");
                return true;
            }

            if (type.equals("max")) {
                plugin.setMaxMaces(value);
                MessageUtils.sendMessage(sender, "&aMaximum maces set to &6" + value);
                plugin.getLogger().info("Max maces updated to " + value + " by " + sender.getName());
            } else if (type.equals("enchantable")) {
                if (value > plugin.getMaxMaces()) {
                    MessageUtils.sendMessage(sender, "&cEnchantable maces cannot be greater than max maces (&6" + plugin.getMaxMaces() + "&c).");
                    return true;
                }
                plugin.setEnchantableMaces(value);
                MessageUtils.sendMessage(sender, "&aEnchantable maces set to &6" + value);
                plugin.getLogger().info("Enchantable maces updated to " + value + " by " + sender.getName());
            } else {
                MessageUtils.sendMessage(sender, "&cInvalid type. Use 'max' or 'enchantable'.");
            }
        } catch (NumberFormatException e) {
            MessageUtils.sendMessage(sender, "&cInvalid number format.");
        }

        return true;
    }

    private boolean handleMacefindCommand(CommandSender sender) {
        if (!sender.hasPermission("macecontrol.macefind")) {
            MessageUtils.sendNoPermission(sender);
            return true;
        }

        int maxMaces = plugin.getMaxMaces();
        int enchantableMaces = plugin.getEnchantableMaces();

        MessageUtils.sendMessages(sender,
            "&6Scanning for maces on the server...",
            "&7This may take a moment as we scan all loaded chunks..."
        );

        Map<String, MaceInfo> playerMaceDetails = new HashMap<>();
        List<String> playersWithMaces = new ArrayList<>();
        int totalValidMaces = 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            MaceInfo maceInfo = scanPlayerForMaces(player);
            if (maceInfo.totalValidMaces > 0) {
                playersWithMaces.add(player.getName() + " §a(Online)");
                playerMaceDetails.put(player.getName(), maceInfo);
                totalValidMaces += maceInfo.totalValidMaces;
            }
        }

        for (@NotNull OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            if (offlinePlayer.isOnline()) continue;

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

        MaceInfo worldInfo = scanWorldContainers();
        totalValidMaces += worldInfo.totalValidMaces;

        String separator = "&6" + "=".repeat(50);
        MessageUtils.sendMessages(sender,
            separator,
            "&6Mace Status Report:",
            separator,
            "&eTotal valid maces found: &6" + totalValidMaces + "&e/&6" + maxMaces,
            "&eTotal maces crafted: &6" + dataManager.getTotalMacesCrafted(),
            "&eEnchantable maces: &6" + enchantableMaces
        );

        if (playersWithMaces.isEmpty() && worldInfo.totalValidMaces == 0) {
            MessageUtils.sendMessage(sender, "&cNo maces found anywhere on the server!");
        } else {
            if (!playersWithMaces.isEmpty()) {
                MessageUtils.sendMessage(sender, "&6Players with maces:");
                for (String playerInfo : playersWithMaces) {
                    MessageUtils.sendMessage(sender, "&e• " + playerInfo);

                    String playerName = playerInfo.split(" ")[0];
                    MaceInfo details = playerMaceDetails.get(playerName);
                    if (details != null) {
                        MessageUtils.sendMessage(sender, "  &7Details: " + details.getDetailsString(enchantableMaces));
                    }
                }
            }

            if (worldInfo.totalValidMaces > 0) {
                MessageUtils.sendMessages(sender,
                    "&6World containers (chests/shulkers):",
                    "&e• Found in loaded chunks: &6" + worldInfo.totalValidMaces + " maces",
                    "  &7Details: " + worldInfo.getDetailsString(enchantableMaces),
                    "  &7Note: Only loaded chunks were scanned"
                );
            }
        }

        MessageUtils.sendMessage(sender, separator);
        return true;
    }

    private boolean handleMacecleanCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("macecontrol.maceclean")) {
            MessageUtils.sendNoPermission(sender);
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("confirm")) {
            MessageUtils.sendMessage(sender, "&6Cleaning invalid maces from all online players and resetting mace data...");

            plugin.getMaceControl().cleanAllOnlinePlayers();
            dataManager.resetMaceData();

            int maxMaces = plugin.getMaxMaces();
            int enchantableMaces = plugin.getEnchantableMaces();

            MessageUtils.sendMessages(sender,
                "&aClean completed!",
                "&a• Invalid maces have been removed from all online players",
                "&a• Mace data has been reset - players can now craft maces again!",
                "&7Remember: Only " + maxMaces + " maces total, " + enchantableMaces + " can be enchanted."
            );

            MessageUtils.broadcastMessage("&6Server mace data has been reset by an admin!");
            MessageUtils.broadcastMessage("&eMace crafting is now available again. Invalid maces have been removed.");

            return true;
        } else {
            MessageUtils.sendMessages(sender,
                "&e&lMACECLEAN - Enhanced Version",
                "&7This command will:",
                "&c• Remove ALL invalid maces from online players",
                "&c• Reset mace crafting data (allows new maces to be crafted)",
                "&c• Clear the macedata.yml file",
                "",
                "&eThis is a DESTRUCTIVE operation!",
                "&cType '&e/maceclean confirm&c' to proceed."
            );
            return true;
        }
    }

    private boolean handleMaceresetCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("macecontrol.macereset")) {
            MessageUtils.sendNoPermission(sender);
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("confirm")) {
            dataManager.resetMaceData();

            int maxMaces = plugin.getMaxMaces();
            int enchantableMaces = plugin.getEnchantableMaces();

            MessageUtils.sendMessages(sender,
                "&aAll mace data has been reset! Players can now craft maces again.",
                "&7Remember: Only " + maxMaces + " maces total, " + enchantableMaces + " can be enchanted."
            );
            MessageUtils.broadcastMessage("&6Server mace data has been reset by an admin! Mace crafting is now available again.");
        } else {
            MessageUtils.sendMessages(sender,
                "&cThis will reset ALL mace data and allow new maces to be crafted!",
                "&cType '&e/macereset confirm&c' to proceed."
            );
        }
        return true;
    }

    private boolean handleMacecountCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("macecontrol.macecount")) {
            MessageUtils.sendNoPermission(sender);
            return true;
        }

        int maxMaces = plugin.getMaxMaces();

        if (args.length == 0) {
            MessageUtils.sendMessage(sender, "&6Current mace count: &e" + dataManager.getTotalMacesCrafted() + "/" + maxMaces);
            return true;
        }

        if (args[0].equalsIgnoreCase("set") && args.length > 1) {
            try {
                int newCount = Integer.parseInt(args[1]);
                if (newCount >= 0 && newCount <= maxMaces) {
                    dataManager.setTotalMacesCrafted(newCount);
                    MessageUtils.sendMessage(sender, "&aMace count set to: &6" + newCount + "/" + maxMaces);
                    MessageUtils.broadcastMessage("&6Server mace count has been adjusted by an admin to " + newCount + "/" + maxMaces);
                } else {
                    MessageUtils.sendMessage(sender, "&cInvalid count! Must be between 0 and " + maxMaces + ".");
                }
            } catch (NumberFormatException e) {
                MessageUtils.sendMessage(sender, "&cInvalid number! Usage: /macecount set <0-" + maxMaces + ">");
            }
        } else {
            MessageUtils.sendMessage(sender, "&cUsage: /macecount [set <0-" + maxMaces + ">]");
        }

        return true;
    }

    private MaceInfo scanPlayerForMaces(Player player) {
        MaceInfo info = new MaceInfo();
        scanInventoryForMaces(player.getInventory(), info);
        scanInventoryForMaces(player.getEnderChest(), info);
        scanShulkerBoxesInInventory(player.getInventory(), info);
        return info;
    }

    private MaceInfo scanEnderchest(org.bukkit.OfflinePlayer offlinePlayer) {
        MaceInfo info = new MaceInfo();
        if (offlinePlayer.isOnline()) {
            Player onlinePlayer = offlinePlayer.getPlayer();
            if (onlinePlayer != null) {
                scanInventoryForMaces(onlinePlayer.getEnderChest(), info);
            }
        }
        return info;
    }

    private MaceInfo scanWorldContainers() {
        MaceInfo info = new MaceInfo();

        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (BlockState blockState : chunk.getTileEntities()) {
                    if (blockState instanceof Chest) {
                        scanInventoryForMaces(((Chest) blockState).getInventory(), info);
                    } else if (blockState instanceof ShulkerBox) {
                        scanInventoryForMaces(((ShulkerBox) blockState).getInventory(), info);
                    } else if (blockState instanceof org.bukkit.block.Barrel) {
                        scanInventoryForMaces(((org.bukkit.block.Barrel) blockState).getInventory(), info);
                    }
                }
            }
        }

        return info;
    }

    private void scanInventoryForMaces(Inventory inventory, MaceInfo info) {
        int maxMaces = plugin.getMaxMaces();
        int enchantableMaces = plugin.getEnchantableMaces();

        for (ItemStack item : inventory.getContents()) {
            if (MaceUtils.isMace(item)) {
                Integer maceNumber = MaceUtils.getMaceNumber(item);
                if (maceNumber != null && maceNumber >= 1 && maceNumber <= maxMaces) {
                    info.totalValidMaces++;
                    info.maceNumbers.add(maceNumber);

                    if (maceNumber <= enchantableMaces && !item.getEnchantments().isEmpty()) {
                        info.enchantedMaces.add(maceNumber);
                    }
                } else {
                    info.invalidMaces++;
                }
            }
        }
    }

    private void scanShulkerBoxesInInventory(Inventory inventory, MaceInfo info) {
        for (ItemStack item : inventory.getContents()) {
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
        Set<Integer> maceNumbers = new HashSet<>();
        Set<Integer> enchantedMaces = new HashSet<>();

        String getDetailsString(int enchantableMaces) {
            StringBuilder sb = new StringBuilder();
            sb.append(totalValidMaces).append(" valid maces");

            if (!maceNumbers.isEmpty()) {
                sb.append(" (");
                List<Integer> sorted = new ArrayList<>(maceNumbers);
                Collections.sort(sorted);
                List<String> maceList = new ArrayList<>();

                for (int num : sorted) {
                    String maceStr = "#" + num;
                    if (enchantedMaces.contains(num)) {
                        maceStr += " enchanted";
                    } else if (num <= enchantableMaces) {
                        maceStr += " enchantable";
                    }
                    maceList.add(maceStr);
                }

                sb.append(String.join(", ", maceList)).append(")");
            }

            if (invalidMaces > 0) {
                sb.append(", ").append(invalidMaces).append(" invalid");
            }

            return sb.toString();
        }
    }
}