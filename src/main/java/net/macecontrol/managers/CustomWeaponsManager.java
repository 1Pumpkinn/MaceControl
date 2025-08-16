package net.macecontrol.managers;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;

public class CustomWeaponsManager implements Listener {

    // Custom weapon names - centralized for easy management
    public static final String THOR_HAMMER_NAME = "§eThor's Hammer";
    public static final String DRAGON_CHESTPLATE_NAME = "§dDragon Chestplate";
    public static final String CORAL_TRIDENT_NAME = "§3Trident Of The Coral Sea";

    private static final double THOR_HAMMER_CHANCE = 0.05;
    private static final double TRIDENT_POISON_CHANCE = 0.10;
    private static final double DRAGON_CHESTPLATE_CHANCE = 1.0;

    // Create custom weapons with proper metadata
    public static ItemStack createThorHammer() {
        ItemStack hammer = new ItemStack(Material.NETHERITE_AXE);
        ItemMeta meta = hammer.getItemMeta();
        meta.setDisplayName(THOR_HAMMER_NAME);
        meta.setLore(Arrays.asList("§7A legendary weapon of the gods", "§e5% chance to summon lightning"));
        meta.addEnchant(Enchantment.SHARPNESS, 5, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        hammer.setItemMeta(meta);
        return hammer;
    }

    public static ItemStack createDragonChestplate() {
        ItemStack chestplate = new ItemStack(Material.ELYTRA);
        ItemMeta meta = chestplate.getItemMeta();
        meta.setDisplayName(DRAGON_CHESTPLATE_NAME);
        meta.setLore(Arrays.asList("§7Forged from dragon scales", "§dSlows attackers with dragon magic"));
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.addEnchant(Enchantment.PROTECTION, 4, true);
        chestplate.setItemMeta(meta);
        return chestplate;
    }

    public static ItemStack createCoralTrident() {
        ItemStack trident = new ItemStack(Material.TRIDENT);
        ItemMeta meta = trident.getItemMeta();
        meta.setDisplayName(CORAL_TRIDENT_NAME);
        meta.setLore(Arrays.asList("§7From the depths of the ocean", "§310% chance to poison targets"));
        meta.addEnchant(Enchantment.LOYALTY, 3, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        trident.setItemMeta(meta);
        return trident;
    }

    // Check if an item is a custom weapon
    public static boolean isCustomWeapon(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return false;
        }
        String displayName = item.getItemMeta().getDisplayName();
        return displayName.equals(THOR_HAMMER_NAME) ||
                displayName.equals(DRAGON_CHESTPLATE_NAME) ||
                displayName.equals(CORAL_TRIDENT_NAME);
    }

    // Prevent custom weapons from being crafted
    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        ItemStack result = event.getRecipe() != null ? event.getRecipe().getResult() : null;
        if (result != null && isCustomWeapon(result)) {
            event.getInventory().setResult(null);
        }
    }

    // Prevent custom weapons from being created via smithing table
    @EventHandler
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        ItemStack result = event.getResult();
        if (result != null && isCustomWeapon(result)) {
            event.setResult(null);
        }
    }

    // Prevent renaming items to match custom weapon names in anvil
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getType() != InventoryType.ANVIL) return;
        if (event.getSlot() != 2) return; // Result slot

        ItemStack result = event.getCurrentItem();
        if (result != null && isCustomWeapon(result)) {
            // Check if this is a legitimate custom weapon or a renamed fake
            ItemStack input = event.getInventory().getItem(0);
            if (input != null && !isCustomWeapon(input)) {
                // Someone is trying to rename a regular item to match a custom weapon
                event.setCancelled(true);
                ((Player) event.getWhoClicked()).sendMessage("§cYou cannot create fake custom weapons!");
            }
        }
    }

    // Handle weapon special effects
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;

        Player attacker = (Player) event.getDamager();
        LivingEntity victim = (LivingEntity) event.getEntity();
        ItemStack weapon = attacker.getInventory().getItemInMainHand();

        handleThorHammer(attacker, victim, weapon);
        handleDragonChestplate(attacker, victim);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        if (!(projectile instanceof Trident)) return;
        if (!(projectile.getShooter() instanceof Player)) return;
        if (!(event.getHitEntity() instanceof LivingEntity)) return;

        Player shooter = (Player) projectile.getShooter();
        LivingEntity victim = (LivingEntity) event.getHitEntity();

        boolean hasSpecialTrident = isCoralSeaTrident(shooter.getInventory().getItemInMainHand()) ||
                isCoralSeaTrident(shooter.getInventory().getItemInOffHand());

        if (!hasSpecialTrident) {
            for (ItemStack item : shooter.getInventory().getContents()) {
                if (isCoralSeaTrident(item)) {
                    hasSpecialTrident = true;
                    break;
                }
            }
        }

        if (hasSpecialTrident && Math.random() < TRIDENT_POISON_CHANCE) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 0));
            shooter.sendMessage("§3🌊 The Trident of the Coral Sea poisoned " +
                    (victim instanceof Player ? ((Player) victim).getName() : victim.getType().name()) + "!");
        }
    }

    private void handleThorHammer(Player attacker, LivingEntity victim, ItemStack weapon) {
        if (weapon == null || weapon.getType() != Material.NETHERITE_AXE) return;
        ItemMeta meta = weapon.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;
        if (!meta.getDisplayName().equals(THOR_HAMMER_NAME)) return;

        if (Math.random() < THOR_HAMMER_CHANCE) {
            victim.getWorld().strikeLightning(victim.getLocation());
            attacker.sendMessage("§e⚡ Thor's Hammer has summoned lightning!");
        }
    }

    private void handleDragonChestplate(Player attacker, LivingEntity victim) {
        if (!(victim instanceof Player)) return;
        Player playerVictim = (Player) victim;
        ItemStack chestplate = playerVictim.getInventory().getChestplate();
        if (chestplate == null || chestplate.getType() != Material.ELYTRA) return;
        ItemMeta meta = chestplate.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;
        if (!meta.getDisplayName().equals(DRAGON_CHESTPLATE_NAME)) return;

        if (Math.random() < DRAGON_CHESTPLATE_CHANCE) {
            attacker.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 200, 0));
            playerVictim.sendMessage("§d🐉 The Dragon Chestplate slowed your attacker!");
            attacker.sendMessage("§dYou feel light as air...");
        }
    }

    private boolean isCoralSeaTrident(ItemStack item) {
        if (item == null || item.getType() != Material.TRIDENT) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() && meta.getDisplayName().equals(CORAL_TRIDENT_NAME);
    }
}