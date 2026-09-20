package net.macecontrol.commands;

import net.macecontrol.util.MaceItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds every mace it can reach: online players (inventory, armor, offhand, ender chest, cursor),
 * every inventory-holding block in loaded chunks (chests, barrels, hoppers, droppers, dispensers,
 * shulker boxes, decorated pots, shelves, chiseled bookshelves, furnaces, crafters, ...), and
 * entities (dropped items, item frames, armor stands, mob equipment, minecart/boat/mount inventories).
 * Shulker boxes and bundles are opened recursively, however deeply they are nested.
 * Used by {@link MaceFindCommand}.
 */
public class MaceScanner {

    private final int maxMaces;
    private final int enchantableMaces;

    public MaceScanner(int maxMaces, int enchantableMaces) {
        this.maxMaces = maxMaces;
        this.enchantableMaces = enchantableMaces;
    }

    // ===== Public scans =====

    /** Main inventory, armor, offhand, ender chest and cursor item of an online player. */
    public MaceScanResult scanPlayer(Player player) {
        MaceScanResult result = new MaceScanResult();
        scanInventory(player.getInventory(), "Inventory", result);
        scanInventory(player.getEnderChest(), "Ender chest", result);
        scanItem(player.getItemOnCursor(), "Cursor", result);
        return result;
    }

    /** Every inventory-holding block in currently loaded chunks. */
    public MaceScanResult scanLoadedWorldContainers() {
        MaceScanResult result = new MaceScanResult();

        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (BlockState blockState : chunk.getTileEntities(false)) {
                    if (!(blockState instanceof InventoryHolder holder)) continue;

                    // A double chest's two halves each report the whole double inventory,
                    // so read only this half to avoid counting every mace twice.
                    Inventory inventory = blockState instanceof Chest chest
                            ? chest.getBlockInventory()
                            : holder.getInventory();

                    scanInventory(inventory, describe(blockState.getType().name(), blockState.getLocation()), result);
                }
            }
        }
        return result;
    }

    /** Every non-player entity in loaded chunks that can hold, display or wear an item. */
    public MaceScanResult scanLoadedEntities() {
        MaceScanResult result = new MaceScanResult();

        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : new ArrayList<>(world.getEntities())) {
                if (entity instanceof Player) continue;
                scanEntity(entity, result);
            }
        }
        return result;
    }

    // ===== Internals =====

    private void scanEntity(Entity entity, MaceScanResult result) {
        String where = describe(entity.getType().name(), entity.getLocation());

        if (entity instanceof Item itemEntity) {
            scanItem(itemEntity.getItemStack(), "Dropped item at " + formatLocation(entity.getLocation()), result);
        } else if (entity instanceof ItemFrame frame) {
            scanItem(frame.getItem(), where, result);
        } else if (entity instanceof ItemDisplay display) {
            scanItem(display.getItemStack(), where, result);
        }

        // Minecart chests, hopper minecarts, chest boats, donkeys/mules/llamas, villagers, allays, ...
        if (entity instanceof InventoryHolder holder) {
            scanInventory(holder.getInventory(), where, result);
        }

        // Armor stands and anything a mob is holding or wearing.
        if (entity instanceof LivingEntity living) {
            EntityEquipment equipment = living.getEquipment();
            if (equipment != null) {
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    try {
                        scanItem(equipment.getItem(slot), where + " (" + slot.name().toLowerCase() + ")", result);
                    } catch (IllegalArgumentException | UnsupportedOperationException ignored) {
                        // This entity type doesn't support this equipment slot.
                    }
                }
            }
        }
    }

    private void scanInventory(Inventory inventory, String where, MaceScanResult result) {
        if (inventory == null) return;
        for (ItemStack item : inventory.getContents()) {
            scanItem(item, where, result);
        }
    }

    /**
     * Records the item if it is a mace; if it is a shulker box or bundle, opens it and
     * scans what is inside (recursively).
     */
    private void scanItem(ItemStack item, String where, MaceScanResult result) {
        if (item == null || item.getType().isAir()) return;

        if (MaceItemUtil.isMace(item)) {
            recordMace(item, where, result);
            return;
        }

        // Cheap type check first so we don't clone item meta for every ordinary item.
        String typeName = item.getType().name();
        boolean shulker = typeName.endsWith("SHULKER_BOX");
        boolean bundle = typeName.endsWith("BUNDLE");
        if (!shulker && !bundle) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        if (shulker && meta instanceof BlockStateMeta blockStateMeta
                && blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox) {
            scanInventory(shulkerBox.getInventory(), where + " > shulker box", result);
        } else if (bundle && meta instanceof BundleMeta bundleMeta && bundleMeta.hasItems()) {
            for (ItemStack inner : bundleMeta.getItems()) {
                scanItem(inner, where + " > bundle", result);
            }
        }
    }

    private void recordMace(ItemStack mace, String where, MaceScanResult result) {
        if (MaceItemUtil.isValidMace(mace, maxMaces)) {
            int number = MaceItemUtil.getMaceNumber(mace);
            boolean enchanted = number <= enchantableMaces && !mace.getEnchantments().isEmpty();

            result.totalValidMaces++;
            result.maceNumbers.add(number);
            if (enchanted) {
                result.enchantedMaces.add(number);
            }
            result.locations.add(where + " &e#" + number + (enchanted ? " &d(enchanted)" : ""));
        } else {
            result.invalidMaces++;
            result.locations.add(where + " &c(invalid)");
        }
    }

    private String describe(String typeName, Location location) {
        return prettify(typeName) + " at " + formatLocation(location);
    }

    private String formatLocation(Location location) {
        String world = location.getWorld() != null ? location.getWorld().getName() : "?";
        return world + " " + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    private String prettify(String enumName) {
        String spaced = enumName.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }
}