package net.macecontrol;

import net.macecontrol.utils.MaceUtils;
import net.macecontrol.utils.MessageUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class HeavyCoreInteractions implements Listener {

    private final Main plugin;

    public HeavyCoreInteractions(Main plugin) {
        this.plugin = plugin;
    }

    private boolean isRestrictedContainer(InventoryType type) {
        return type == InventoryType.CHEST ||
                type == InventoryType.BARREL ||
                type == InventoryType.HOPPER ||
                type == InventoryType.DROPPER ||
                type == InventoryType.DISPENSER ||
                type == InventoryType.SHULKER_BOX ||
                type == InventoryType.BLAST_FURNACE ||
                type == InventoryType.SMOKER ||
                type == InventoryType.CREATIVE ||
                type == InventoryType.PLAYER ||
                type == InventoryType.ANVIL ||
                type == InventoryType.CRAFTER;

    }

    // Prevent interactions with heavy cores in restricted containers
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory clickedInventory = event.getClickedInventory();
        Inventory topInventory = event.getView().getTopInventory();
        
        if (clickedInventory == null || topInventory == null) return;
        if (!isRestrictedContainer(topInventory.getType())) return;

        boolean isHeavyCoreInvolved = false;

        // Check if current item (clicked slot) is a heavy core
        if (MaceUtils.isHeavyCore(event.getCurrentItem())) {
            isHeavyCoreInvolved = true;
        }

        // Check if cursor (held item) is a heavy core
        if (MaceUtils.isHeavyCore(event.getCursor())) {
            isHeavyCoreInvolved = true;
        }

        // Check hotbar swap (number keys)
        if (event.getClick() == ClickType.NUMBER_KEY) {
            int hotbarSlot = event.getHotbarButton();
            if (hotbarSlot >= 0 && hotbarSlot < 9) {
                ItemStack hotbarItem = event.getWhoClicked().getInventory().getItem(hotbarSlot);
                if (MaceUtils.isHeavyCore(hotbarItem)) {
                    isHeavyCoreInvolved = true;
                }
            }
        }

        // Check swap offhand key (F)
        if (event.getClick() == ClickType.SWAP_OFFHAND) {
            ItemStack offhandItem = event.getWhoClicked().getInventory().getItemInOffHand();
            if (MaceUtils.isHeavyCore(offhandItem)) {
                isHeavyCoreInvolved = true;
            }
        }

        // If no heavy core is involved in the transaction, we don't care
        if (!isHeavyCoreInvolved) return;

        boolean shouldCancel = false;

        // 1. Interaction specifically INSIDE the restricted container
        // (Putting in, Taking out, Swapping, Hotkeying in/out)
        if (clickedInventory.equals(topInventory)) {
            shouldCancel = true;
        }

        // 2. Shift-clicking FROM player inventory INTO restricted container
        if (event.isShiftClick() && !clickedInventory.equals(topInventory)) {
            shouldCancel = true;
        }

        // 3. Double-clicking (Collect to Cursor)
        // This is dangerous as it can pull items from the restricted container
        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            shouldCancel = true;
        }

        if (shouldCancel) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player) {
                Player player = (Player) event.getWhoClicked();
                MessageUtils.sendHeavyCoreRestricted(player);
                player.updateInventory();
            }
        }
    }

    // Prevent Hoppers (and hopper minecarts) from picking up dropped Heavy Cores
    @EventHandler
    public void onInventoryPickupItem(InventoryPickupItemEvent event) {
        if (MaceUtils.isHeavyCore(event.getItem().getItemStack())) {
            event.setCancelled(true);
        }
    }

    // Prevent dragging heavy cores into containers
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (MaceUtils.isHeavyCore(event.getOldCursor()) && isRestrictedContainer(event.getInventory().getType())) {
            event.setCancelled(true);
        }
    }

    // Prevent hoppers or droppers moving heavy cores
    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (MaceUtils.isHeavyCore(event.getItem())) {
            event.setCancelled(true);
        }
    }

    // Prevent right-clicking decorated pots with heavy core
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (MaceUtils.isHeavyCore(event.getItem())) {
            Block block = event.getClickedBlock();
            if (block != null && block.getType() == Material.DECORATED_POT && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                MessageUtils.sendHeavyCoreRestricted(event.getPlayer());
            }
        }
    }

    // Prevent placing heavy cores in item frames
    @EventHandler
    public void onItemFramePlace(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() != null && event.getRightClicked().getType().toString().contains("ITEM_FRAME")) {
            if (MaceUtils.isHeavyCore(event.getPlayer().getInventory().getItemInMainHand()) ||
                    MaceUtils.isHeavyCore(event.getPlayer().getInventory().getItemInOffHand())) {
                event.setCancelled(true);
                MessageUtils.sendHeavyCoreRestricted(event.getPlayer());
            }
        }
    }
}