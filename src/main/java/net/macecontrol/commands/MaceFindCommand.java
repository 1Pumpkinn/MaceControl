package net.macecontrol.commands;


import net.macecontrol.config.MaceConfig;
import net.macecontrol.data.MaceDataStore;
import net.macecontrol.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MaceFindCommand implements MaceSubCommand {

    private static final long OFFLINE_LOOKBACK_MILLIS = TimeUnit.DAYS.toMillis(30);

    private final MaceConfig config;
    private final MaceDataStore dataStore;

    public MaceFindCommand(MaceConfig config, MaceDataStore dataStore) {
        this.config = config;
        this.dataStore = dataStore;
    }

    @Override
    public String name() {
        return "macefind";
    }

    @Override
    public String permission() {
        return "macecontrol.macefind";
    }

    @Override
    public String description() {
        return "Find all valid maces currently on the server";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        int maxMaces = config.getMaxMaces();
        int enchantableMaces = config.getEnchantableMaces();

        MaceScanner scanner = new MaceScanner(maxMaces, enchantableMaces);

        MessageUtil.sendMessages(
                sender,
                "&6Scanning for maces on the server...",
                "&7This may take a moment as we scan all loaded chunks..."
        );

        Map<String, MaceScanResult> perPlayerDetails = new LinkedHashMap<>();
        List<String> playerSummaries = new ArrayList<>();

        int totalValidMaces = 0;

        // Scan online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            MaceScanResult result = scanner.scanPlayer(player);

            if (result.getTotalValidMaces() > 0) {
                playerSummaries.add(player.getName() + " §a(Online)");
                perPlayerDetails.put(player.getName(), result);

                totalValidMaces += result.getTotalValidMaces();
            }
        }

        // Scan recently active offline players
        long recentCutoff = System.currentTimeMillis() - OFFLINE_LOOKBACK_MILLIS;

        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {

            if (offlinePlayer.isOnline()) {
                continue;
            }

            if (offlinePlayer.getLastPlayed() <= recentCutoff) {
                continue;
            }

            MaceScanResult result = scanner.scanEnderChestOf(offlinePlayer);

            if (result.getTotalValidMaces() > 0) {
                String playerName = offlinePlayer.getName();

                if (playerName == null) {
                    playerName = offlinePlayer.getUniqueId().toString();
                }

                playerSummaries.add(
                        playerName + " §7(Offline - Enderchest only)"
                );

                perPlayerDetails.put(playerName, result);

                totalValidMaces += result.getTotalValidMaces();
            }
        }

        // Scan containers in loaded chunks
        MaceScanResult worldResult = scanner.scanLoadedWorldContainers();

        totalValidMaces += worldResult.getTotalValidMaces();

        // Display results
        reportResults(
                sender,
                maxMaces,
                enchantableMaces,
                totalValidMaces,
                playerSummaries,
                perPlayerDetails,
                worldResult
        );

        return true;
    }

    private void reportResults(
            CommandSender sender,
            int maxMaces,
            int enchantableMaces,
            int totalValidMaces,
            List<String> playerSummaries,
            Map<String, MaceScanResult> perPlayerDetails,
            MaceScanResult worldResult
    ) {
        String separator = "&6" + "=".repeat(50);

        MessageUtil.sendMessages(
                sender,
                separator,
                "&6Mace Status Report:",
                separator,
                "&eTotal valid maces found: &6"
                        + totalValidMaces
                        + "&e/&6"
                        + maxMaces,

                "&eTotal maces crafted: &6"
                        + dataStore.getTotalMacesCrafted(),

                "&eEnchantable maces: &6"
                        + enchantableMaces
        );

        // Nothing found
        if (playerSummaries.isEmpty()
                && worldResult.getTotalValidMaces() == 0) {

            MessageUtil.sendMessage(
                    sender,
                    "&cNo maces found anywhere on the server!"
            );

        } else {

            // Player maces
            if (!playerSummaries.isEmpty()) {

                MessageUtil.sendMessage(
                        sender,
                        "&6Players with maces:"
                );

                for (String summary : playerSummaries) {

                    MessageUtil.sendMessage(
                            sender,
                            "&e• " + summary
                    );

                    String playerName = summary.split(" ")[0];

                    MaceScanResult details =
                            perPlayerDetails.get(playerName);

                    if (details != null) {

                        MessageUtil.sendMessage(
                                sender,
                                "  &7Details: "
                                        + details.getDetailsString(
                                        enchantableMaces
                                )
                        );
                    }
                }
            }

            // World containers
            if (worldResult.getTotalValidMaces() > 0) {

                MessageUtil.sendMessages(
                        sender,

                        "&6World containers (chests/shulkers):",

                        "&e• Found in loaded chunks: &6"
                                + worldResult.getTotalValidMaces()
                                + " maces",

                        "  &7Details: "
                                + worldResult.getDetailsString(
                                enchantableMaces
                        ),

                        "  &7Note: Only loaded chunks were scanned"
                );
            }
        }

        MessageUtil.sendMessage(
                sender,
                separator
        );
    }
}
