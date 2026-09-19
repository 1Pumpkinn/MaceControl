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
    /** * The word typed after /macecontrol, e.g. "find". * Always lowercase. */
    String name(); /** * Permission node required to run this subcommand. */
    String permission(); /** * One-line summary shown in the /macecontrol help listing. */
    String description(); /** * Argument hint shown after the subcommand name in help/usage, * e.g. "[confirm]". * * Empty if the command has no arguments. */
    default String usageArgs() { return ""; } /** * Execute the command. * * @param sender command sender * @param args arguments after the subcommand name * @return true if the command executed successfully */
    boolean execute(CommandSender sender, String[] args); /** * Tab completion for this subcommand. */
    default List<String> tabComplete( CommandSender sender, String[] args ) { return Collections.emptyList(); } }