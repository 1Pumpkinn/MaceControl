package net.macecontrol.cleaning;

import net.macecontrol.config.MaceConfig;
import net.macecontrol.util.MaceItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
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
 * Recursively strips invalid maces (untagged, or tagged with a number outside
 * the current max-maces limit) from anywhere they could be hiding:
 * <ul>
 *   <li>Player inventories and ender chests (online players only - see below)</li>
 *   <li>Every block in loaded chunks that holds an inventory: chests, barrels,
 *       hoppers, droppers, dispensers, shulker boxes, decorated pots, chiseled
 *       bookshelves/shelves, brewing stands, furnaces, crafters, etc. - anything
 *       that implements {@link InventoryHolder} is covered generically</li>
 *   <li>Shulker boxes and bundles sitting as *items* inside any of the above,
 *       recursively (a bundle inside a shulker box inside a chest is still found)</li>
 *   <li>Dropped items sitting on the ground in loaded chunks</li>
 * </ul>
 * <p>
 * <b>Offline players:</b> vanilla Bukkit has no supported API for reading or
 * writing another player's inventory/ender chest while they're offline (doing
 * so safely requires parsing their raw player-data NBT file, which is outside
 * what this plugin does). Instead, every offline player is guaranteed to be
 * swept the moment they rejoin - see {@code MaceInventoryGuardListener}'s join
 * hook, which calls {@link #cleanPlayer(Player)}.
 */
public class MaceCleaner {

    private final MaceConfig config;

    public MaceCleaner(MaceConfig config) {
        this.config = config;
    }

    /** Cleans one online player's main inventory and ender chest. Returns items removed. */
    public int cleanPlayer(Player player) {
        return cleanInventory(player.getInventory()) + cleanInventory(player.getEnderChest());
    }

    /** Cleans every inventory-holding block in every currently loaded chunk. Returns items removed. */
    public int cleanLoadedWorldContainers() {
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (BlockState blockState : chunk.getTileEntities()) {
                    if (blockState instanceof InventoryHolder holder) {
                        removed += cleanInventory(holder.getInventory());
                    }
                }
            }
        }
        return removed;
    }

    /** Cleans dropped item stacks lying on the ground in every currently loaded chunk. Returns items removed. */
    public int cleanGroundItems() {
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntitiesByClass(Item.class)) {
                Item itemEntity = (Item) entity;
                ItemStack stack = itemEntity.getItemStack();

                if (isInvalidMace(stack)) {
                    itemEntity.remove();
                    removed++;
                } else if (cleanContainerItem(stack)) {
                    itemEntity.setItemStack(stack);
                    removed++;
                }
            }
        }
        return removed;
    }

    /** Cleans every online player, every loaded-chunk container, and every dropped item. Returns items removed. */
    public int cleanEverythingLoaded() {
        int removed = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            removed += cleanPlayer(player);
        }
        removed += cleanLoadedWorldContainers();
        removed += cleanGroundItems();
        return removed;
    }

    // ===== Core recursive logic =====

    /** Removes invalid maces directly in this inventory's slots, and cleans any shulker/bundle items found. */
    private int cleanInventory(Inventory inventory) {
        int removed = 0;
        ItemStack[] contents = inventory.getContents();

        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item == null) continue;

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

    /**
     * If this item stack is itself a shulker box or a bundle, recursively cleans
     * the items stored inside it (mutating {@code item} in place). Returns true
     * if anything was removed from inside it.
     */
    private boolean cleanContainerItem(ItemStack item) {
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