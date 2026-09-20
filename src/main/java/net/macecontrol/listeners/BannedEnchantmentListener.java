package net.macecontrol.listeners;

import net.macecontrol.config.MaceConfig;
import net.macecontrol.util.MessageUtil;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Random;

/**
 * Strips any server-banned enchantment (see {@code banned-enchantments} in
 * config.yml) from items: rerolled away from enchanting-table offers, removed
 * (with an XP refund) if one is ever actually applied, stripped from anvil
 * results, and swept from a player's gear on join / item switch.
 * <p>
 * Every handler bails out immediately when no enchantments are banned, so
 * this listener costs nothing on servers that don't use the feature.
 */
public class BannedEnchantmentListener implements Listener {

    private static final Random RANDOM = new Random();
    private static final int MAX_REROLL_ATTEMPTS = 20;

    private final JavaPlugin plugin;
    private final MaceConfig config;

    public BannedEnchantmentListener(JavaPlugin plugin, MaceConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    @EventHandler
    public void onPrepareItemEnchant(PrepareItemEnchantEvent event) {
        List<Enchantment> banned = config.getBannedEnchantments();
        if (banned.isEmpty()) return;

        ItemStack item = event.getItem();
        for (var offer : event.getOffers()) {
            if (offer == null || !banned.contains(offer.getEnchantment())) continue;
            rerollAwayFromBanned(offer, item, banned);
        }
    }

    private void rerollAwayFromBanned(org.bukkit.enchantments.EnchantmentOffer offer, ItemStack item, List<Enchantment> banned) {
        Enchantment[] candidates = Enchantment.values();

        for (int attempt = 0; attempt < MAX_REROLL_ATTEMPTS; attempt++) {
            Enchantment candidate = candidates[RANDOM.nextInt(candidates.length)];
            if (banned.contains(candidate) || !candidate.canEnchantItem(item)) continue;

            offer.setEnchantment(candidate);
            offer.setEnchantmentLevel(RANDOM.nextInt(candidate.getMaxLevel()) + 1);
            return;
        }
        // Couldn't find a legal replacement in time - leave the offer as-is;
        // onEnchant() below will still strip it if the player picks it anyway.
    }

    @EventHandler
    public void onEnchantItem(EnchantItemEvent event) {
        List<Enchantment> banned = config.getBannedEnchantments();
        if (banned.isEmpty()) return;

        boolean removedAny = false;
        for (Enchantment enchantment : banned) {
            if (event.getEnchantsToAdd().remove(enchantment) != null) {
                removedAny = true;
            }
        }

        if (removedAny) {
            event.getEnchanter().giveExp(event.getExpLevelCost());
            MessageUtil.sendBannedEnchantmentRefund(event.getEnchanter());
        }
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        List<Enchantment> banned = config.getBannedEnchantments();
        if (banned.isEmpty()) return;

        ItemStack result = event.getResult();
        if (result == null) return;

        ItemStack cleaned = result.clone();
        boolean removedAny = false;
        for (Enchantment enchantment : banned) {
            if (cleaned.containsEnchantment(enchantment)) {
                cleaned.removeEnchantment(enchantment);
                removedAny = true;
            }
        }

        if (removedAny) {
            event.setResult(cleaned);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (config.getBannedEnchantments().isEmpty()) return;
        cleanInventory(event.getPlayer());
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        List<Enchantment> banned = config.getBannedEnchantments();
        if (banned.isEmpty()) return;

        ItemStack newItem = event.getPlayer().getInventory().getItem(event.getNewSlot());
        if (cleanItem(newItem, banned)) {
            MessageUtil.sendBannedEnchantmentRemoved(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (config.getBannedEnchantments().isEmpty()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // The click may still be mid-transfer, so re-check the whole inventory next tick.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && cleanInventory(player)) {
                MessageUtil.sendBannedEnchantmentRemoved(player);
            }
        }, 1L);
    }

    private boolean cleanInventory(Player player) {
        List<Enchantment> banned = config.getBannedEnchantments();
        if (banned.isEmpty()) return false;

        PlayerInventory inventory = player.getInventory();
        boolean removedAny = false;

        for (int i = 0; i < inventory.getSize(); i++) {
            removedAny |= cleanItem(inventory.getItem(i), banned);
        }
        for (ItemStack armor : inventory.getArmorContents()) {
            removedAny |= cleanItem(armor, banned);
        }
        removedAny |= cleanItem(inventory.getItemInOffHand(), banned);

        return removedAny;
    }

    private boolean cleanItem(ItemStack item, List<Enchantment> banned) {
        if (item == null) return false;

        boolean removedAny = false;
        for (Enchantment enchantment : banned) {
            if (item.containsEnchantment(enchantment)) {
                item.removeEnchantment(enchantment);
                removedAny = true;
            }
        }
        return removedAny;
    }
}