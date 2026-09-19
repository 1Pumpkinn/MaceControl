package net.macecontrol.data;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

/**
 * Persists the running total of maces crafted on the server to macedata.yml.
 */
public class MaceDataStore {

    private static final String DATA_FILE_NAME = "macedata.yml";
    private static final String TOTAL_CRAFTED_KEY = "totalMacesCrafted";

    private final JavaPlugin plugin;
    private final File dataFile;
    private final FileConfiguration dataConfig;

    private int totalMacesCrafted;

    public MaceDataStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = createDataFile();
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        this.totalMacesCrafted = loadTotalCrafted();
    }

    private File createDataFile() {
        File file = new File(plugin.getDataFolder(), DATA_FILE_NAME);
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create " + DATA_FILE_NAME + ": " + e.getMessage());
            }
        }
        return file;
    }

    private int loadTotalCrafted() {
        if (!dataConfig.contains(TOTAL_CRAFTED_KEY)) {
            plugin.getLogger().info("No existing mace data found, starting fresh");
            return 0;
        }
        int loaded = dataConfig.getInt(TOTAL_CRAFTED_KEY);
        plugin.getLogger().info("Loaded mace data: " + loaded + " maces have been crafted");
        return loaded;
    }

    private void persist() {
        dataConfig.set(TOTAL_CRAFTED_KEY, totalMacesCrafted);
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save mace data: " + e.getMessage());
        }
    }

    public int getTotalMacesCrafted() {
        return totalMacesCrafted;
    }

    public void incrementTotalMaces() {
        totalMacesCrafted++;
        plugin.getLogger().info("Mace count incremented to: " + totalMacesCrafted);
        persist();
    }

    public void setTotalMacesCrafted(int count) {
        totalMacesCrafted = count;
        plugin.getLogger().info("Mace count manually set to: " + totalMacesCrafted);
        persist();
    }

    public void resetMaceData() {
        totalMacesCrafted = 0;
        plugin.getLogger().info("All mace data has been reset");
        persist();
    }

    public void forceSave() {
        persist();
    }
}
