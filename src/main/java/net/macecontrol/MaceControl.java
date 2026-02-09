package net.macecontrol;

import net.macecontrol.managers.PluginDataManager;
import net.macecontrol.utils.MaceUtils;
import net.macecontrol.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class MaceControl implements Listener {

    private final Main plugin;
    private final PluginDataManager dataManager;

    public MaceControl(Main plugin, PluginDataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    // Helper to check if crafting is blocked
    private boolean isCraftingBlocked(int currentCount, int maxMaces) {
        return plugin.isMaceBanned() || currentCount >= maxMaces;
    }

    // Helper to send appropriate block message
    private void sendBlockMessage(Player player, int currentCount, int maxMaces, int enchantableMaces) {
        if (plugin.isMaceBanned()) {
            MessageUtils.sendMessage(player, "&cMace crafting is currently disabled.");
        } else {
            MessageUtils.sendLimitReached(player, maxMaces, enchantableMaces, currentCount);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        ItemStack result = event.getInventory().getResult();

        // Only process if it's a mace
        if (!MaceUtils.isMace(result)) return;

        int maxMaces = plugin.getMaxMaces();
        int enchantableMaces = plugin.getEnchantableMaces();
        int currentMaceCount = dataManager.getTotalMacesCrafted();

        // If we've reached the limit or explicitly banned, block crafting completely
        if (isCraftingBlocked(currentMaceCount, maxMaces)) {
            event.getInventory().setResult(null);
            return;
        }

        // Add persistent data to mace
        ItemStack mace = result.clone();
        ItemMeta meta = mace.getItemMeta();
        if (meta != null) {
            int maceNumber = currentMaceCount + 1;
            meta.getPersistentDataContainer().set(MaceUtils.getMaceNumberKey(), PersistentDataType.INTEGER, maceNumber);

            // Determine if this mace can be enchanted
            boolean canEnchant = maceNumber <= enchantableMaces;

            if (canEnchant) {
                meta.setDisplayName("§6Mace #" + maceNumber + " §e(Enchantable)");
                meta.setLore(List.of("§7This mace can be enchanted", "§7Mace " + maceNumber + " of " + maxMaces));
            } else {
                meta.setDisplayName("§6Mace #" + maceNumber + " §7(Normal)");
                meta.setLore(List.of("§7This mace cannot be enchanted", "§7Mace " + maceNumber + " of " + maxMaces));
            }

            mace.setItemMeta(meta);
            event.getInventory().setResult(mace);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCraftItem(CraftItemEvent event) {
        ItemStack result = event.getCurrentItem();
        if (!MaceUtils.isMace(result)) return;

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        int maxMaces = plugin.getMaxMaces();
        int enchantableMaces = plugin.getEnchantableMaces();

        if (isCraftingBlocked(dataManager.getTotalMacesCrafted(), maxMaces)) {
            event.setCancelled(true);
            sendBlockMessage(player, dataManager.getTotalMacesCrafted(), maxMaces, enchantableMaces);
            player.updateInventory();
            return;
        }

        if (event.isShiftClick()) {
            event.setCancelled(true);
            MessageUtils.sendMessage(player, "&cShift-clicking to craft maces is disabled.");
            player.updateInventory();
            return;
        }

        Integer maceNumber = MaceUtils.getMaceNumber(result);
        if (maceNumber != null) {
            dataManager.incrementTotalMaces();
            MessageUtils.sendMaceCrafted(player, maceNumber, maxMaces, enchantableMaces, dataManager.getTotalMacesCrafted(), player.getName());
            MessageUtils.broadcastMaceCrafted(maceNumber, maxMaces, enchantableMaces, dataManager.getTotalMacesCrafted(), player.getName());
            plugin.getLogger().info("Mace #" + maceNumber + " crafted by " + player.getName() + ". Total maces now: " + dataManager.getTotalMacesCrafted());

            if (dataManager.getTotalMacesCrafted() >= maxMaces) {
                MessageUtils.broadcastAllMacesCrafted();
            }
        } else {
            event.setCancelled(true);
            MessageUtils.sendMessage(player, "&cError: Invalid mace detected! Crafting cancelled.");
        }
    }

    @EventHandler
    public void onPrepareItemEnchant(PrepareItemEnchantEvent event) {
        handleEnchantEvent(event.getItem(), event.getEnchanter(), event);
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        handleEnchantEvent(event.getItem(), event.getEnchanter(), event);
    }

    private void handleEnchantEvent(ItemStack item, Player enchanter, org.bukkit.event.Cancellable event) {
        int enchantableLimit = plugin.getEnchantableMaces();
        if (MaceUtils.isMace(item) && !MaceUtils.isEnchantable(item, enchantableLimit)) {
            event.setCancelled(true);
            MessageUtils.sendEnchantRestricted(enchanter, enchantableLimit);
        }
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack firstItem = event.getInventory().getItem(0);
        ItemStack secondItem = event.getInventory().getItem(1);
        int enchantableLimit = plugin.getEnchantableMaces();

        // If the first item is a restricted mace, check what the player is trying to do
        if (isRestrictedMace(firstItem, enchantableLimit)) {
            // Check if the second item is an enchantment book
            if (secondItem != null && secondItem.getType() == org.bukkit.Material.ENCHANTED_BOOK) {
                // Block the enchanting attempt
                event.setResult(null);

                if (event.getViewers().size() > 0 && event.getViewers().get(0) instanceof Player) {
                    Player player = (Player) event.getViewers().get(0);
                    MessageUtils.sendAnvilRestricted(player, enchantableLimit);
                }
                return;
            }
            // If it's not an enchantment book, it's either a repair or a rename, which is allowed.
            // The commented-out code below is what was previously blocking renaming.
        }

        // The old renaming block is now removed, allowing renaming for all maces.
        // The logic above handles the enchanting restriction.
    }

    private boolean isRestrictedMace(ItemStack item, int limit) {
        return MaceUtils.isMace(item) && !MaceUtils.isEnchantable(item, limit);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        int maxMaces = plugin.getMaxMaces();
        int enchantableMaces = plugin.getEnchantableMaces();
        int currentCount = dataManager.getTotalMacesCrafted();

        if (currentCount >= maxMaces) {
            MessageUtils.sendJoinAllCrafted(player, maxMaces, currentCount);
        } else {
            MessageUtils.sendJoinMacesAvailable(player, maxMaces, enchantableMaces, currentCount);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            removeInvalidMaces(player);
        }, 20L);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    removeInvalidMaces(player);
                } else {
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 100L, 200L);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();

        // Prevent crafting a mace if the player has an item on their cursor
        if (event.getSlotType() == InventoryType.SlotType.RESULT &&
            (event.getInventory().getType() == InventoryType.CRAFTING || event.getInventory().getType() == InventoryType.WORKBENCH)) {
            ItemStack result = event.getCurrentItem();
            if (result != null && MaceUtils.isMace(result)) {
                ItemStack cursor = event.getCursor();
                if (cursor != null && cursor.getType() != org.bukkit.Material.AIR) {
                    event.setCancelled(true);
                    MessageUtils.sendMessage(player, "&cPlease empty your cursor before crafting this item.");
                    player.updateInventory(); // Force a client-side update
                    return; // Stop processing this event further
                }
            }
        }

        int enchantableLimit = plugin.getEnchantableMaces();
        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();
        InventoryType invType = event.getInventory().getType();

        // 1. Block putting non-enchantable maces in Anvils
        // if (invType == InventoryType.ANVIL) {
        //     // Regular click into slots
        //     if ((event.getSlot() == 0 || event.getSlot() == 1) &&
        //             (isRestrictedMace(clickedItem, enchantableLimit) || isRestrictedMace(cursorItem, enchantableLimit))) {
        //         event.setCancelled(true);
        //         MessageUtils.sendAnvilRestricted(player, enchantableLimit);
        //         return;
        //     }
        //     // Shift click from inventory
        //     if (event.isShiftClick() && isRestrictedMace(clickedItem, enchantableLimit)) {
        //         event.setCancelled(true);
        //         MessageUtils.sendAnvilRestricted(player, enchantableLimit);
        //         return;
        //     }
        // }

        // 2. Block putting non-enchantable maces in Enchanting Tables
        if (invType == InventoryType.ENCHANTING) {
            // Regular click into slots
            if (event.getSlot() == 0 && (isRestrictedMace(clickedItem, enchantableLimit) || isRestrictedMace(cursorItem, enchantableLimit))) {
                event.setCancelled(true);
                MessageUtils.sendEnchantRestricted(player, enchantableLimit);
                return;
            }
            // Shift click from inventory
            if (event.isShiftClick() && isRestrictedMace(clickedItem, enchantableLimit)) {
                event.setCancelled(true);
                MessageUtils.sendEnchantRestricted(player, enchantableLimit);
                return;
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                removeInvalidMaces(player);
            }
        }, 1L);
    }

    private void removeInvalidMaces(Player player) {
        PlayerInventory inventory = player.getInventory();
        int maxMaces = plugin.getMaxMaces();
        int enchantableMaces = plugin.getEnchantableMaces();
        int removedCount = 0;

        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && MaceUtils.isMace(item) && !MaceUtils.isValidMace(item, maxMaces)) {
                inventory.setItem(i, null);
                removedCount++;
            }
        }

        if (removedCount > 0) {
            MessageUtils.sendInvalidMacesRemoved(player, removedCount, maxMaces);
            plugin.getLogger().info("Removed " + removedCount + " invalid maces from player " + player.getName());
        }
    }

    public void cleanAllOnlinePlayers() {
        int totalRemoved = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            int before = countPlayerMaces(player);
            removeInvalidMaces(player);
            int after = countPlayerMaces(player);
            totalRemoved += (before - after);
        }

        plugin.getLogger().info("Cleanup complete: Removed " + totalRemoved + " invalid maces from all online players");
    }

    private int countPlayerMaces(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (MaceUtils.isMace(item)) count++;
        }
        return count;
    }
}