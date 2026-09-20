package net.macecontrol.listeners;

import net.macecontrol.util.MaceItemUtil;
import net.macecontrol.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Keeps Heavy Cores out of storage containers, hoppers, item frames and
 * decorated pots so they can't be stashed or auto-collected.
 */
public class HeavyCoreListener implements Listener {

    private static final java.util.Set<InventoryType> RESTRICTED_CONTAINERS = java.util.EnumSet.of(
            InventoryType.CHEST, InventoryType.BARREL, InventoryType.HOPPER,
            InventoryType.DROPPER, InventoryType.DISPENSER, InventoryType.SHULKER_BOX,
            InventoryType.BLAST_FURNACE, InventoryType.SMOKER, InventoryType.CREATIVE,
            InventoryType.PLAYER, InventoryType.ANVIL, InventoryType.CRAFTER
    );

    private boolean isRestrictedContainer(InventoryType type) {
        return RESTRICTED_CONTAINERS.contains(type);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory clickedInventory = event.getClickedInventory();
        Inventory topInventory = event.getView().getTopInventory();

        if (clickedInventory == null || !isRestrictedContainer(topInventory.getType())) return;
        if (!isHeavyCoreInvolved(event)) return;

        boolean shouldCancel =
                // Interacting directly inside the restricted container (put/take/swap/hotkey).
                clickedInventory.equals(topInventory)
                        // Shift-clicking a heavy core from the player's inventory into it.
                        || (event.isShiftClick() && !clickedInventory.equals(topInventory))
                        // Double-click "collect to cursor" can pull a heavy core out of it.
                        || event.getAction() == InventoryAction.COLLECT_TO_CURSOR;

        if (shouldCancel) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                MessageUtil.sendHeavyCoreRestricted(player);
                player.updateInventory();
            }
        }
    }

    private boolean isHeavyCoreInvolved(InventoryClickEvent event) {
        if (MaceItemUtil.isHeavyCore(event.getCurrentItem())) return true;
        if (MaceItemUtil.isHeavyCore(event.getCursor())) return true;

        if (event.getClick() == ClickType.NUMBER_KEY) {
            int hotbarSlot = event.getHotbarButton();
            if (hotbarSlot >= 0 && hotbarSlot < 9) {
                ItemStack hotbarItem = event.getWhoClicked().getInventory().getItem(hotbarSlot);
                if (MaceItemUtil.isHeavyCore(hotbarItem)) return true;
            }
        }

        if (event.getClick() == ClickType.SWAP_OFFHAND) {
            ItemStack offhandItem = event.getWhoClicked().getInventory().getItemInOffHand();
            if (MaceItemUtil.isHeavyCore(offhandItem)) return true;
        }

        return false;
    }

    /** Hoppers (and hopper minecarts) may not vacuum up a dropped Heavy Core. */
    @EventHandler
    public void onInventoryPickupItem(InventoryPickupItemEvent event) {
        if (MaceItemUtil.isHeavyCore(event.getItem().getItemStack())) {
            event.setCancelled(true);
        }
    }

    /** Blocks dragging a Heavy Core into a restricted container. */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (MaceItemUtil.isHeavyCore(event.getOldCursor()) && isRestrictedContainer(event.getInventory().getType())) {
            event.setCancelled(true);
        }
    }

    /** Blocks hoppers/droppers from moving a Heavy Core between containers. */
    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (MaceItemUtil.isHeavyCore(event.getItem())) {
            event.setCancelled(true);
        }
    }

    /** Blocks right-clicking a decorated pot while holding a Heavy Core. */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!MaceItemUtil.isHeavyCore(event.getItem())) return;

        Block block = event.getClickedBlock();
        if (block != null && block.getType() == Material.DECORATED_POT && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            MessageUtil.sendHeavyCoreRestricted(event.getPlayer());
        }
    }

    /** Blocks placing a Heavy Core into an item frame. */
    @EventHandler
    public void onItemFramePlace(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() == null || !event.getRightClicked().getType().toString().contains("ITEM_FRAME")) return;

        Player player = event.getPlayer();
        if (MaceItemUtil.isHeavyCore(player.getInventory().getItemInMainHand())
                || MaceItemUtil.isHeavyCore(player.getInventory().getItemInOffHand())) {
            event.setCancelled(true);
            MessageUtil.sendHeavyCoreRestricted(player);
        }
    }
}