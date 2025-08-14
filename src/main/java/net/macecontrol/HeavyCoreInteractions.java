package net.macecontrol;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class HeavyCoreInteractions implements Listener {

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

    private boolean isHeavyCore(ItemStack item) {
        return item != null && item.getType() == Material.HEAVY_CORE;
    }

    // Prevent putting heavy cores in restricted containers
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (isHeavyCore(event.getCurrentItem()) || isHeavyCore(event.getCursor())) {
            if (isRestrictedContainer(event.getInventory().getType())) {
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player) {
                    ((Player) event.getWhoClicked()).sendMessage("§cYou cannot place Heavy Cores in containers!");
                }
            }
        }
    }

    // Prevent dragging heavy cores into containers
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (isHeavyCore(event.getOldCursor()) && isRestrictedContainer(event.getInventory().getType())) {
            event.setCancelled(true);
        }
    }

    // Prevent hoppers or droppers moving heavy cores
    @EventHandler
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (isHeavyCore(event.getItem())) {
            event.setCancelled(true);
        }
    }

    // Prevent right-clicking decorated pots with heavy core
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getItem() != null && isHeavyCore(event.getItem())) {
            Block block = event.getClickedBlock();
            if (block != null && block.getType() == Material.DECORATED_POT && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§cYou cannot put Heavy Cores in decorated pots!");
            }
        }
    }

    // Prevent placing heavy cores in item frames
    @EventHandler
    public void onItemFramePlace(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() != null && event.getRightClicked().getType().toString().contains("ITEM_FRAME")) {
            if (isHeavyCore(event.getPlayer().getInventory().getItemInMainHand()) ||
                    isHeavyCore(event.getPlayer().getInventory().getItemInOffHand())) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§cYou cannot put Heavy Cores in item frames!");
            }
        }
    }

    // Prevent dropping heavy cores
    @EventHandler
    public void onPlayerDrop(PlayerDropItemEvent event) {
        if (isHeavyCore(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cYou cannot drop Heavy Cores!");
        }
    }

    // Prevent Q / Ctrl+Q inside inventories
    @EventHandler
    public void onInventoryDropClick(InventoryClickEvent event) {
        if ((event.getClick() == ClickType.DROP || event.getClick() == ClickType.CONTROL_DROP) &&
                isHeavyCore(event.getCurrentItem())) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player) {
                Player player = (Player) event.getWhoClicked();
                player.sendMessage("§cYou cannot drop Heavy Cores!");
                player.updateInventory();
            }
        }
    }
}
