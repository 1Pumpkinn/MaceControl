package net.macecontrol.util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Reads and writes the persistent "mace number" tag that every plugin-crafted
 * mace carries, and provides small predicates used throughout the plugin.
 */
public final class MaceItemUtil {

    private static NamespacedKey maceNumberKey;

    private MaceItemUtil() {
    }

    public static void init(JavaPlugin plugin) {
        maceNumberKey = new NamespacedKey(plugin, "mace_number");
    }

    public static NamespacedKey getMaceNumberKey() {
        return maceNumberKey;
    }

    public static boolean isMace(ItemStack item) {
        return item != null && item.getType() == Material.MACE;
    }

    public static boolean isHeavyCore(ItemStack item) {
        return item != null && item.getType() == Material.HEAVY_CORE;
    }

    /** The mace's assigned number (the order it was crafted in), or null if untagged. */
    public static Integer getMaceNumber(ItemStack item) {
        if (!isMace(item)) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(maceNumberKey, PersistentDataType.INTEGER);
    }

    /** Tags the given mace with its crafted-order number. */
    public static void setMaceNumber(ItemStack item, int number) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(maceNumberKey, PersistentDataType.INTEGER, number);
        item.setItemMeta(meta);
    }

    /** A mace is "valid" while its number still falls within the current max-maces limit. */
    public static boolean isValidMace(ItemStack item, int maxMaces) {
        if (!isMace(item)) return false;
        Integer number = getMaceNumber(item);
        return number != null && number >= 1 && number <= maxMaces;
    }

    public static boolean isEnchantable(ItemStack item, int enchantableLimit) {
        Integer number = getMaceNumber(item);
        return number != null && number <= enchantableLimit;
    }
}
