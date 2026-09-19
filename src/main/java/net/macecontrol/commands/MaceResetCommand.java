package net.macecontrol.commands;

import net.macecontrol.config.MaceConfig;
import net.macecontrol.data.MaceDataStore;
import net.macecontrol.util.MessageUtil;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class MaceResetCommand implements MaceSubCommand {

    private final MaceConfig config;
    private final MaceDataStore dataStore;

    public MaceResetCommand(MaceConfig config, MaceDataStore dataStore) {
        this.config = config;
        this.dataStore = dataStore;
    }

    @Override
    public String name() {
        return "reset";
    }

    @Override
    public String permission() {
        return "macecontrol.reset";
    }

    @Override
    public String description() {
        return "Reset all mace data";
    }

    @Override
    public String usageArgs() {
        return "[confirm]";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0 || !args[0].equalsIgnoreCase("confirm")) {
            MessageUtil.sendMessages(sender,
                    "&cThis will reset ALL mace data and allow new maces to be crafted!",
                    "&cType '&e/macecontrol reset confirm&c' to proceed."
            );
            return true;
        }

        dataStore.resetMaceData();
        MessageUtil.sendMessages(sender,
                "&aAll mace data has been reset! Players can now craft maces again.",
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