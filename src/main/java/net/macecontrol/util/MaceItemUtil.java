package net.macecontrol.util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Reads and writes the persistent tags every plugin-crafted mace carries
 * (its crafted-order number and the "generation" it was crafted in), and
 * provides small predicates used throughout the plugin.
 * <p>
 * The generation is bumped every time {@code /macecontrol clean} runs. Any mace
 * from an older generation is stale and counts as invalid everywhere - including
 * maces held by offline players or sitting in unloaded chunks, which get removed
 * as soon as they are next seen. Maces with no generation tag are generation 0.
 */
public final class MaceItemUtil {

    private static NamespacedKey maceNumberKey;
    private static NamespacedKey maceGenerationKey;
    private static int currentGeneration = 0;

    private MaceItemUtil() {
    }

    public static void init(JavaPlugin plugin) {
        maceNumberKey = new NamespacedKey(plugin, "mace_number");
        maceGenerationKey = new NamespacedKey(plugin, "mace_generation");
    }

    public static NamespacedKey getMaceNumberKey() {
        return maceNumberKey;
    }

    public static int getCurrentGeneration() {
        return currentGeneration;
    }

    /** Called by the data store on load and on every clean/reset. */
    public static void setCurrentGeneration(int generation) {
        currentGeneration = generation;
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

    /** The generation this mace was crafted in (0 if it carries no generation tag). */
    public static int getMaceGeneration(ItemStack item) {
        if (!isMace(item)) return 0;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;
        return meta.getPersistentDataContainer().getOrDefault(maceGenerationKey, PersistentDataType.INTEGER, 0);
    }

    /** Tags the given mace with its crafted-order number and the current generation. */
    public static void setMaceNumber(ItemStack item, int number) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(maceNumberKey, PersistentDataType.INTEGER, number);
        meta.getPersistentDataContainer().set(maceGenerationKey, PersistentDataType.INTEGER, currentGeneration);
        item.setItemMeta(meta);
    }

    /** A mace is "valid" while it is from the current generation and its number is within the max-maces limit. */
    public static boolean isValidMace(ItemStack item, int maxMaces) {
        if (!isMace(item)) return false;
        Integer number = getMaceNumber(item);
        return number != null
                && number >= 1
                && number <= maxMaces
                && getMaceGeneration(item) == currentGeneration;
    }

    public static boolean isEnchantable(ItemStack item, int enchantableLimit) {
        Integer number = getMaceNumber(item);
        return number != null
                && number <= enchantableLimit
                && getMaceGeneration(item) == currentGeneration;
    }
}