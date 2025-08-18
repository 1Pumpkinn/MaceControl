package net.macecontrol;

import net.macecontrol.managers.CombatManager;
import net.macecontrol.managers.PluginDataManager;
import net.macecontrol.utils.EnchantmentManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    private PluginDataManager dataManager;
    private net.macecontrol.MaceControl maceControl;
    private net.macecontrol.PotionRestrictions potionRestrictions;
    private CombatManager combatManager;
    private EnchantmentManager enchantmentManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        dataManager = new PluginDataManager(this);
        maceControl = new net.macecontrol.MaceControl(this, dataManager);
        potionRestrictions = new net.macecontrol.PotionRestrictions();
        SkriptConversion skriptConversion = new SkriptConversion(this);

        // Initialize new managers
        combatManager = new CombatManager(this);
        enchantmentManager = new EnchantmentManager();

        // Register event listeners
        getServer().getPluginManager().registerEvents(maceControl, this);
        getServer().getPluginManager().registerEvents(new net.macecontrol.HeavyCoreInteractions(), this);
        getServer().getPluginManager().registerEvents(potionRestrictions, this);
        getServer().getPluginManager().registerEvents(skriptConversion, this);
        getServer().getPluginManager().registerEvents(combatManager, this);
        getServer().getPluginManager().registerEvents(enchantmentManager, this);

        // Register commands
        net.macecontrol.MaceCommands maceCommands = new net.macecontrol.MaceCommands(this, dataManager);
        this.getCommand("macefind").setExecutor(maceCommands);
        this.getCommand("maceclean").setExecutor(maceCommands);
        this.getCommand("macereset").setExecutor(maceCommands);
        this.getCommand("macecount").setExecutor(maceCommands);

        getCommand("spawn").setExecutor(skriptConversion);
        getCommand("setspawn").setExecutor(skriptConversion);
        getCommand("giveweapon").setExecutor(skriptConversion);

        getLogger().info("-- Mace limit: 3 maces ENABLED --");
        getLogger().info("-- Current maces crafted: " + dataManager.getTotalMacesCrafted() + "/3 --");
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

    public net.macecontrol.MaceControl getMaceControl() {
        return maceControl;
    }

    public CombatManager getCombatManager() {
        return combatManager;
    }

    public EnchantmentManager getEnchantmentManager() {
        return enchantmentManager;
    }
}