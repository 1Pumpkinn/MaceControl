package net.macecontrol.commands;

import net.macecontrol.config.MaceConfig;
import net.macecontrol.util.MessageUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MaceSetCommand implements MaceSubCommand {

    private static final Map<String, String> EDITABLE_MESSAGE_PATHS = new LinkedHashMap<>();

    static {
        EDITABLE_MESSAGE_PATHS.put("crafted", "crafting.mace-crafted");
        EDITABLE_MESSAGE_PATHS.put("broadcast", "crafting.mace-broadcast");
        EDITABLE_MESSAGE_PATHS.put("limit", "crafting.limit-reached");
        EDITABLE_MESSAGE_PATHS.put("join", "join.available");
        EDITABLE_MESSAGE_PATHS.put("join-full", "join.all-crafted");
        EDITABLE_MESSAGE_PATHS.put("enchant-deny", "restrictions.enchant-denied");
        EDITABLE_MESSAGE_PATHS.put("anvil-deny", "restrictions.anvil-denied");
    }

    private final JavaPlugin plugin;
    private final MaceConfig config;

    public MaceSetCommand(JavaPlugin plugin, MaceConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public String name() {
        return "set";
    }

    @Override
    public String permission() {
        return "macecontrol.set";
    }

    @Override
    public String description() {
        return "Configure mace settings";
    }

    @Override
    public String usageArgs() {
        return "<max|enchantable|cooldown|message> <value>";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendUsage(sender);
            return true;
        }

        String type = args[0].toLowerCase();
        if (type.equals("message")) {
            return handleSetMessage(sender, args);
        }
        return handleSetNumber(sender, type, args[1]);
    }

    private void sendUsage(CommandSender sender) {
        MessageUtil.sendMessages(sender,
                "&6Mace Configuration Commands:",
                "&e/macecontrol set max <number> &7- Set maximum craftable maces",
                "&e/macecontrol set enchantable <number> &7- Set number of enchantable maces",
                "&e/macecontrol set cooldown <seconds> &7- Set mace cooldown in seconds",
                "&e/macecontrol set message <path> <new message> &7- Change a plugin message",
                "&7Current Settings: Max: " + config.getMaxMaces() + ", Enchantable: " + config.getEnchantableMaces()
                        + ", Cooldown: " + config.getMaceCooldownSeconds() + "s"
        );
    }

    private boolean handleSetNumber(CommandSender sender, String type, String rawValue) {
        int value;
        try {
            value = Integer.parseInt(rawValue);
        } catch (NumberFormatException e) {
            MessageUtil.sendMessage(sender, "&cInvalid number format.");
            return true;
        }

        if (value < 0) {
            MessageUtil.sendMessage(sender, "&cValue must be 0 or greater.");
            return true;
        }

        switch (type) {
            case "max" -> {
                config.setMaxMaces(value);
                MessageUtil.sendMessage(sender, "&aMaximum maces set to &6" + value);
                plugin.getLogger().info("Max maces updated to " + value + " by " + sender.getName());
            }
            case "enchantable" -> {
                if (value > config.getMaxMaces()) {
                    MessageUtil.sendMessage(sender, "&cEnchantable maces cannot be greater than max maces (&6"
                            + config.getMaxMaces() + "&c).");
                    return true;
                }
                config.setEnchantableMaces(value);
                MessageUtil.sendMessage(sender, "&aEnchantable maces set to &6" + value);
                plugin.getLogger().info("Enchantable maces updated to " + value + " by " + sender.getName());
            }
            case "cooldown" -> {
                config.setMaceCooldownSeconds(value);
                MessageUtil.sendMessage(sender, "&aMace cooldown set to &6" + value + "s");
                plugin.getLogger().info("Mace cooldown updated to " + value + " seconds by " + sender.getName());
            }
            default -> MessageUtil.sendMessage(sender, "&cInvalid type. Use 'max', 'enchantable', or 'cooldown'.");
        }
        return true;
    }

    private boolean handleSetMessage(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtil.sendMessage(sender, "&cUsage: /macecontrol set message <type> <new message>");
            MessageUtil.sendMessage(sender, "&7Types: &e" + String.join(", ", EDITABLE_MESSAGE_PATHS.keySet()));
            return true;
        }

        String alias = args[1].toLowerCase();
        String configPath = EDITABLE_MESSAGE_PATHS.get(alias);
        if (configPath == null) {
            MessageUtil.sendMessage(sender, "&cInvalid message type! Available: &e"
                    + String.join(", ", EDITABLE_MESSAGE_PATHS.keySet()));
            return true;
        }

        String newMessage = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        String fullPath = "messages." + configPath;

        if (newMessage.contains(",")) {
            plugin.getConfig().set(fullPath, Arrays.asList(newMessage.split(",")));
        } else {
            plugin.getConfig().set(fullPath, newMessage);
        }
        plugin.saveConfig();

        MessageUtil.sendMessage(sender, "&aMessage '&6" + alias + "&a' updated successfully!");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return MaceCommandManager.filterByPrefix(
                    Arrays.asList("max", "enchantable", "cooldown", "message"), args[0]);
        }

        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "message" -> MaceCommandManager.filterByPrefix(
                        new ArrayList<>(EDITABLE_MESSAGE_PATHS.keySet()), args[1]);
                case "max" -> Collections.singletonList(String.valueOf(defaultInt("max-maces", 3)));
                case "enchantable" -> Collections.singletonList(String.valueOf(defaultInt("enchantable-maces", 1)));
                case "cooldown" -> Collections.singletonList(String.valueOf(defaultInt("mace-cooldown-seconds", 5)));
                default -> Collections.emptyList();
            };
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("message")) {
            return suggestCurrentMessage(args[1].toLowerCase(), args[2]);
        }

        return Collections.emptyList();
    }

    private int defaultInt(String path, int fallback) {
        var defaults = plugin.getConfig().getDefaults();
        return defaults != null ? defaults.getInt(path, fallback) : fallback;
    }

    private List<String> suggestCurrentMessage(String alias, String input) {
        String configPath = EDITABLE_MESSAGE_PATHS.get(alias);
        if (configPath == null) return Collections.emptyList();

        var defaults = plugin.getConfig().getDefaults();
        Object defaultValue = defaults != null ? defaults.get("messages." + configPath) : null;
        if (defaultValue == null) return Collections.emptyList();

        String suggestion = defaultValue instanceof List<?> list
                ? String.join(",", list.stream().map(String::valueOf).toList())
                : defaultValue.toString();

        return MaceCommandManager.filterByPrefix(Collections.singletonList(suggestion), input);
    }
}