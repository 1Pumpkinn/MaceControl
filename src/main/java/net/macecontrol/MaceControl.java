package net.macecontrol;

import net.macecontrol.managers.PluginDataManager;
import net.macecontrol.utils.MaceUtils;
import net.macecontrol.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        ItemStack result = event.getInventory().getResult();

        // Only process if it's a mace
        if (!MaceUtils.isMace(result)) return;

        int maxMaces = plugin.getMaxMaces();
        int enchantableMaces = plugin.getEnchantableMaces();
        int currentMaceCount = dataManager.getTotalMacesCrafted();

        // If we've reached the limit, block crafting completely
        if (currentMaceCount >= maxMaces) {
            event.getInventory().setResult(null);
            if (event.getView().getPlayer() instanceof Player) {
                MessageUtils.sendLimitReached((Player) event.getView().getPlayer(), maxMaces, enchantableMaces, currentMaceCount);
            }
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

        // Prevent shift-clicking maces to ensure limit is strictly enforced
        if (event.isShiftClick()) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player) {
                MessageUtils.sendMessage((Player) event.getWhoClicked(), "&cShift-clicking maces is disabled to ensure crafting limits are accurate.");
            }
            return;
        }

        int maxMaces = plugin.getMaxMaces();
        int enchantableMaces = plugin.getEnchantableMaces();
        int currentCount = dataManager.getTotalMacesCrafted();

        // Double-check the limit before allowing the craft
        if (currentCount >= maxMaces) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player) {
                MessageUtils.sendLimitReached((Player) event.getWhoClicked(), maxMaces, enchantableMaces, currentCount);
            }
            return;
        }

        // Check if this mace has our number tag
        Integer maceNumber = MaceUtils.getMaceNumber(result);
        if (maceNumber != null) {
            // Final check before incrementing - prevent race conditions
            if (dataManager.getTotalMacesCrafted() >= maxMaces) {
                event.setCancelled(true);
                if (event.getWhoClicked() instanceof Player) {
                    Player player = (Player) event.getWhoClicked();
                    MessageUtils.sendMessage(player, "&cSomeone else just crafted the final mace! Crafting cancelled.");
                }
                return;
            }

            // Increment the counter when a numbered mace is actually crafted
            dataManager.incrementTotalMaces();

            Player player = (Player) event.getWhoClicked();
            boolean canEnchant = maceNumber <= enchantableMaces;

            String enchantableText = canEnchant ? " &e(Enchantable)" : " &7(Cannot be enchanted)";
            MessageUtils.sendMessage(player, "&6You have crafted Mace #" + maceNumber + enchantableText + "! &e(" + dataManager.getTotalMacesCrafted() + "/" + maxMaces + ")");

            // Announce to server
            String announcement = "&6Mace #" + maceNumber + " &ehas been crafted by &6" + player.getName() + "&e! &7(" + dataManager.getTotalMacesCrafted() + "/" + maxMaces + ")";
            if (canEnchant) {
                announcement += " &e(This mace can be enchanted!)";
            }
            MessageUtils.broadcastMessage(announcement);

            plugin.getLogger().info("Mace #" + maceNumber + " crafted by " + player.getName() + ". Total maces now: " + dataManager.getTotalMacesCrafted());

            // If this was the last mace, broadcast a special message
            if (dataManager.getTotalMacesCrafted() >= maxMaces) {
                MessageUtils.broadcastMessage("&c&lALL MACES HAVE BEEN CRAFTED! &7No more maces can be created on this server.");
            }
        } else {
            // This shouldn't happen with our prepare logic, but just in case
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player) {
                Player player = (Player) event.getWhoClicked();
                MessageUtils.sendMessage(player, "&cError: Invalid mace detected! Crafting cancelled.");
            }
        }
    }

    @EventHandler
    public void onPrepareItemEnchant(PrepareItemEnchantEvent event) {
        ItemStack item = event.getItem();
        int enchantableLimit = plugin.getEnchantableMaces();

        if (MaceUtils.isMace(item) && !MaceUtils.isEnchantable(item, enchantableLimit)) {
            event.setCancelled(true);
            MessageUtils.sendEnchantRestricted(event.getEnchanter(), enchantableLimit);
        }
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        ItemStack item = event.getItem();
        int enchantableLimit = plugin.getEnchantableMaces();

        if (MaceUtils.isMace(item) && !MaceUtils.isEnchantable(item, enchantableLimit)) {
            event.setCancelled(true);
            MessageUtils.sendEnchantRestricted(event.getEnchanter(), enchantableLimit);
        }
    }

    // Prevent non-enchantable maces from being used in anvils and block renaming
    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack firstItem = event.getInventory().getItem(0);
        ItemStack secondItem = event.getInventory().getItem(1);
        int enchantableLimit = plugin.getEnchantableMaces();

        // 1. Block any interaction with non-enchantable maces
        if (isRestrictedMace(firstItem, enchantableLimit) || isRestrictedMace(secondItem, enchantableLimit)) {
            event.setResult(null);

            if (event.getViewers().size() > 0 && event.getViewers().get(0) instanceof Player) {
                Player player = (Player) event.getViewers().get(0);
                MessageUtils.sendAnvilRestricted(player, enchantableLimit);
            }
            return;
        }

        // 2. Prevent renaming of ANY mace (enchantable or not)
         if (MaceUtils.isMace(firstItem)) {
             String renameText = event.getInventory().getRenameText();
             if (renameText != null && !renameText.isEmpty()) {
                 event.setResult(null); // Block the result if they try to rename it

                 if (event.getViewers().size() > 0 && event.getViewers().get(0) instanceof Player) {
                     Player player = (Player) event.getViewers().get(0);
                     MessageUtils.sendRenameRestricted(player);
                 }
             }
         }
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
            MessageUtils.sendMessages(player,
                "&7All " + maxMaces + " maces have been crafted on this server.",
                "&7Mace crafting is disabled. Current count: " + currentCount + "/" + maxMaces
            );
        } else {
            MessageUtils.sendMessages(player,
                "&7Maces available: " + (maxMaces - currentCount) + " remaining (" + currentCount + "/" + maxMaces + " crafted)",
                "&7" + enchantableMaces + " can be enchanted, " + (maxMaces - enchantableMaces) + " will be normal."
            );
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
        int enchantableLimit = plugin.getEnchantableMaces();

        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            ItemStack clickedItem = event.getCurrentItem();
            ItemStack cursorItem = event.getCursor();
            InventoryType invType = event.getInventory().getType();

            // 1. Block putting non-enchantable maces in Anvils
            if (invType == InventoryType.ANVIL) {
                // Regular click into slots
                if ((event.getSlot() == 0 || event.getSlot() == 1) &&
                        (isRestrictedMace(clickedItem, enchantableLimit) || isRestrictedMace(cursorItem, enchantableLimit))) {
                    event.setCancelled(true);
                    MessageUtils.sendAnvilRestricted(player, enchantableLimit);
                    return;
                }
                // Shift click from inventory
                if (event.isShiftClick() && isRestrictedMace(clickedItem, enchantableLimit)) {
                    event.setCancelled(true);
                    MessageUtils.sendAnvilRestricted(player, enchantableLimit);
                    return;
                }
            }

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
        }

        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                removeInvalidMaces(player);
            }, 1L);
        }
    }

    private void removeInvalidMaces(Player player) {
        PlayerInventory inventory = player.getInventory();
        int maxMaces = plugin.getMaxMaces();
        int removedCount = 0;

        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && MaceUtils.isMace(item) && !MaceUtils.isValidMace(item, maxMaces)) {
                inventory.setItem(i, null);
                removedCount++;
            }
        }

        if (removedCount > 0) {
            int enchantableMaces = plugin.getEnchantableMaces();

            MessageUtils.sendMessages(player,
                "&c" + removedCount + " invalid mace(s) have been removed from your inventory!",
                "&eOnly maces numbered 1-" + maxMaces + " are allowed on this server.",
                "&7Maces #1-" + enchantableMaces + " can be enchanted, the rest cannot."
            );

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