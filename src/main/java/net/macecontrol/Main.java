package net.macecontrol;

import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    private PluginDataManager dataManager;
    private net.macecontrol.MaceControl maceControl;
    private net.macecontrol.PotionRestrictions potionRestrictions;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        dataManager = new PluginDataManager(this);
        maceControl = new net.macecontrol.MaceControl(this, dataManager);
        potionRestrictions = new net.macecontrol.PotionRestrictions();
        SkriptConversion skriptConversion = new SkriptConversion(this);



        // Register event listeners
        getServer().getPluginManager().registerEvents(maceControl, this);
        getServer().getPluginManager().registerEvents(new net.macecontrol.HeavyCoreInteractions(), this);
        getServer().getPluginManager().registerEvents(potionRestrictions, this);
        getServer().getPluginManager().registerEvents(skriptConversion, this);




        // Register commands
        net.macecontrol.MaceCommands maceCommands = new net.macecontrol.MaceCommands(this, dataManager);
        getCommand("macefind").setExecutor(maceCommands);
        getCommand("maceclean").setExecutor(maceCommands);
        getCommand("spawn").setExecutor(skriptConversion);
        getCommand("setspawn").setExecutor(skriptConversion);




        getLogger().info("-- Mace limit: 3 maces ENABLED --");
    }

    @Override
    public void onDisable() {
        getLogger().info("-- MACE CONTROL DISABLED --");
    }

    public PluginDataManager getDataManager() {
        return dataManager;
    }

    public net.macecontrol.MaceControl getMaceControl() {
        return maceControl;
    }
}