package net.macecontrol.commands;

import net.macecontrol.config.MaceConfig;
import net.macecontrol.data.MaceDataStore;
import net.macecontrol.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MaceFindCommand implements MaceSubCommand {

    /** Max location lines printed per section so a big find doesn't flood chat. */
    private static final int MAX_LINES_PER_SECTION = 15;

    private final MaceConfig config;
    private final MaceDataStore dataStore;

    public MaceFindCommand(MaceConfig config, MaceDataStore dataStore) {
        this.config = config;
        this.dataStore = dataStore;
    }

    @Override
    public String name() {
        return "find";
    }

    @Override
    public String permission() {
        return "macecontrol.find";
    }

    @Override
    public String description() {
        return "Find all maces on the server (players, containers, entities, bundles, shulkers)";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        int maxMaces = config.getMaxMaces();
        int enchantableMaces = config.getEnchantableMaces();
        MaceScanner scanner = new MaceScanner(maxMaces, enchantableMaces);

        MessageUtil.sendMessage(sender, "&6Scanning online players and all loaded chunks for maces...");

        Map<String, MaceScanResult> playerResults = new LinkedHashMap<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            MaceScanResult result = scanner.scanPlayer(player);
            if (result.getTotalMaces() > 0) {
                playerResults.put(player.getName(), result);
            }
        }

        MaceScanResult containerResult = scanner.scanLoadedWorldContainers();
        MaceScanResult entityResult = scanner.scanLoadedEntities();

        int totalValid = containerResult.getTotalValidMaces() + entityResult.getTotalValidMaces();
        int totalInvalid = containerResult.getInvalidMaces() + entityResult.getInvalidMaces();
        for (MaceScanResult result : playerResults.values()) {
            totalValid += result.getTotalValidMaces();
            totalInvalid += result.getInvalidMaces();
        }

        String separator = "&6" + "=".repeat(50);
        MessageUtil.sendMessages(sender,
                separator,
                "&6Mace Status Report:",
                separator,
                "&eTotal valid maces found: &6" + totalValid + "&e/&6" + maxMaces,
                "&eTotal maces crafted: &6" + dataStore.getTotalMacesCrafted(),
                "&eEnchantable maces: &6" + enchantableMaces
        );
        if (totalInvalid > 0) {
            MessageUtil.sendMessage(sender, "&cInvalid/stale maces found: &6" + totalInvalid + " &7(removed by /macecontrol clean)");
        }

        if (playerResults.isEmpty() && containerResult.getTotalMaces() == 0 && entityResult.getTotalMaces() == 0) {
            MessageUtil.sendMessage(sender, "&cNo maces found on online players or in loaded chunks!");
        } else {
            if (!playerResults.isEmpty()) {
                MessageUtil.sendMessage(sender, "&6Online players with maces:");
                for (Map.Entry<String, MaceScanResult> entry : playerResults.entrySet()) {
                    MessageUtil.sendMessage(sender, "&e• " + entry.getKey() + " &a(Online)");
                    MessageUtil.sendMessage(sender, "  &7Details: " + entry.getValue().getDetailsString(enchantableMaces));
                    sendLocations(sender, entry.getValue().getLocations());
                }
            }

            sendSection(sender, "&6Containers (chests, barrels, hoppers, droppers, dispensers, pots, shelves, shulkers, etc.):",
                    containerResult, enchantableMaces);
            sendSection(sender, "&6Entities & ground (dropped items, item frames, armor stands, minecarts, mobs):",
                    entityResult, enchantableMaces);
        }

        MessageUtil.sendMessages(sender,
                "&7Note: only online players and loaded chunks can be scanned - offline players' inventories and unloaded chunks are not included.",
                separator
        );
        return true;
    }

    private void sendSection(CommandSender sender, String header, MaceScanResult result, int enchantableMaces) {
        if (result.getTotalMaces() == 0) return;

        MessageUtil.sendMessages(sender,
                header,
                "  &7Details: " + result.getDetailsString(enchantableMaces)
        );
        sendLocations(sender, result.getLocations());
    }

    private void sendLocations(CommandSender sender, List<String> locations) {
        int shown = Math.min(locations.size(), MAX_LINES_PER_SECTION);
        for (int i = 0; i < shown; i++) {
            MessageUtil.sendMessage(sender, "  &7- " + locations.get(i));
        }
        if (locations.size() > shown) {
            MessageUtil.sendMessage(sender, "  &7...and " + (locations.size() - shown) + " more");
        }
    }
}