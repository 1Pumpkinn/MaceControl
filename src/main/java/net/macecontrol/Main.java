package net.macecontrol;

import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    private PluginDataManager dataManager;
    private net.macecontrol.MaceControl maceControl;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        dataManager = new PluginDataManager(this);
        maceControl = new net.macecontrol.MaceControl(this, dataManager);

        getServer().getPluginManager().registerEvents(maceControl, this);
        getServer().getPluginManager().registerEvents(new net.macecontrol.HeavyCoreInteractions(), this);

        getCommand("macefind").setExecutor(new net.macecontrol.MaceCommands(this, dataManager));

        getLogger().info("-- MACE CONTROL ENABLED --");
    }

    @Override
    public void onDisable() {
        getLogger().info("-- MACE CONTROL DISABLED --");
    }

    public PluginDataManager getDataManager() {
        return dataManager;
    }
}
