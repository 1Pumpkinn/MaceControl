package net.macecontrol.commands;

import net.macecontrol.config.MaceConfig;
import net.macecontrol.data.MaceDataStore;
import net.macecontrol.listeners.MaceInventoryGuardListener;
import net.macecontrol.util.MessageUtil;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class MaceCleanCommand implements MaceSubCommand {

    private final MaceConfig config;
    private final MaceDataStore dataStore;
    private final MaceInventoryGuardListener inventoryGuard;

    public MaceCleanCommand(MaceConfig config, MaceDataStore dataStore, MaceInventoryGuardListener inventoryGuard) {
        this.config = config;
        this.dataStore = dataStore;
        this.inventoryGuard = inventoryGuard;
    }

    @Override
    public String name() {
        return "maceclean";
    }

    @Override
    public String permission() {
        return "macecontrol.maceclean";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0 || !args[0].equalsIgnoreCase("confirm")) {
            MessageUtil.sendMessages(sender,
                    "&e&lMACECLEAN",
                    "&7This command will:",
                    "&c• Remove ALL invalid maces from online players",
                    "&c• Reset mace crafting data (allows new maces to be crafted)",
                    "&c• Clear the macedata.yml file",
                    "",
                    "&eThis is a DESTRUCTIVE operation!",
                    "&cType '&e/maceclean confirm&c' to proceed."
            );
            return true;
        }

        MessageUtil.sendMessage(sender, "&6Cleaning invalid maces from all online players and resetting mace data...");

        int removed = inventoryGuard.cleanAllOnlinePlayers();
        dataStore.resetMaceData();

        MessageUtil.sendMessages(sender,
                "&aClean completed!",
                "&a• Removed " + removed + " invalid mace(s) from online players",
                "&a• Mace data has been reset - players can now craft maces again!",
                "&7Remember: Only " + config.getMaxMaces() + " maces total, " + config.getEnchantableMaces() + " can be enchanted."
        );
        MessageUtil.broadcastDataReset();
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return MaceCommandManager.filterByPrefix(Collections.singletonList("confirm"), args[0]);
        }
        return Collections.emptyList();
    }
}
