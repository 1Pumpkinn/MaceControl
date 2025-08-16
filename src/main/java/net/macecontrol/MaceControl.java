package net.macecontrol;

import net.macecontrol.Main;
import net.macecontrol.PluginDataManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MaceControl implements Listener {

    private final Main plugin;
    private final PluginDataManager dataManager;
    private final NamespacedKey maceNumberKey;
    private final int maxMaces = 3; // server-wide cap

    public MaceControl(Main plugin, PluginDataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.maceNumberKey = new NamespacedKey(plugin, "mace_number");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        ItemStack result = event.getInventory().getResult();

        // Only process if it's a mace
        if (result == null || result.getType() != Material.MACE) return;

        // Check if server already has 3 maces - fixed logic
        int currentMaceCount = dataManager.getTotalMacesCrafted();

        // Debug message for testing (remove in production)
        if (event.getView().getPlayer() instanceof Player) {
            Player player = (Player) event.getView().getPlayer();
            plugin.getLogger().info("Player " + player.getName() + " is trying to craft a mace. Current count: " + currentMaceCount);
        }

        if (currentMaceCount >= maxMaces) {
            event.getInventory().setResult(null);
            if (event.getView().getPlayer() instanceof Player) {
                Player player = (Player) event.getView().getPlayer();
                player.sendMessage("§cAll " + maxMaces + " maces have already been crafted on this server!");
            }
            return;
        }

        // Add persistent data to mace - only if we can still craft one
        ItemStack mace = result.clone();
        ItemMeta meta = mace.getItemMeta();
        if (meta != null) {
            int maceNumber = currentMaceCount + 1;
            meta.getPersistentDataContainer().set(maceNumberKey, PersistentDataType.INTEGER, maceNumber);
            meta.setDisplayName("§6Mace #" + maceNumber);
            mace.setItemMeta(meta);
            event.getInventory().setResult(mace);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCraftItem(CraftItemEvent event) {
        ItemStack result = event.getCurrentItem();

        if (result == null || result.getType() != Material.MACE) return;

        // Double-check the limit before allowing the craft
        if (dataManager.getTotalMacesCrafted() >= maxMaces) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player) {
                Player player = (Player) event.getWhoClicked();
                player.sendMessage("§cAll " + maxMaces + " maces have already been crafted on this server!");
            }
            return;
        }

        // Check if this mace has our number tag
        ItemMeta meta = result.getItemMeta();
        if (meta != null && meta.getPersistentDataContainer().has(maceNumberKey, PersistentDataType.INTEGER)) {
            // Increment the counter when a numbered mace is actually crafted
            dataManager.incrementTotalMaces();

            Player player = (Player) event.getWhoClicked();
            Integer maceNumber = meta.getPersistentDataContainer().get(maceNumberKey, PersistentDataType.INTEGER);
            player.sendMessage("§6You have crafted Mace #" + maceNumber + "! §e(" + dataManager.getTotalMacesCrafted() + "/" + maxMaces + ")");

            // Announce to server
            Bukkit.broadcastMessage("§6Mace #" + maceNumber + " §ehas been crafted by §6" + player.getName() + "§e! §7(" + dataManager.getTotalMacesCrafted() + "/" + maxMaces + ")");

            // Log for debugging (remove in production)
            plugin.getLogger().info("Mace #" + maceNumber + " crafted by " + player.getName() + ". Total maces now: " + dataManager.getTotalMacesCrafted());
        } else {
            // This shouldn't happen with our prepare logic, but just in case
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player) {
                Player player = (Player) event.getWhoClicked();
                player.sendMessage("§cError: Invalid mace detected! Crafting cancelled.");
            }
        }
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        ItemStack item = event.getItem();

        if (item.getType() != Material.MACE) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        Integer maceNumber = meta.getPersistentDataContainer().get(maceNumberKey, PersistentDataType.INTEGER);
        if (maceNumber == null || maceNumber != 1) {
            event.setCancelled(true);
            event.getEnchanter().sendMessage("§cOnly Mace #1 can be enchanted!");
        }
    }

    // Check players on join and remove invalid maces
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Run check after a short delay to ensure player is fully loaded
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            removeInvalidMaces(player);
        }, 20L); // 1 second delay
    }

    // Also check when players interact with inventories
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();

            // Check on next tick to avoid issues with the event
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                removeInvalidMaces(player);
            }, 1L);
        }
    }

    private void removeInvalidMaces(Player player) {
        PlayerInventory inventory = player.getInventory();
        boolean removedAny = false;

        // Check main inventory
        for (int i = 0; i < inventory.getContents().length; i++) {
            ItemStack item = inventory.getContents()[i];
            if (shouldRemoveMace(item)) {
                inventory.setItem(i, null);
                removedAny = true;
            }
        }

        // Check armor slots
        if (shouldRemoveMace(inventory.getHelmet())) {
            inventory.setHelmet(null);
            removedAny = true;
        }
        if (shouldRemoveMace(inventory.getChestplate())) {
            inventory.setChestplate(null);
            removedAny = true;
        }
        if (shouldRemoveMace(inventory.getLeggings())) {
            inventory.setLeggings(null);
            removedAny = true;
        }
        if (shouldRemoveMace(inventory.getBoots())) {
            inventory.setBoots(null);
            removedAny = true;
        }

        // Check offhand
        if (shouldRemoveMace(inventory.getItemInOffHand())) {
            inventory.setItemInOffHand(null);
            removedAny = true;
        }

        if (removedAny) {
            player.sendMessage("§cInvalid maces have been removed from your inventory!");
            player.sendMessage("§eOnly maces numbered 1-3 are allowed on this server.");
        }
    }

    private boolean shouldRemoveMace(ItemStack item) {
        if (item == null || item.getType() != Material.MACE) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            // Mace without metadata should be removed
            return true;
        }

        Integer maceNumber = meta.getPersistentDataContainer().get(maceNumberKey, PersistentDataType.INTEGER);

        // Remove if no number or invalid number
        return maceNumber == null || maceNumber < 1 || maceNumber > 3;
    }

    // Method to scan and clean all online players (can be called from commands)
    public void cleanAllOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeInvalidMaces(player);
        }
    }
}