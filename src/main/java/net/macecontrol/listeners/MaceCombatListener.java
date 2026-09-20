package net.macecontrol.listeners;

import net.macecontrol.config.MaceConfig;
import net.macecontrol.util.MaceItemUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applies a configurable cooldown between mace hits, independent of vanilla
 * attack-speed cooldown mechanics.
 */
public class MaceCombatListener implements Listener {

    private final MaceConfig config;
    private final Map<UUID, Long> lastHitTimestamps = new ConcurrentHashMap<>();

    public MaceCombatListener(MaceConfig config) {
        this.config = config;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMaceDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (!MaceItemUtil.isMace(inHand)) return;

        int cooldownSeconds = config.getMaceCooldownSeconds();
        if (cooldownSeconds <= 0) return;

        long now = System.currentTimeMillis();
        long cooldownMillis = cooldownSeconds * 1000L;
        Long lastHit = lastHitTimestamps.get(player.getUniqueId());

        if (lastHit != null && now - lastHit < cooldownMillis) {
            // Cancel the hit without touching setCooldown() again, or the visual
            // cooldown bar would reset back to full on every blocked swing.
            event.setCancelled(true);
            return;
        }

        lastHitTimestamps.put(player.getUniqueId(), now);
        player.setCooldown(Material.MACE, cooldownSeconds * 20);
    }
}