package net.macecontrol;

import net.macecontrol.managers.EnchantmentManager;
import net.macecontrol.managers.PluginDataManager;
import net.macecontrol.utils.MaceUtils;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    private PluginDataManager dataManager;
    private MaceControl maceControl;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        MaceUtils.init(this);
        dataManager = new PluginDataManager(this);
        maceControl = new MaceControl(this, dataManager);

        // Register event listeners
        getServer().getPluginManager().registerEvents(maceControl, this);
        getServer().getPluginManager().registerEvents(new EnchantmentManager(this), this);
        getServer().getPluginManager().registerEvents(new HeavyCoreInteractions(this), this);

        // Register commands
        MaceCommands maceCommands = new MaceCommands(this, dataManager);
        this.getCommand("macefind").setExecutor(maceCommands);
        this.getCommand("macefind").setTabCompleter(maceCommands);
        this.getCommand("maceclean").setExecutor(maceCommands);
        this.getCommand("maceclean").setTabCompleter(maceCommands);
        this.getCommand("macereset").setExecutor(maceCommands);
        this.getCommand("macereset").setTabCompleter(maceCommands);
        this.getCommand("macecount").setExecutor(maceCommands);
        this.getCommand("macecount").setTabCompleter(maceCommands);
        this.getCommand("maceset").setExecutor(maceCommands);
        this.getCommand("maceset").setTabCompleter(maceCommands);

        getLogger().info("-- Mace limit: " + getMaxMaces() + " maces ENABLED --");
        getLogger().info("-- Current maces crafted: " + dataManager.getTotalMacesCrafted() + "/" + getMaxMaces() + " --");
    }

    @Override
    public void onDisable() {
        // Save data when plugin shuts down
        if (dataManager != null) {
            dataManager.forceSave();
            getLogger().info("Mace data saved on shutdown");
        }
        getLogger().info("-- MACE CONTROL DISABLED --");
    }

    public PluginDataManager getDataManager() {
        return dataManager;
    }

    public MaceControl getMaceControl() {
        return maceControl;
    }

    public int getMaxMaces() {
        return getConfig().getInt("max-maces", 3);
    }

    public void setMaxMaces(int value) {
        getConfig().set("max-maces", value);
        saveConfig();
    }

    public int getEnchantableMaces() {
        return getConfig().getInt("enchantable-maces", 1);
    }

    public void setEnchantableMaces(int value) {
        getConfig().set("enchantable-maces", value);
        saveConfig();
    }
}