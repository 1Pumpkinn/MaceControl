package net.macecontrol.managers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatManager implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> combatTimers = new HashMap<>();
    private static final int COMBAT_COOLDOWN_SECONDS = 20;

    public CombatManager(JavaPlugin plugin) {
        this.plugin = plugin;
        startRepeatingTasks();
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;

        Player attacker = (Player) event.getDamager();
        LivingEntity victim = (LivingEntity) event.getEntity();

        // Set combat timer for both players if victim is a player
        if (victim instanceof Player) {
            setCombatTimer(attacker.getUniqueId());
            setCombatTimer(((Player) victim).getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().toLowerCase();

        // Allow certain commands during combat
        if (command.startsWith("/msg") || command.startsWith("/tell") ||
                command.startsWith("/reply") || command.startsWith("/r")) {
            return;
        }

        if (isInCombat(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage("§cYou are in combat! You must wait " + COMBAT_COOLDOWN_SECONDS +
                    " seconds after your last PvP action to use commands.");
        }
    }

    private void setCombatTimer(UUID playerUUID) {
        combatTimers.put(playerUUID, System.currentTimeMillis());
    }

    public boolean isInCombat(UUID playerUUID) {
        Long combatTime = combatTimers.get(playerUUID);
        return combatTime != null && (System.currentTimeMillis() - combatTime) < (COMBAT_COOLDOWN_SECONDS * 1000);
    }

    private void startRepeatingTasks() {
        // Combat elytra removal task
        new BukkitRunnable() {
            @Override
            public void run() {
                combatElytraRemovalTask();
            }
        }.runTaskTimer(plugin, 1L, 1L);

        // Cleanup task
        new BukkitRunnable() {
            @Override
            public void run() {
                cleanupTask();
            }
        }.runTaskTimer(plugin, 1200L, 1200L);
    }

    private void combatElytraRemovalTask() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isInCombat(player.getUniqueId())) continue;

            ItemStack chestplate = player.getInventory().getChestplate();
            if (chestplate != null && chestplate.getType() == Material.ELYTRA) {
                // Don't remove custom items (you can add custom weapon checks here later)
                // if (CustomWeaponsManager.isCustomWeapon(chestplate)) continue;

                player.getInventory().setChestplate(null);
                player.getInventory().addItem(chestplate);
                player.sendMessage("§cYou cannot wear an Elytra during combat!");
            }
        }
    }

    private void cleanupTask() {
        long now = System.currentTimeMillis();
        combatTimers.entrySet().removeIf(e -> (now - e.getValue()) > (COMBAT_COOLDOWN_SECONDS * 1000));
    }

    public int getCombatTimeLeft(UUID playerUUID) {
        if (!isInCombat(playerUUID)) return 0;
        long combatTime = combatTimers.get(playerUUID);
        long timeLeft = COMBAT_COOLDOWN_SECONDS - ((System.currentTimeMillis() - combatTime) / 1000);
        return Math.max(0, (int) timeLeft);
    }
}