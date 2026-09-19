package net.macecontrol.listeners;

import net.macecontrol.config.MaceConfig;
import net.macecontrol.util.MaceItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Sweeps players' inventories for maces that are no longer valid - i.e. their
 * tagged number now exceeds a max-maces limit that was lowered after they were
 * crafted - and removes them. Runs on join, shortly after any inventory click,
 * and on a single shared periodic timer (rather than one timer per player).
 */
public class MaceInventoryGuardListener implements Listener {

    private static final long JOIN_CHECK_DELAY_TICKS = 20L;
    private static final long PERIODIC_SWEEP_PERIOD_TICKS = 200L; // 10 seconds

    private final JavaPlugin plugin;
    private final MaceConfig config;

    public MaceInventoryGuardListener(JavaPlugin plugin, MaceConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    /** Starts the single shared sweep task. Call once from onEnable(). */
    public void startPeriodicSweep() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                removeInvalidMaces(player);
            }
        }, PERIODIC_SWEEP_PERIOD_TICKS, PERIODIC_SWEEP_PERIOD_TICKS);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> removeInvalidMaces(player), JOIN_CHECK_DELAY_TICKS);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                removeInvalidMaces(player);
            }
        }, 1L);
    }

    private void removeInvalidMaces(Player player) {
        PlayerInventory inventory = player.getInventory();
        int maxMaces = config.getMaxMaces();
        int removedCount = 0;

        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && MaceItemUtil.isMace(item) && !MaceItemUtil.isValidMace(item, maxMaces)) {
                inventory.setItem(i, null);
                removedCount++;
            }
        }

        if (removedCount > 0) {
            plugin.getLogger().info("Removed " + removedCount + " invalid mace(s) from player " + player.getName());
        }
    }

    /** Used by /maceclean to report how many maces were removed from each online player. */
    public int cleanAllOnlinePlayers() {
        int totalRemoved = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            int before = countMaces(player);
            removeInvalidMaces(player);
            totalRemoved += before - countMaces(player);
        }
        return totalRemoved;
    }

    private int countMaces(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (MaceItemUtil.isMace(item)) count++;
        }
        return count;
    }
}
