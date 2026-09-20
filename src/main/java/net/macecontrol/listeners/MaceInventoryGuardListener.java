package net.macecontrol.listeners;

import net.macecontrol.cleaning.MaceCleaner;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Keeps online players' inventories free of invalid maces - i.e. maces whose
 * tagged number now exceeds a max-maces limit that was lowered after they
 * were crafted. Runs shortly after any inventory click, on join, and on a
 * single shared periodic timer (rather than one timer per player). All the
 * actual removal logic (including recursing into shulker boxes and bundles)
 * lives in {@link MaceCleaner}, which /macecontrol clean also uses for its
 * full server-wide sweep.
 */
public class MaceInventoryGuardListener implements Listener {

    private static final long JOIN_CHECK_DELAY_TICKS = 20L;
    private static final long PERIODIC_SWEEP_PERIOD_TICKS = 200L; // 10 seconds

    private final JavaPlugin plugin;
    private final MaceCleaner cleaner;

    public MaceInventoryGuardListener(JavaPlugin plugin, MaceCleaner cleaner) {
        this.plugin = plugin;
        this.cleaner = cleaner;
    }

    /** Starts the single shared sweep task. Call once from onEnable(). */
    public void startPeriodicSweep() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                cleaner.cleanPlayer(player);
            }
        }, PERIODIC_SWEEP_PERIOD_TICKS, PERIODIC_SWEEP_PERIOD_TICKS);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Delayed so the player's inventory has fully loaded before we scan it.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                int removed = cleaner.cleanPlayer(player);
                if (removed > 0) {
                    plugin.getLogger().info("Removed " + removed + " invalid mace(s) from " + player.getName() + " on join");
                }
            }
        }, JOIN_CHECK_DELAY_TICKS);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        // The click may still be mid-transfer, so re-check the whole inventory next tick.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                cleaner.cleanPlayer(player);
            }
        }, 1L);
    }
}