package net.macecontrol.utils;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class EnchantmentManager implements Listener {

    private static final List<Enchantment> BANNED_ENCHANTS = Arrays.asList(
            Enchantment.KNOCKBACK,
            Enchantment.FIRE_ASPECT,
            Enchantment.FLAME
    );

    // Prevent banned enchantments from appearing in enchanting table
    @EventHandler
    public void onPrepareItemEnchant(PrepareItemEnchantEvent event) {
        ItemStack item = event.getItem();
        if (!isSword(item)) return;

        Random random = new Random();
        for (int i = 0; i < event.getOffers().length; i++) {
            if (event.getOffers()[i] == null) continue;

            // Force reroll if banned
            int attempts = 0;
            while (BANNED_ENCHANTS.contains(event.getOffers()[i].getEnchantment()) && attempts < 20) {
                Enchantment[] possible = Enchantment.values();
                Enchantment newEnchant;
                int newLevel;

                do {
                    newEnchant = possible[random.nextInt(possible.length)];
                    newLevel = random.nextInt(newEnchant.getMaxLevel()) + 1;
                    attempts++;
                } while (BANNED_ENCHANTS.contains(newEnchant) || !newEnchant.canEnchantItem(item));

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
        if (!isSword(item)) return;

        // Check if any banned enchants are being added
        boolean hadBanned = false;
        for (Enchantment enchant : BANNED_ENCHANTS) {
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
        if (result == null || !isSword(result)) return;

        boolean hadBanned = false;
        ItemStack cleanedResult = result.clone();

        for (Enchantment banned : BANNED_ENCHANTS) {
            if (cleanedResult.containsEnchantment(banned)) {
                cleanedResult.removeEnchantment(banned);
                hadBanned = true;
            }
        }

        if (hadBanned) {
            event.setResult(cleanedResult);
        }
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

    public static boolean isBannedEnchantment(Enchantment enchantment) {
        return BANNED_ENCHANTS.contains(enchantment);
    }
}