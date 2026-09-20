package net.macecontrol.listeners;

import net.macecontrol.cleaning.MaceCleaner;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps the world free of invalid maces (untagged, over the max-maces limit, or left over from
 * before a {@code /macecontrol clean}). Runs shortly after any inventory click, on join, on a
 * single shared periodic timer, and whenever a chunk's containers/entities load - which is how
 * offline players and unloaded chunks get swept after a clean. All the actual removal logic
 * (including recursing into shulker boxes and bundles) lives in {@link MaceCleaner}.
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

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (event.isNewChunk()) return;
        Chunk chunk = event.getChunk();
        // Next tick: don't touch tile entities in the middle of the chunk load itself.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (chunk.isLoaded()) {
                int removed = cleaner.cleanChunkContainers(chunk);
                if (removed > 0) {
                    plugin.getLogger().info("Removed " + removed + " invalid mace(s) from containers in chunk "
                            + chunk.getX() + "," + chunk.getZ() + " (" + chunk.getWorld().getName() + ")");
                }
            }
        });
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        List<Entity> entities = new ArrayList<>(event.getEntities());
        if (entities.isEmpty()) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            int removed = 0;
            for (Entity entity : entities) {
                removed += cleaner.cleanEntity(entity);
            }
            if (removed > 0) {
                plugin.getLogger().info("Removed " + removed + " invalid mace(s) from entities on chunk load");
            }
        });
    }
}