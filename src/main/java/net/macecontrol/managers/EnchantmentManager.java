package net.macecontrol.managers;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
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