package net.macecontrol.listeners;

import net.macecontrol.config.MaceConfig;
import net.macecontrol.data.MaceDataStore;
import net.macecontrol.util.MaceItemUtil;
import net.macecontrol.util.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Enforces the server-wide mace crafting limit: tags each newly crafted mace
 * with its number, and blocks crafting once the limit (or an outright ban) is
 * reached.
 */
public class MaceCraftListener implements Listener {

    private final MaceConfig config;
    private final MaceDataStore dataStore;

    public MaceCraftListener(MaceConfig config, MaceDataStore dataStore) {
        this.config = config;
        this.dataStore = dataStore;
    }

    private boolean isCraftingBlocked(int currentCount, int maxMaces) {
        return config.isMaceCraftingBanned() || currentCount >= maxMaces;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        ItemStack result = event.getInventory().getResult();
        if (!MaceItemUtil.isMace(result)) return;

        int currentCount = dataStore.getTotalMacesCrafted();
        if (isCraftingBlocked(currentCount, config.getMaxMaces())) {
            event.getInventory().setResult(null);
            return;
        }

        ItemStack mace = result.clone();
        ItemMeta meta = mace.getItemMeta();
        if (meta != null) {
            MaceItemUtil.setMaceNumber(mace, currentCount + 1);
            event.getInventory().setResult(mace);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCraftItem(CraftItemEvent event) {
        ItemStack result = event.getCurrentItem();
        if (!MaceItemUtil.isMace(result)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int maxMaces = config.getMaxMaces();
        int currentCount = dataStore.getTotalMacesCrafted();

        if (isCraftingBlocked(currentCount, maxMaces)) {
            event.setCancelled(true);
            MessageUtil.sendLimitReached(player, maxMaces, config.getEnchantableMaces(), currentCount);
            player.updateInventory();
            return;
        }

        if (event.isShiftClick()) {
            // Shift-clicking a mace out of the crafting grid skips onPrepareCraft's
            // tagging path for every subsequent item, so it's simplest to disallow it.
            event.setCancelled(true);
            player.updateInventory();
            return;
        }

        Integer maceNumber = MaceItemUtil.getMaceNumber(result);
        if (maceNumber == null) {
            event.setCancelled(true);
            return;
        }

        dataStore.incrementTotalMaces();
        player.getServer().getLogger().info(
                "Mace #" + maceNumber + " crafted by " + player.getName() + ". Total maces now: " + dataStore.getTotalMacesCrafted());
    }

    /**
     * Blocks picking a crafted mace up while already holding an item on the cursor,
     * which would otherwise let a player dupe a queued craft result.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;
        if (event.getInventory().getType() != InventoryType.CRAFTING
                && event.getInventory().getType() != InventoryType.WORKBENCH) return;

        ItemStack result = event.getCurrentItem();
        if (!MaceItemUtil.isMace(result)) return;

        ItemStack cursor = event.getCursor();
        if (cursor != null && cursor.getType() != Material.AIR) {
            event.setCancelled(true);
            player.updateInventory();
        }
    }
}