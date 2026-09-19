package net.macecontrol;

import net.macecontrol.commands.MaceCleanCommand;
import net.macecontrol.commands.MaceCommandManager;
import net.macecontrol.commands.MaceCountCommand;
import net.macecontrol.commands.MaceFindCommand;
import net.macecontrol.commands.MaceResetCommand;
import net.macecontrol.commands.MaceSetCommand;
import net.macecontrol.config.MaceConfig;
import net.macecontrol.data.MaceDataStore;
import net.macecontrol.listeners.BannedEnchantmentListener;
import net.macecontrol.listeners.HeavyCoreListener;
import net.macecontrol.listeners.MaceCombatListener;
import net.macecontrol.listeners.MaceCraftListener;
import net.macecontrol.listeners.MaceEnchantListener;
import net.macecontrol.listeners.MaceInventoryGuardListener;
import net.macecontrol.util.MaceItemUtil;
import net.macecontrol.util.MessageUtil;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Plugin entry point. Only wiring lives here - every feature has its own
 * config/data/listener/command class, so this stays short no matter how the
 * plugin grows.
 */
public final class Main extends JavaPlugin {

    private MaceConfig maceConfig;
    private MaceDataStore dataStore;
    private MaceInventoryGuardListener inventoryGuard;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        MaceItemUtil.init(this);
        MessageUtil.init(this);
        maceConfig = new MaceConfig(this);
        dataStore = new MaceDataStore(this);

        registerListeners();
        registerCommands();

        getLogger().info("-- Mace limit: " + maceConfig.getMaxMaces() + " maces ENABLED --");
        getLogger().info("-- Current maces crafted: " + dataStore.getTotalMacesCrafted() + "/" + maceConfig.getMaxMaces() + " --");
    }

    @Override
    public void onDisable() {
        if (dataStore != null) {
            dataStore.forceSave();
            getLogger().info("Mace data saved on shutdown");
        }
        getLogger().info("-- MACE CONTROL DISABLED --");
    }

    private void registerListeners() {
        inventoryGuard = new MaceInventoryGuardListener(this, maceConfig);
        inventoryGuard.startPeriodicSweep();

        getServer().getPluginManager().registerEvents(new MaceCraftListener(maceConfig, dataStore), this);
        getServer().getPluginManager().registerEvents(new MaceCombatListener(maceConfig), this);
        getServer().getPluginManager().registerEvents(new MaceEnchantListener(maceConfig), this);
        getServer().getPluginManager().registerEvents(new BannedEnchantmentListener(this, maceConfig), this);
        getServer().getPluginManager().registerEvents(inventoryGuard, this);
        getServer().getPluginManager().registerEvents(new HeavyCoreListener(), this);
    }

    private void registerCommands() {
        MaceCommandManager commandManager = new MaceCommandManager();
        commandManager.register(new MaceFindCommand(maceConfig, dataStore));
        commandManager.register(new MaceCleanCommand(maceConfig, dataStore, inventoryGuard));
        commandManager.register(new MaceResetCommand(maceConfig, dataStore));
        commandManager.register(new MaceCountCommand(maceConfig, dataStore));
        commandManager.register(new MaceSetCommand(this, maceConfig));

        for (String commandName : new String[]{"macefind", "maceclean", "macereset", "macecount", "maceset"}) {
            var command = getCommand(commandName);
            if (command != null) {
                command.setExecutor(commandManager);
                command.setTabCompleter(commandManager);
            }
        }
    }

    public MaceConfig getMaceConfig() {
        return maceConfig;
    }

    public MaceDataStore getDataStore() {
        return dataStore;
    }
}
