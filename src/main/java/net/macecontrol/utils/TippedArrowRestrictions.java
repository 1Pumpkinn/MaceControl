package net.macecontrol.utils;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.Material;

public class TippedArrowRestrictions implements Listener {

    // Check if a tipped arrow has restricted effects
    private boolean hasRestrictedEffects(ItemStack item) {
        if (item == null || item.getType() != Material.TIPPED_ARROW || !item.hasItemMeta()) {
            return false;
        }

        if (item.getItemMeta() instanceof PotionMeta) {
            PotionMeta potionMeta = (PotionMeta) item.getItemMeta();

            // Check base potion type for restricted effects
            PotionType potionType = potionMeta.getBasePotionType();
            if (potionType == PotionType.STRENGTH ||
                    potionType == PotionType.LONG_STRENGTH ||
                    potionType == PotionType.STRONG_STRENGTH ||
                    potionType == PotionType.SWIFTNESS ||
                    potionType == PotionType.LONG_SWIFTNESS ||
                    potionType == PotionType.STRONG_SWIFTNESS ||
                    potionType == PotionType.FIRE_RESISTANCE ||
                    potionType == PotionType.LONG_FIRE_RESISTANCE ||
                    potionType == PotionType.WEAKNESS ||
                    potionType == PotionType.LONG_WEAKNESS ||
                    potionType == PotionType.HARMING ||
                    potionType == PotionType.STRONG_HARMING ||
                    potionType == PotionType.POISON ||
                    potionType == PotionType.LONG_POISON ||
                    potionType == PotionType.STRONG_POISON) {
                return true;
            }

            // Check custom effects
            for (PotionEffect effect : potionMeta.getCustomEffects()) {
                if (effect.getType() == PotionEffectType.STRENGTH ||
                        effect.getType() == PotionEffectType.SPEED ||
                        effect.getType() == PotionEffectType.FIRE_RESISTANCE ||
                        effect.getType() == PotionEffectType.WEAKNESS ||
                        effect.getType() == PotionEffectType.INSTANT_DAMAGE ||
                        effect.getType() == PotionEffectType.POISON) {
                    return true;
                }
            }
        }

        return false;
    }

    // Prevent shooting restricted tipped arrows
    @EventHandler
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        ItemStack arrow = event.getConsumable();

        if (hasRestrictedEffects(arrow)) {
            event.setCancelled(true);
            player.sendMessage("§cYou cannot shoot tipped arrows with Strength, Speed, Fire Resistance, Weakness, Instant Damage or Poison effects!");
            player.updateInventory();
        }
    }

    // Prevent tipped arrow effects from being applied on hit
    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow)) return;

        Arrow arrow = (Arrow) event.getEntity();

        // Check if arrow has restricted potion effects
        if (arrow.hasCustomEffects()) {
            for (PotionEffect effect : arrow.getCustomEffects()) {
                if (effect.getType() == PotionEffectType.STRENGTH ||
                        effect.getType() == PotionEffectType.SPEED ||
                        effect.getType() == PotionEffectType.FIRE_RESISTANCE ||
                        effect.getType() == PotionEffectType.WEAKNESS ||
                        effect.getType() == PotionEffectType.INSTANT_DAMAGE ||
                        effect.getType() == PotionEffectType.POISON) {

                    // Clear all custom effects from the arrow
                    arrow.clearCustomEffects();

                    // Notify the shooter if it's a player
                    if (arrow.getShooter() instanceof Player) {
                        Player shooter = (Player) arrow.getShooter();
                        shooter.sendMessage("§cRestricted tipped arrow effects have been blocked!");
                    }

                    // Notify the hit player if applicable
                    if (event.getHitEntity() instanceof Player) {
                        Player hitPlayer = (Player) event.getHitEntity();
                        hitPlayer.sendMessage("§cA restricted tipped arrow effect was blocked!");
                    }

                    break;
                }
            }
        }
    }
}