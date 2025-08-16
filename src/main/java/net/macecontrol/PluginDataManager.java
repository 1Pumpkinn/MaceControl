package net.macecontrol;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PluginDataManager {

    private final net.macecontrol.Main plugin;
    private final Map<UUID, Integer> playerMaceCount = new HashMap<>();
    private int totalMacesCrafted = 0;
    private File dataFile;
    private FileConfiguration dataConfig;

    public PluginDataManager(net.macecontrol.Main plugin) {
        this.plugin = plugin;
        setupDataFile();
        loadData();
    }

    private void setupDataFile() {
        dataFile = new File(plugin.getDataFolder(), "macedata.yml");
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create macedata.yml file!");
                e.printStackTrace();
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void loadData() {
        if (dataConfig.contains("totalMacesCrafted")) {
            totalMacesCrafted = dataConfig.getInt("totalMacesCrafted");
            plugin.getLogger().info("Loaded mace data: " + totalMacesCrafted + " maces have been crafted");
        } else {
            plugin.getLogger().info("No existing mace data found");
        }

        // Load player mace counts if they exist
        if (dataConfig.contains("playerMaceCounts")) {
            for (String uuidString : dataConfig.getConfigurationSection("playerMaceCounts").getKeys(false)) {
                try {
                    UUID playerId = UUID.fromString(uuidString);
                    int count = dataConfig.getInt("playerMaceCounts." + uuidString);
                    playerMaceCount.put(playerId, count);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in mace data: " + uuidString);
                }
            }
            plugin.getLogger().info("Loaded mace counts for " + playerMaceCount.size() + " players");
        }
    }

    private void saveData() {
        dataConfig.set("totalMacesCrafted", totalMacesCrafted);

        // Save player mace counts
        dataConfig.set("playerMaceCounts", null); // Clear old data
        for (Map.Entry<UUID, Integer> entry : playerMaceCount.entrySet()) {
            dataConfig.set("playerMaceCounts." + entry.getKey().toString(), entry.getValue());
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save mace data to file!");
            e.printStackTrace();
        }
    }

    // ===== MACE COUNTING =====
    public Map<UUID, Integer> getPlayerMaceCount() {
        return playerMaceCount;
    }

    public void incrementPlayerMaceCount(UUID playerId) {
        playerMaceCount.put(playerId, playerMaceCount.getOrDefault(playerId, 0) + 1);
        saveData(); // Save immediately when data changes
    }

    public int getTotalMacesCrafted() {
        return totalMacesCrafted;
    }

    public void incrementTotalMaces() {
        totalMacesCrafted++;
        plugin.getLogger().info("Mace count incremented to: " + totalMacesCrafted);
        saveData(); // Save immediately when data changes
    }

    // Method to manually set the total (useful for admin commands or corrections)
    public void setTotalMacesCrafted(int count) {
        totalMacesCrafted = count;
        plugin.getLogger().info("Mace count manually set to: " + totalMacesCrafted);
        saveData();
    }

    // Method to reset all mace data (useful for admin commands)
    public void resetMaceData() {
        totalMacesCrafted = 0;
        playerMaceCount.clear();
        plugin.getLogger().info("All mace data has been reset");
        saveData();
    }

    // Method to force save (useful for plugin disable)
    public void forceSave() {
        saveData();
    }
}