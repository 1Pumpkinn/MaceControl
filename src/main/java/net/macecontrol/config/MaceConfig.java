package net.macecontrol.config;

import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Single source of truth for every value read from / written to config.yml.
 * Nothing else in the plugin should call {@code getConfig()} directly - this
 * keeps config keys defined in exactly one place and makes values easy to
 * find, validate and unit test.
 */
public class MaceConfig {

    private static final String MAX_MACES = "max-maces";
    private static final String ENCHANTABLE_MACES = "enchantable-maces";
    private static final String COOLDOWN_SECONDS = "mace-cooldown-seconds";
    private static final String BANNED_ENCHANTMENTS = "banned-enchantments";
    private static final String MESSAGES_ENABLED = "messages-enabled";
    private static final String BROADCASTS_ENABLED = "broadcasts-enabled";

    private static final int DEFAULT_MAX_MACES = 3;
    private static final int DEFAULT_ENCHANTABLE_MACES = 1;
    private static final int DEFAULT_COOLDOWN_SECONDS = 5;

    private final JavaPlugin plugin;

    public MaceConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public int getMaxMaces() {
        return plugin.getConfig().getInt(MAX_MACES, DEFAULT_MAX_MACES);
    }

    public void setMaxMaces(int value) {
        plugin.getConfig().set(MAX_MACES, value);
        plugin.saveConfig();
    }

    public int getEnchantableMaces() {
        return plugin.getConfig().getInt(ENCHANTABLE_MACES, DEFAULT_ENCHANTABLE_MACES);
    }

    public void setEnchantableMaces(int value) {
        plugin.getConfig().set(ENCHANTABLE_MACES, value);
        plugin.saveConfig();
    }

    public int getMaceCooldownSeconds() {
        return plugin.getConfig().getInt(COOLDOWN_SECONDS, DEFAULT_COOLDOWN_SECONDS);
    }

    public void setMaceCooldownSeconds(int value) {
        plugin.getConfig().set(COOLDOWN_SECONDS, value);
        plugin.saveConfig();
    }

    /** Crafting is fully banned once the max-maces limit is set to zero (or lower). */
    public boolean isMaceCraftingBanned() {
        return getMaxMaces() <= 0;
    }

    public boolean isMessagesEnabled() {
        return plugin.getConfig().getBoolean(MESSAGES_ENABLED, false);
    }

    public boolean isBroadcastsEnabled() {
        return plugin.getConfig().getBoolean(BROADCASTS_ENABLED, false);
    }

    /**
     * Enchantments listed here are stripped from maces (rerolled at the enchanting
     * table, removed on enchant, and removed from anvil results). Returns an empty,
     * immutable list - and therefore behaves as a no-op - when nothing is configured.
     */
    public List<Enchantment> getBannedEnchantments() {
        List<String> keys = plugin.getConfig().getStringList(BANNED_ENCHANTMENTS);
        if (keys.isEmpty()) {
            return Collections.emptyList();
        }

        List<Enchantment> enchantments = new ArrayList<>(keys.size());
        for (String key : keys) {
            Enchantment enchantment = Registry.ENCHANTMENT.get(
                    org.bukkit.NamespacedKey.minecraft(key.toLowerCase().trim()));
            if (enchantment != null) {
                enchantments.add(enchantment);
            } else {
                plugin.getLogger().warning("Unknown enchantment in banned-enchantments: " + key);
            }
        }
        return enchantments;
    }
}
