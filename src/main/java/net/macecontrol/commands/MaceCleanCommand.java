package net.macecontrol.commands;

import net.macecontrol.cleaning.MaceCleaner;
import net.macecontrol.config.MaceConfig;
import net.macecontrol.data.MaceDataStore;
import net.macecontrol.util.MessageUtil;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class MaceCleanCommand implements MaceSubCommand {

    private final MaceConfig config;
    private final MaceDataStore dataStore;
    private final MaceCleaner cleaner;

    public MaceCleanCommand(MaceConfig config, MaceDataStore dataStore, MaceCleaner cleaner) {
        this.config = config;
        this.dataStore = dataStore;
        this.cleaner = cleaner;
    }

    @Override
    public String name() {
        return "clean";
    }

    @Override
    public String permission() {
        return "macecontrol.clean";
    }

    @Override
    public String description() {
        return "Deep-clean invalid maces everywhere and reset mace data";
    }

    @Override
    public String usageArgs() {
        return "[confirm]";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0 || !args[0].equalsIgnoreCase("confirm")) {
            MessageUtil.sendMessages(sender,
                    "&e&lMACECLEAN",
                    "&7This command will remove every invalid mace from:",
                    "&c• Online players' inventories and ender chests",
                    "&c• Chests, barrels, hoppers, droppers, dispensers, shulker boxes",
                    "&c• Decorated pots and chiseled bookshelves/shelves",
                    "&c• Bundles - and shulker boxes/bundles nested inside each other",
                    "&c• Items dropped on the ground, in every loaded chunk",
                    "&7Offline players are swept automatically the moment they rejoin.",
                    "",
                    "&cIt will also reset mace crafting data (allows new maces to be crafted)",
                    "&eThis is a DESTRUCTIVE operation!",
                    "&cType '&e/macecontrol clean confirm&c' to proceed."
            );
            return true;
        }

        MessageUtil.sendMessage(sender, "&6Deep-cleaning invalid maces across the server and resetting mace data...");

        int removed = cleaner.cleanEverythingLoaded();
        dataStore.resetMaceData();

        MessageUtil.sendMessages(sender,
                "&aClean completed!",
                "&a• Removed " + removed + " invalid mace(s) from players, containers and the ground",
                "&a• Mace data has been reset - players can now craft maces again!",
                "&7Remember: Only " + config.getMaxMaces() + " maces total, " + config.getEnchantableMaces() + " can be enchanted.",
                "&7Note: only currently loaded chunks were swept, and offline players will be swept on their next join."
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