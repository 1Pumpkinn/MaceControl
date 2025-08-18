package net.macecontrol;

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

            // Check base potion type
            PotionType potionType = potionMeta.getBasePotionType();
            if (potionType == PotionType.STRENGTH ||
                    potionType == PotionType.LONG_STRENGTH ||
                    potionType == PotionType.STRONG_STRENGTH ||
                    potionType == PotionType.SWIFTNESS ||
                    potionType == PotionType.LONG_SWIFTNESS ||
                    potionType == PotionType.FIRE_RESISTANCE ||
                    potionType == PotionType.LONG_FIRE_RESISTANCE ||
                    potionType == PotionType.STRONG_SWIFTNESS) {
                return true;
            }

            // Check custom effects
            for (PotionEffect effect : potionMeta.getCustomEffects()) {
                if (effect.getType() == PotionEffectType.STRENGTH ||
                        effect.getType() == PotionEffectType.FIRE_RESISTANCE ||
                        effect.getType() == PotionEffectType.SPEED) {
                    return true;
                }
            }
        }

        return false;
    }

    // Prevent drinking strength/speed potions
    @EventHandler
    public void onPlayerDrinkPotion(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (hasRestrictedEffects(item)) {
            event.setCancelled(true);
            player.sendMessage("§cYou cannot drink Strength Speed or Fire Resistance potions!");
            player.updateInventory();
        }
    }

    // Prevent splash potions from giving strength/speed effects
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
                thrower.sendMessage("§cStrength Speed or Fire Resistance potions are disabled!");
            }

            // Also notify affected entities if they're players
            for (org.bukkit.entity.LivingEntity entity : event.getAffectedEntities()) {
                if (entity instanceof Player) {
                    Player affected = (Player) entity;
                    affected.sendMessage("§cStrength Speed or Fire Resistance are disabled on this server!");
                }
            }
        }
    }
}