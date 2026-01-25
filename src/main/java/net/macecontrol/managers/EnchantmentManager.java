package net.macecontrol.managers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class EnchantmentManager implements Listener {

    private static final List<Enchantment> BANNED_SWORD_ENCHANTS = Arrays.asList(
            Enchantment.FIRE_ASPECT
    );

    private static final List<Enchantment> BANNED_BOW_ENCHANTS = Arrays.asList(
            Enchantment.FLAME
    );

    // Prevent banned enchantments from appearing in enchanting table
    @EventHandler
    public void onPrepareItemEnchant(PrepareItemEnchantEvent event) {
        ItemStack item = event.getItem();
        List<Enchantment> bannedEnchants = getBannedEnchantsForItem(item);
        if (bannedEnchants.isEmpty()) return;

        Random random = new Random();
        for (int i = 0; i < event.getOffers().length; i++) {
            if (event.getOffers()[i] == null) continue;

            // Force reroll if banned
            int attempts = 0;
            while (bannedEnchants.contains(event.getOffers()[i].getEnchantment()) && attempts < 20) {
                Enchantment[] possible = Enchantment.values();
                Enchantment newEnchant;
                int newLevel;

                do {
                    newEnchant = possible[random.nextInt(possible.length)];
                    newLevel = random.nextInt(newEnchant.getMaxLevel()) + 1;
                    attempts++;
                } while (bannedEnchants.contains(newEnchant) || !newEnchant.canEnchantItem(item));

                if (attempts < 20) {
                    event.getOffers()[i].setEnchantment(newEnchant);
                    event.getOffers()[i].setEnchantmentLevel(newLevel);
                }
            }
        }
    }

    // Remove banned enchantments when enchanting and refund XP
    @EventHandler
    public void onEnchantItem(EnchantItemEvent event) {
        ItemStack item = event.getItem();
        List<Enchantment> bannedEnchants = getBannedEnchantsForItem(item);
        if (bannedEnchants.isEmpty()) return;

        // Check if any banned enchants are being added
        boolean hadBanned = false;
        for (Enchantment enchant : bannedEnchants) {
            if (event.getEnchantsToAdd().containsKey(enchant)) {
                event.getEnchantsToAdd().remove(enchant);
                hadBanned = true;
            }
        }

        if (hadBanned) {
            event.getEnchanter().giveExp(event.getExpLevelCost()); // Refund XP
            event.getEnchanter().sendMessage("§cBanned enchantments were removed! XP refunded.");
        }
    }

    // Remove banned enchantments from anvil results
    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack result = event.getResult();
        if (result == null) return;

        List<Enchantment> bannedEnchants = getBannedEnchantsForItem(result);
        if (bannedEnchants.isEmpty()) return;

        boolean hadBanned = false;
        ItemStack cleanedResult = result.clone();

        for (Enchantment banned : bannedEnchants) {
            if (cleanedResult.containsEnchantment(banned)) {
                cleanedResult.removeEnchantment(banned);
                hadBanned = true;
            }
        }

        if (hadBanned) {
            event.setResult(cleanedResult);
        }
    }

    // Check and clean items when player joins server
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        cleanPlayerInventory(player);
    }

    // Check and clean items when player switches held item
    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        PlayerInventory inventory = player.getInventory();
        ItemStack newItem = inventory.getItem(event.getNewSlot());

        if (cleanItem(newItem)) {
            player.sendMessage("§cBanned enchantments removed from your item!");
        }
    }

    // Check and clean items when clicking in inventory
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        ItemStack cursorItem = event.getCursor();

        // Clean the clicked item
        boolean cleaned = false;
        if (cleanItem(clickedItem)) {
            cleaned = true;
        }

        // Clean the cursor item
        if (cleanItem(cursorItem)) {
            cleaned = true;
        }

        // Also clean when moving items around
        if (event.getSlotType() == InventoryType.SlotType.CONTAINER ||
                event.getSlotType() == InventoryType.SlotType.QUICKBAR ||
                event.getSlotType() == InventoryType.SlotType.ARMOR) {

            // Schedule a check for next tick to ensure item movement is complete
            org.bukkit.Bukkit.getScheduler().runTaskLater(
                    Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("MaceControl")),
                    () -> {
                        if (cleanPlayerInventory(player)) {
                            player.sendMessage("§cBanned enchantments removed from your items!");
                        }
                    }, 1L
            );
        }

        if (cleaned) {
            player.sendMessage("§cBanned enchantments removed from your item!");
        }
    }

    // Clean all items in player's inventory
    private boolean cleanPlayerInventory(Player player) {
        boolean hadBanned = false;
        PlayerInventory inventory = player.getInventory();

        // Check main inventory
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (cleanItem(item)) {
                hadBanned = true;
            }
        }

        // Check armor slots
        for (ItemStack armor : inventory.getArmorContents()) {
            if (cleanItem(armor)) {
                hadBanned = true;
            }
        }

        // Check offhand
        if (cleanItem(inventory.getItemInOffHand())) {
            hadBanned = true;
        }

        return hadBanned;
    }

    // Clean a single item and return true if any enchantments were removed
    private boolean cleanItem(ItemStack item) {
        if (item == null) return false;

        List<Enchantment> bannedEnchants = getBannedEnchantsForItem(item);
        if (bannedEnchants.isEmpty()) return false;

        boolean hadBanned = false;
        for (Enchantment banned : bannedEnchants) {
            if (item.containsEnchantment(banned)) {
                item.removeEnchantment(banned);
                hadBanned = true;
            }
        }

        return hadBanned;
    }

    private List<Enchantment> getBannedEnchantsForItem(ItemStack item) {
        if (item == null) return Arrays.asList();

        if (isSword(item)) {
            return BANNED_SWORD_ENCHANTS;
        } else if (isBow(item)) {
            return BANNED_BOW_ENCHANTS;
        }

        return Arrays.asList();
    }

    private boolean isSword(ItemStack item) {
        if (item == null) return false;
        return Arrays.asList(
                Material.WOODEN_SWORD,
                Material.STONE_SWORD,
                Material.IRON_SWORD,
                Material.GOLDEN_SWORD,
                Material.DIAMOND_SWORD,
                Material.NETHERITE_SWORD
        ).contains(item.getType());
    }

    private boolean isBow(ItemStack item) {
        if (item == null) return false;
        return item.getType() == Material.BOW;
    }

    public static boolean isBannedEnchantment(Enchantment enchantment) {
        return BANNED_SWORD_ENCHANTS.contains(enchantment) || BANNED_BOW_ENCHANTS.contains(enchantment);
    }
}