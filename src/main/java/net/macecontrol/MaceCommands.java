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
        if (!sender.hasPermission("stupidmacecontrol.macefind")) {
            sender.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        sender.sendMessage("§6Scanning for players with 3 maces...");

        List<String> playersWithThreeMaces = new ArrayList<>();
        Map<String, MaceInfo> playerMaceDetails = new HashMap<>();

        // Check online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            MaceInfo maceInfo = scanPlayerForMaces(player);
            if (maceInfo.totalMaces >= 3 || maceInfo.hasMace3) {
                playersWithThreeMaces.add(player.getName() + " §a(Online)");
                playerMaceDetails.put(player.getName(), maceInfo);
            }
        }

        // Also check from data manager
        Map<UUID, Integer> playerMaceCount = dataManager.getPlayerMaceCount();
        for (Map.Entry<UUID, Integer> entry : playerMaceCount.entrySet()) {
            if (entry.getValue() >= 3) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(entry.getKey());
                String playerName = offlinePlayer.getName();
                if (playerName != null) {
                    boolean alreadyAdded = playersWithThreeMaces.stream()
                            .anyMatch(name -> name.startsWith(playerName));
                    if (!alreadyAdded) {
                        playersWithThreeMaces.add(playerName + " §7(Offline - " + entry.getValue() + " maces crafted)");
                    }
                }
            }
        }

        if (playersWithThreeMaces.isEmpty()) {
            sender.sendMessage("§aNo players found with 3 maces!");
        } else {
            sender.sendMessage("§6Players with 3 maces found:");
            sender.sendMessage("§6" + "=".repeat(40));

            for (String playerInfo : playersWithThreeMaces) {
                sender.sendMessage("§e• " + playerInfo);

                String playerName = playerInfo.split(" ")[0];
                MaceInfo details = playerMaceDetails.get(playerName);
                if (details != null) {
                    sender.sendMessage("  §7Details: " + details.getDetailsString());
                }
            }

            sender.sendMessage("§6" + "=".repeat(40));
            sender.sendMessage("§6Total: §e" + playersWithThreeMaces.size() + " §6player(s)");
        }

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
                info.totalMaces++;

                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    Integer maceNumber = meta.getPersistentDataContainer().get(maceNumberKey, PersistentDataType.INTEGER);
                    if (maceNumber != null) {
                        switch (maceNumber) {
                            case 1 -> {
                                info.hasMace1 = true;
                                info.mace1Enchanted = !meta.getEnchants().isEmpty();
                            }
                            case 2 -> info.hasMace2 = true;
                            case 3 -> info.hasMace3 = true;
                        }
                    } else {
                        info.unnumberedMaces++;
                    }
                }
            }
        }
        return info;
    }

    private static class MaceInfo {
        int totalMaces = 0;
        boolean hasMace1 = false;
        boolean hasMace2 = false;
        boolean hasMace3 = false;
        boolean mace1Enchanted = false;
        int unnumberedMaces = 0;

        String getDetailsString() {
            StringBuilder sb = new StringBuilder();
            sb.append(totalMaces).append(" maces total");

            if (hasMace1 || hasMace2 || hasMace3) {
                sb.append(" (");
                List<String> maces = new ArrayList<>();
                if (hasMace1) maces.add("#1" + (mace1Enchanted ? " enchanted" : ""));
                if (hasMace2) maces.add("#2");
                if (hasMace3) maces.add("#3");
                sb.append(String.join(", ", maces)).append(")");
            }

            if (unnumberedMaces > 0) {
                sb.append(", ").append(unnumberedMaces).append(" unnumbered");
            }

            return sb.toString();
        }
    }
}
