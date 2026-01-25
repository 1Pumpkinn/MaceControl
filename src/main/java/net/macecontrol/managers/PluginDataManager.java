package net.macecontrol.managers;

import net.macecontrol.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class PluginDataManager {

    private final Main plugin;
    private int totalMacesCrafted = 0;
    private File dataFile;
    private FileConfiguration dataConfig;

    public PluginDataManager(Main plugin) {
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
            plugin.getLogger().info("No existing mace data found, starting fresh");
        }
    }

    private void saveData() {
        dataConfig.set("totalMacesCrafted", totalMacesCrafted);

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save mace data to file!");
            e.printStackTrace();
        }
    }

    public int getTotalMacesCrafted() {
        return totalMacesCrafted;
    }

    public void incrementTotalMaces() {
        totalMacesCrafted++;
        plugin.getLogger().info("Mace count incremented to: " + totalMacesCrafted);
        saveData();
    }

    public void setTotalMacesCrafted(int count) {
        totalMacesCrafted = count;
        plugin.getLogger().info("Mace count manually set to: " + totalMacesCrafted);
        saveData();
    }

    public void resetMaceData() {
        totalMacesCrafted = 0;
        plugin.getLogger().info("All mace data has been reset");
        saveData();
    }

    public void forceSave() {
        saveData();
    }
}