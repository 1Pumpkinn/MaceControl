package net.macecontrol.commands;

import net.macecontrol.config.MaceConfig;
import net.macecontrol.data.MaceDataStore;
import net.macecontrol.util.MessageUtil;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class MaceCountCommand implements MaceSubCommand {

    private final MaceConfig config;
    private final MaceDataStore dataStore;

    public MaceCountCommand(MaceConfig config, MaceDataStore dataStore) {
        this.config = config;
        this.dataStore = dataStore;
    }

    @Override
    public String name() {
        return "count";
    }

    @Override
    public String permission() {
        return "macecontrol.count";
    }

    @Override
    public String description() {
        return "View or set the mace count";
    }

    @Override
    public String usageArgs() {
        return "[set <value>]";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        int maxMaces = config.getMaxMaces();

        if (args.length == 0) {
            MessageUtil.sendMessage(sender, "&6Current mace count: &e" + dataStore.getTotalMacesCrafted() + "/" + maxMaces);
            return true;
        }

        if (!args[0].equalsIgnoreCase("set") || args.length < 2) {
            MessageUtil.sendMessage(sender, "&cUsage: /macecontrol count [set <0-" + maxMaces + ">]");
            return true;
        }

        try {
            int newCount = Integer.parseInt(args[1]);
            if (newCount < 0 || newCount > maxMaces) {
                MessageUtil.sendMessage(sender, "&cInvalid count! Must be between 0 and " + maxMaces + ".");
                return true;
            }

            dataStore.setTotalMacesCrafted(newCount);
            MessageUtil.sendMessage(sender, "&aMace count set to: &6" + newCount + "/" + maxMaces);
            MessageUtil.broadcastCountAdjusted(newCount, maxMaces);
        } catch (NumberFormatException e) {
            MessageUtil.sendMessage(sender, "&cInvalid number! Usage: /macecontrol count set <0-" + maxMaces + ">");
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return MaceCommandManager.filterByPrefix(Collections.singletonList("set"), args[0]);
        }
        return Collections.emptyList();
    }
}