package net.macecontrol.commands;

import net.macecontrol.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Routes every mace-related command to its {@link MaceSubCommand}, handling
 * the shared permission check in one place.
 */
public class MaceCommandManager implements CommandExecutor, TabCompleter {

    private final Map<String, MaceSubCommand> commands = new HashMap<>();

    public void register(MaceSubCommand command) {
        commands.put(command.name().toLowerCase(), command);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MaceSubCommand handler = commands.get(command.getName().toLowerCase());
        if (handler == null) return false;

        if (!sender.hasPermission(handler.permission())) {
            MessageUtil.sendNoPermission(sender);
            return true;
        }

        return handler.execute(sender, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        MaceSubCommand handler = commands.get(command.getName().toLowerCase());
        if (handler == null || !sender.hasPermission(handler.permission())) {
            return Collections.emptyList();
        }
        return handler.tabComplete(sender, args);
    }

    static List<String> filterByPrefix(List<String> options, String input) {
        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(input.toLowerCase()))
                .toList();
    }
}
