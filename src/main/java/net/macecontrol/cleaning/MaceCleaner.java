package net.macecontrol.cleaning;

import net.macecontrol.config.MaceConfig;
import net.macecontrol.util.MaceItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
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
import java.util.Iterator;
import java.util.List;

/**
 * Recursively strips invalid maces from anywhere they could be hiding. A mace is
 * invalid when it is untagged, its number is outside the current max-maces limit,
 * or it is from an older generation (i.e. it survived a {@code /macecontrol clean}).
 * <ul>
 *   <li>Online players: inventory, armor, offhand, ender chest and cursor item</li>
 *   <li>Every block in loaded chunks that holds an inventory (chests, barrels, hoppers,
 *       droppers, dispensers, shulker boxes, decorated pots, chiseled bookshelves,
 *       furnaces, crafters, ...) - anything that is an {@link InventoryHolder}</li>
 *   <li>Entities: dropped items, item frames, item displays, armor stands, mob equipment,
 *       and entity inventories (chest/hopper minecarts, chest boats, donkeys, llamas, villagers, ...)</li>
 *   <li>Shulker boxes and bundles sitting as items inside any of the above, recursively</li>
 * </ul>
 * <p>
 * <b>Offline players / unloaded chunks:</b> these cannot be touched through the Bukkit API,
 * so they are swept the moment they are seen again - see {@code MaceInventoryGuardListener}
 * (join, chunk load and entity load hooks).
 */
public class MaceCleaner {

    private final MaceConfig config;

    public MaceCleaner(MaceConfig config) {
        this.config = config;
    }

    // ===== Public entry points =====

    /** Cleans one online player's inventory, armor, offhand, ender chest and cursor. Returns items removed. */
    public int cleanPlayer(Player player) {
        int removed = cleanInventory(player.getInventory()) + cleanInventory(player.getEnderChest());

        ItemStack cursor = player.getItemOnCursor();
        if (cursor != null && !cursor.getType().isAir()) {
            if (isInvalidMace(cursor)) {
                player.setItemOnCursor(null);
                removed++;
            } else if (cleanContainerItem(cursor)) {
                player.setItemOnCursor(cursor);
                removed++;
            }
        }
        return removed;
    }

    /** Cleans every inventory-holding block in one chunk. Returns items removed. */
    public int cleanChunkContainers(Chunk chunk) {
        int removed = 0;
        for (BlockState blockState : chunk.getTileEntities(false)) {
            if (blockState instanceof InventoryHolder holder) {
                removed += cleanInventory(holder.getInventory());
            }
        }
        return removed;
    }

    /** Cleans every inventory-holding block in every currently loaded chunk. Returns items removed. */
    public int cleanLoadedWorldContainers() {
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                removed += cleanChunkContainers(chunk);
            }
        }
        return removed;
    }

    /** Cleans every non-player entity in every currently loaded chunk. Returns items removed. */
    public int cleanLoadedEntities() {
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : new ArrayList<>(world.getEntities())) {
                removed += cleanEntity(entity);
            }
        }
        return removed;
    }

    /** Cleans a single non-player entity (dropped item, item frame, armor stand, minecart chest, ...). */
    public int cleanEntity(Entity entity) {
        if (entity == null || !entity.isValid() || entity instanceof Player) return 0;

        int removed = 0;

        if (entity instanceof Item itemEntity) {
            ItemStack stack = itemEntity.getItemStack();
            if (isInvalidMace(stack)) {
                itemEntity.remove();
                return 1;
            }
            if (cleanContainerItem(stack)) {
                itemEntity.setItemStack(stack);
                removed++;
            }
        } else if (entity instanceof ItemFrame frame) {
            ItemStack stack = frame.getItem();
            if (isInvalidMace(stack)) {
                frame.setItem(null);
                removed++;
            } else if (cleanContainerItem(stack)) {
                frame.setItem(stack);
                removed++;
            }
        } else if (entity instanceof ItemDisplay display) {
            ItemStack stack = display.getItemStack();
            if (isInvalidMace(stack)) {
                display.setItemStack(null);
                removed++;
            } else if (cleanContainerItem(stack)) {
                display.setItemStack(stack);
                removed++;
            }
        }

        // Minecart chests, hopper minecarts, chest boats, donkeys/mules/llamas, villagers, allays, ...
        if (entity instanceof InventoryHolder holder) {
            removed += cleanInventory(holder.getInventory());
        }

        // Armor stands and anything a mob is holding or wearing.
        if (entity instanceof LivingEntity living) {
            removed += cleanEquipment(living.getEquipment());
        }

        return removed;
    }

    /** Cleans every online player, every loaded-chunk container, and every loaded entity. Returns items removed. */
    public int cleanEverythingLoaded() {
        int removed = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            removed += cleanPlayer(player);
        }
        removed += cleanLoadedWorldContainers();
        removed += cleanLoadedEntities();
        return removed;
    }

    // ===== Core recursive logic =====

    /** Removes invalid maces directly in this inventory's slots, and cleans any shulker/bundle items found. */
    private int cleanInventory(Inventory inventory) {
        if (inventory == null) return 0;

        int removed = 0;
        ItemStack[] contents = inventory.getContents();

        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item == null || item.getType().isAir()) continue;

            if (isInvalidMace(item)) {
                inventory.setItem(slot, null);
                removed++;
            } else if (cleanContainerItem(item)) {
                inventory.setItem(slot, item);
                removed++;
            }
        }
        return removed;
    }

    private int cleanEquipment(EntityEquipment equipment) {
        if (equipment == null) return 0;

        int removed = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            try {
                ItemStack item = equipment.getItem(slot);
                if (item == null || item.getType().isAir()) continue;

                if (isInvalidMace(item)) {
                    equipment.setItem(slot, null);
                    removed++;
                } else if (cleanContainerItem(item)) {
                    equipment.setItem(slot, item);
                    removed++;
                }
            } catch (IllegalArgumentException | UnsupportedOperationException ignored) {
                // This entity type doesn't support this equipment slot.
            }
        }
        return removed;
    }

    /**
     * If this item stack is itself a shulker box or a bundle, recursively cleans
     * the items stored inside it (mutating {@code item} in place). Returns true
     * if anything was removed from inside it.
     */
    private boolean cleanContainerItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;

        // Cheap type check first so we don't clone item meta for every ordinary item.
        String typeName = item.getType().name();
        if (!typeName.endsWith("SHULKER_BOX") && !typeName.endsWith("BUNDLE")) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        if (meta instanceof BlockStateMeta blockStateMeta && blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox) {
            int removed = cleanInventory(shulkerBox.getInventory());
            if (removed > 0) {
                blockStateMeta.setBlockState(shulkerBox);
                item.setItemMeta(blockStateMeta);
                return true;
            }
            return false;
        }

        if (meta instanceof BundleMeta bundleMeta && bundleMeta.hasItems()) {
            List<ItemStack> items = new ArrayList<>(bundleMeta.getItems());
            boolean changed = false;

            Iterator<ItemStack> iterator = items.iterator();
            while (iterator.hasNext()) {
                ItemStack inner = iterator.next();
                if (isInvalidMace(inner)) {
                    iterator.remove();
                    changed = true;
                } else if (cleanContainerItem(inner)) {
                    changed = true;
                }
            }

            if (changed) {
                bundleMeta.setItems(items);
                item.setItemMeta(bundleMeta);
                return true;
            }
        }

        return false;
    }

    private boolean isInvalidMace(ItemStack item) {
        return MaceItemUtil.isMace(item) && !MaceItemUtil.isValidMace(item, config.getMaxMaces());
    }
}