package net.macecontrol.utils;

import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

public class PotionRestrictions implements Listener {

    // Check if a potion item contains restricted effects
    private boolean hasRestrictedEffects(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        if (item.getItemMeta() instanceof PotionMeta) {
            PotionMeta potionMeta = (PotionMeta) item.getItemMeta();

            // Check base potion type - only restrict Strength 2
            PotionType potionType = potionMeta.getBasePotionType();
            if (potionType == PotionType.STRONG_STRENGTH ||  // This is Strength 2
                    potionType == PotionType.SWIFTNESS ||
                    potionType == PotionType.LONG_SWIFTNESS ||
                    potionType == PotionType.STRONG_SWIFTNESS ||
                    potionType == PotionType.FIRE_RESISTANCE ||
                    potionType == PotionType.LONG_FIRE_RESISTANCE) {
                return true;
            }

            // Check custom effects - allow Strength 1 (amplifier 0), restrict Strength 2+ (amplifier 1+)
            for (PotionEffect effect : potionMeta.getCustomEffects()) {
                if (effect.getType() == PotionEffectType.STRENGTH && effect.getAmplifier() >= 1) {
                    return true; // Strength 2 or higher is restricted
                }
                if (effect.getType() == PotionEffectType.FIRE_RESISTANCE ||
                        effect.getType() == PotionEffectType.SPEED) {
                    return true;
                }
            }
        }

        return false;
    }

    // Prevent drinking restricted potions
    @EventHandler
    public void onPlayerDrinkPotion(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (hasRestrictedEffects(item)) {
            event.setCancelled(true);
            player.sendMessage("§cYou cannot drink Strength 2+ Speed or Fire Resistance potions! (Strength 1 is allowed)");
            player.updateInventory();
        }
    }

    // Prevent splash potions from giving restricted effects
    @EventHandler
    public void onPotionSplash(PotionSplashEvent event) {
        ThrownPotion potion = event.getPotion();
        ItemStack item = potion.getItem();

        if (hasRestrictedEffects(item)) {
            // Cancel the event to prevent any effects
            event.setCancelled(true);

            // Notify the thrower if it's a player
            if (potion.getShooter() instanceof Player) {
                Player thrower = (Player) potion.getShooter();
                thrower.sendMessage("§cStrength 2 Speed or Fire Resistance potions are disabled! (Strength 1 is allowed)");
            }

            // Also notify affected entities if they're players
            for (org.bukkit.entity.LivingEntity entity : event.getAffectedEntities()) {
                if (entity instanceof Player) {
                    Player affected = (Player) entity;
                    affected.sendMessage("§cStrength 2+ Speed or Fire Resistance are disabled on this server! (Strength 1 is allowed)");
                }
            }
        }
    }
}