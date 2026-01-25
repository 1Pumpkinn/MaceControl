package net.macecontrol.utils;

import net.macecontrol.Main;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class MaceUtils {

    private static NamespacedKey maceNumberKey;

    public static void init(Main plugin) {
        maceNumberKey = new NamespacedKey(plugin, "mace_number");
    }

    public static NamespacedKey getMaceNumberKey() {
        return maceNumberKey;
    }

    public static boolean isMace(ItemStack item) {
        return item != null && item.getType() == Material.MACE;
    }

    public static Integer getMaceNumber(ItemStack item) {
        if (!isMace(item)) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(maceNumberKey, PersistentDataType.INTEGER);
    }

    public static boolean isValidMace(ItemStack item, int maxMaces) {
        if (!isMace(item)) return false;
        Integer number = getMaceNumber(item);
        return number != null && number >= 1 && number <= maxMaces;
    }

    public static boolean isEnchantable(ItemStack item, int enchantableLimit) {
        Integer number = getMaceNumber(item);
        return number != null && number <= enchantableLimit;
    }

    public static boolean isHeavyCore(ItemStack item) {
        return item != null && item.getType() == Material.HEAVY_CORE;
    }
}
