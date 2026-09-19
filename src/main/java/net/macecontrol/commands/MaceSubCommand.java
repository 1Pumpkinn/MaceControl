package net.macecontrol.commands;

import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/**
 * One /macefind, /maceclean, /macereset, /macecount or /maceset command.
 * Each implementation owns its own permission check, execution and tab
 * completion, so {@link MaceCommandManager} only needs to dispatch.
 */
public interface MaceSubCommand {

    /** The bukkit command name this handles, e.g. "macefind". */
    String name();

    String permission();

    boolean execute(CommandSender sender, String[] args);

    default List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
