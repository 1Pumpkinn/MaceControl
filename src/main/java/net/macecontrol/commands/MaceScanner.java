package net.macecontrol.commands;

import net.macecontrol.util.MaceItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Barrel;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Walks players' inventories, ender chests and loaded-chunk containers to
 * find every mace on the server. Used by {@link MaceFindCommand}.
 */
public class MaceScanner {

    private final int maxMaces;
    private final int enchantableMaces;

    public MaceScanner(int maxMaces, int enchantableMaces) {
        this.maxMaces = maxMaces;
        this.enchantableMaces = enchantableMaces;
    }

    public MaceScanResult scanPlayer(Player player) {
        MaceScanResult result = new MaceScanResult();
        scanInventory(player.getInventory(), result);
        scanInventory(player.getEnderChest(), result);
        scanShulkerBoxesIn(player.getInventory(), result);
        return result;
    }

    public MaceScanResult scanEnderChestOf(OfflinePlayer offlinePlayer) {
        MaceScanResult result = new MaceScanResult();
        Player online = offlinePlayer.getPlayer();
        if (online != null) {
            scanInventory(online.getEnderChest(), result);
        }
        return result;
    }

    /** Scans every chest, barrel and shulker box in currently loaded chunks. */
    public MaceScanResult scanLoadedWorldContainers() {
        MaceScanResult result = new MaceScanResult();

        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (BlockState blockState : chunk.getTileEntities()) {
                    if (blockState instanceof Chest chest) {
                        scanInventory(chest.getInventory(), result);
                    } else if (blockState instanceof ShulkerBox shulkerBox) {
                        scanInventory(shulkerBox.getInventory(), result);
                    } else if (blockState instanceof Barrel barrel) {
                        scanInventory(barrel.getInventory(), result);
                    }
                }
            }
        }
        return result;
    }

    private void scanInventory(Inventory inventory, MaceScanResult result) {
        for (ItemStack item : inventory.getContents()) {
            if (!MaceItemUtil.isMace(item)) continue;

            Integer number = MaceItemUtil.getMaceNumber(item);
            if (number != null && number >= 1 && number <= maxMaces) {
                result.totalValidMaces++;
                result.maceNumbers.add(number);
                if (number <= enchantableMaces && !item.getEnchantments().isEmpty()) {
                    result.enchantedMaces.add(number);
                }
            } else {
                result.invalidMaces++;
            }
        }
    }

    private void scanShulkerBoxesIn(Inventory inventory, MaceScanResult result) {
        for (ItemStack item : inventory.getContents()) {
            if (item == null || !isShulkerBox(item.getType())) continue;

            ItemMeta meta = item.getItemMeta();
            if (!(meta instanceof BlockStateMeta blockStateMeta)) continue;
            if (!(blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox)) continue;

            scanInventory(shulkerBox.getInventory(), result);
        }
    }

    private boolean isShulkerBox(Material material) {
        return material.name().endsWith("SHULKER_BOX");
    }
}