package net.macecontrol.listeners;

import net.macecontrol.config.MaceConfig;
import net.macecontrol.util.MaceItemUtil;
import net.macecontrol.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Only the first {@code enchantable-maces} maces crafted are allowed to be
 * enchanted. This blocks every path onto/off of an enchanting table or anvil
 * that could otherwise get around that limit.
 */
public class MaceEnchantListener implements Listener {

    private final MaceConfig config;

    public MaceEnchantListener(MaceConfig config) {
        this.config = config;
    }

    private boolean isRestrictedMace(ItemStack item) {
        return MaceItemUtil.isMace(item) && !MaceItemUtil.isEnchantable(item, config.getEnchantableMaces());
    }

    @EventHandler
    public void onPrepareItemEnchant(PrepareItemEnchantEvent event) {
        denyIfRestricted(event.getItem(), event);
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        denyIfRestricted(event.getItem(), event);
    }

    private void denyIfRestricted(ItemStack item, Cancellable event) {
        if (isRestrictedMace(item)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack base = event.getInventory().getItem(0);
        ItemStack addition = event.getInventory().getItem(1);

        if (!isRestrictedMace(base)) return;
        if (addition == null || addition.getType() != Material.ENCHANTED_BOOK) return;

        // Repairing or renaming a restricted mace is still fine - only block
        // it from picking up an enchantment via an enchanted book.
        event.setResult(null);

        if (!event.getViewers().isEmpty() && event.getViewers().get(0) instanceof Player player) {
            MessageUtil.sendAnvilDenied(player, config.getEnchantableMaces());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getType() != InventoryType.ENCHANTING) return;

        ItemStack clicked = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        boolean blockedSlotMove = event.getSlot() == 0
                && (isRestrictedMace(clicked) || isRestrictedMace(cursor));
        boolean blockedShiftClick = event.isShiftClick() && isRestrictedMace(clicked);

        if (blockedSlotMove || blockedShiftClick) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                MessageUtil.sendEnchantDenied(player, config.getEnchantableMaces());
            }
        }
    }
}
