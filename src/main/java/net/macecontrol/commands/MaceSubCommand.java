package net.macecontrol.commands;

import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/**
 * One subcommand of /macecontrol (e.g. "find", "clean", "reset", "count", "set").
 * Each implementation owns its own permission check, execution and tab
 * completion, so {@link MaceCommandManager} only needs to dispatch. All
 * {@code args} passed in have already had the subcommand name itself
 * stripped off.
 */
public interface MaceSubCommand {

    /** The word typed after /macecontrol, e.g. "find". Always lowercase. */
    String name();

    /** Permission node required to run this subcommand. */
    String permission();

    /** One-line summary shown in the /macecontrol help listing. */
    String description();

    /** Argument hint shown after the subcommand name in help/usage, e.g. "[confirm]". Empty if none. */
    default String usageArgs() {
        return "";
    }

    boolean execute(CommandSender sender, String[] args);

    default List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}