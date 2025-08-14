package net.macecontrol;

import net.macecontrol.Main;
import net.macecontrol.PluginDataManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

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

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        ItemStack result = event.getInventory().getResult();

        // Only process if it's a mace
        if (result == null || result.getType() != Material.MACE) return;

        if (dataManager.getTotalMacesCrafted() >= maxMaces) {
            event.getInventory().setResult(null);
            return;
        }

        // Add persistent data to mace
        ItemStack mace = result.clone();
        ItemMeta meta = mace.getItemMeta();
        if (meta != null) {
            int maceNumber = dataManager.getTotalMacesCrafted() + 1;
            meta.getPersistentDataContainer().set(maceNumberKey, PersistentDataType.INTEGER, maceNumber);
            meta.setDisplayName("§6Mace #" + maceNumber);
            mace.setItemMeta(meta);
            event.getInventory().setResult(mace);
        }
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        ItemStack item = event.getItem();

        if (item.getType() != Material.MACE) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        Integer maceNumber = meta.getPersistentDataContainer().get(maceNumberKey, PersistentDataType.INTEGER);
        if (maceNumber != null && maceNumber != 1) {
            event.setCancelled(true);
            event.getEnchanter().sendMessage("§cOnly Mace #1 can be enchanted!");
        }
    }
}
