package net.macecontrol.commands;

import net.macecontrol.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Single executor/tab-completer for /macecontrol. Every feature is a
 * subcommand ("/macecontrol find", "/macecontrol set max 5", ...) registered
 * here rather than its own top-level bukkit command, so adding a new feature
 * never touches plugin.yml again.
 */
public class MaceCommandManager implements CommandExecutor, TabCompleter {

    // LinkedHashMap so /macecontrol (no args) lists subcommands in registration order.
    private final Map<String, MaceSubCommand> subCommands = new LinkedHashMap<>();

    public void register(MaceSubCommand command) {
        subCommands.put(command.name().toLowerCase(), command);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        MaceSubCommand handler = subCommands.get(args[0].toLowerCase());
        if (handler == null) {
            MessageUtil.sendMessage(sender, "&cUnknown subcommand '&e" + args[0] + "&c'.");
            sendHelp(sender, label);
            return true;
        }

        if (!sender.hasPermission(handler.permission())) {
            MessageUtil.sendNoPermission(sender);
            return true;
        }

        return handler.execute(sender, Arrays.copyOfRange(args, 1, args.length));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> visible = new ArrayList<>();
            for (MaceSubCommand sub : subCommands.values()) {
                if (sender.hasPermission(sub.permission())) {
                    visible.add(sub.name());
                }
            }
            return filterByPrefix(visible, args[0]);
        }

        MaceSubCommand handler = subCommands.get(args[0].toLowerCase());
        if (handler == null || !sender.hasPermission(handler.permission())) {
            return Collections.emptyList();
        }
        return handler.tabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
    }

    private void sendHelp(CommandSender sender, String label) {
        MessageUtil.sendMessage(sender, "&6MaceControl Commands:");
        for (MaceSubCommand sub : subCommands.values()) {
            if (!sender.hasPermission(sub.permission())) continue;

            String args = sub.usageArgs().isEmpty() ? "" : " " + sub.usageArgs();
            MessageUtil.sendMessage(sender, "&e/" + label + " " + sub.name() + args + " &7- " + sub.description());
        }
    }

    static List<String> filterByPrefix(List<String> options, String input) {
        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(input.toLowerCase()))
                .toList();
    }
}