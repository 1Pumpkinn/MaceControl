package net.macecontrol;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class SkriptConversion implements Listener, CommandExecutor {

    private final Main plugin;
    private final Map<UUID, Long> combatTimers = new HashMap<>();
    private Location spawnLocation;

    private static final int COMBAT_COOLDOWN_SECONDS = 10;
    private static final double THOR_HAMMER_CHANCE = 0.05;
    private static final double TRIDENT_POISON_CHANCE = 0.10;
    private static final double DRAGON_CHESTPLATE_CHANCE = 1.0;

    private static final List<Enchantment> BANNED_ENCHANTS = Arrays.asList(
            Enchantment.KNOCKBACK,
            Enchantment.FIRE_ASPECT,
            Enchantment.FLAME
    );

    // Custom weapon names - centralized for easy management
    private static final String THOR_HAMMER_NAME = "§eThor's Hammer";
    private static final String DRAGON_CHESTPLATE_NAME = "§dDragon Chestplate";
    private static final String CORAL_TRIDENT_NAME = "§3Trident Of The Coral Sea";

    public SkriptConversion(Main plugin) {
        this.plugin = plugin;
        World world = Bukkit.getWorld("world");
        this.spawnLocation = world != null ? new Location(world, -39, 68, 29) : null;
        loadSpawnLocation();
        startRepeatingTasks();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("spawn")) {
            return handleSpawnCommand(sender);
        } else if (command.getName().equalsIgnoreCase("setspawn")) {
            return handleSetSpawnCommand(sender);
        } else if (command.getName().equalsIgnoreCase("giveweapon")) {
            return handleGiveWeaponCommand(sender, args);
        }
        return false;
    }

    private boolean handleGiveWeaponCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("stupidmacecontrol.giveweapon")) {
            sender.sendMessage("§cYou don't have permission to give custom weapons!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cUsage: /giveweapon <player> <weapon>");
            sender.sendMessage("§cWeapons: thorhammer, dragonchestplate, coraltrident");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found!");
            return true;
        }

        String weaponType = args[1].toLowerCase();
        ItemStack weapon = null;

        switch (weaponType) {
            case "thorhammer":
                weapon = createThorHammer();
                break;
            case "dragonchestplate":
                weapon = createDragonChestplate();
                break;
            case "coraltrident":
                weapon = createCoralTrident();
                break;
            default:
                sender.sendMessage("§cUnknown weapon type! Available: thorhammer, dragonchestplate, coraltrident");
                return true;
        }

        if (weapon != null) {
            target.getInventory().addItem(weapon);
            target.sendMessage("§aYou have received a " + weapon.getItemMeta().getDisplayName() + "§a!");
            sender.sendMessage("§aGave " + target.getName() + " a " + weapon.getItemMeta().getDisplayName() + "§a!");
        }

        return true;
    }

    // Create custom weapons with proper metadata
    private ItemStack createThorHammer() {
        ItemStack hammer = new ItemStack(Material.NETHERITE_AXE);
        ItemMeta meta = hammer.getItemMeta();
        meta.setDisplayName(THOR_HAMMER_NAME);
        meta.setLore(Arrays.asList("§7A legendary weapon of the gods", "§e5% chance to summon lightning"));
        // Add some enchantments to make it special
        meta.addEnchant(Enchantment.SHARPNESS, 5, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        hammer.setItemMeta(meta);
        return hammer;
    }

    private ItemStack createDragonChestplate() {
        ItemStack chestplate = new ItemStack(Material.ELYTRA);
        ItemMeta meta = chestplate.getItemMeta();
        meta.setDisplayName(DRAGON_CHESTPLATE_NAME);
        meta.setLore(Arrays.asList("§7Forged from dragon scales", "§dSlows attackers with dragon magic"));
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.addEnchant(Enchantment.PROTECTION, 4, true);
        chestplate.setItemMeta(meta);
        return chestplate;
    }

    private ItemStack createCoralTrident() {
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
    private boolean isCustomWeapon(ItemStack item) {
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

    private boolean handleSpawnCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }
        Player player = (Player) sender;
        if (spawnLocation != null) {
            player.teleport(spawnLocation);
            player.sendMessage("§aTeleported to spawn!");
        } else {
            player.sendMessage("§cSpawn location has not been set! An admin needs to use /setspawn first.");
        }
        return true;
    }

    private boolean handleSetSpawnCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("stupidmacecontrol.setspawn")) {
            player.sendMessage("§cYou don't have permission to set spawn!");
            return true;
        }
        spawnLocation = player.getLocation().clone();
        saveSpawnLocation();
        player.sendMessage("§aSpawn location set to your current position!");
        player.sendMessage("§7Location: " + formatLocation(spawnLocation));
        return true;
    }

    private void loadSpawnLocation() {
        if (plugin.getConfig().contains("spawn")) {
            try {
                String worldName = plugin.getConfig().getString("spawn.world");
                double x = plugin.getConfig().getDouble("spawn.x");
                double y = plugin.getConfig().getDouble("spawn.y");
                double z = plugin.getConfig().getDouble("spawn.z");
                float yaw = (float) plugin.getConfig().getDouble("spawn.yaw");
                float pitch = (float) plugin.getConfig().getDouble("spawn.pitch");
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    spawnLocation = new Location(world, x, y, z, yaw, pitch);
                    plugin.getLogger().info("Loaded spawn location: " + formatLocation(spawnLocation));
                } else {
                    plugin.getLogger().warning("Could not load spawn location - world '" + worldName + "' not found!");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error loading spawn location: " + e.getMessage());
            }
        }
    }

    private void saveSpawnLocation() {
        if (spawnLocation != null) {
            plugin.getConfig().set("spawn.world", spawnLocation.getWorld().getName());
            plugin.getConfig().set("spawn.x", spawnLocation.getX());
            plugin.getConfig().set("spawn.y", spawnLocation.getY());
            plugin.getConfig().set("spawn.z", spawnLocation.getZ());
            plugin.getConfig().set("spawn.yaw", spawnLocation.getYaw());
            plugin.getConfig().set("spawn.pitch", spawnLocation.getPitch());
            plugin.saveConfig();
            plugin.getLogger().info("Saved spawn location: " + formatLocation(spawnLocation));
        }
    }

    private String formatLocation(Location loc) {
        return String.format("%.1f, %.1f, %.1f in %s", loc.getX(), loc.getY(), loc.getZ(), loc.getWorld().getName());
    }

    // ===== ENCHANTMENT PREVENTION WITH REROLL =====
    @EventHandler
    public void onPrepareItemEnchant(PrepareItemEnchantEvent event) {
        ItemStack item = event.getItem();
        if (!isSword(item)) return;

        Random random = new Random();
        for (int i = 0; i < event.getOffers().length; i++) {
            if (event.getOffers()[i] == null) continue;

            // Force reroll if banned
            while (BANNED_ENCHANTS.contains(event.getOffers()[i].getEnchantment())) {
                Enchantment[] possible = Enchantment.values();
                Enchantment newEnchant;
                int newLevel;

                do {
                    newEnchant = possible[random.nextInt(possible.length)];
                    newLevel = random.nextInt(newEnchant.getMaxLevel()) + 1;
                } while (BANNED_ENCHANTS.contains(newEnchant) || !newEnchant.canEnchantItem(item));

                event.getOffers()[i].setEnchantment(newEnchant);
                event.getOffers()[i].setEnchantmentLevel(newLevel);
            }
        }
    }

    @EventHandler
    public void onEnchantItem(EnchantItemEvent event) {
        ItemStack item = event.getItem();
        if (!isSword(item)) return;

        // Remove banned enchants completely and refund exp
        boolean hadBanned = event.getEnchantsToAdd().keySet().removeIf(BANNED_ENCHANTS::contains);
        if (hadBanned) {
            event.getEnchanter().giveExp(event.getExpLevelCost() * 2); // Refund double to cover lost XP
        }
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack result = event.getResult();
        if (result == null) return;
        if (isSword(result)) {
            for (Enchantment banned : BANNED_ENCHANTS) {
                if (result.containsEnchantment(banned)) {
                    ItemStack cleaned = result.clone();
                    cleaned.removeEnchantment(banned);
                    event.setResult(cleaned);
                }
            }
        }
    }

    // ===== WEAPON SPECIAL EFFECTS =====
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;
        Player attacker = (Player) event.getDamager();
        LivingEntity victim = (LivingEntity) event.getEntity();
        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        handleThorHammer(attacker, victim, weapon);
        handleDragonChestplate(attacker, victim);
        if (victim instanceof Player) {
            setCombatTimer(attacker.getUniqueId());
            setCombatTimer(((Player) victim).getUniqueId());
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

    private boolean isCoralSeaTrident(ItemStack item) {
        if (item == null || item.getType() != Material.TRIDENT) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() && meta.getDisplayName().equals(CORAL_TRIDENT_NAME);
    }

    // ===== COMBAT SYSTEM =====
    private void setCombatTimer(UUID playerUUID) {
        combatTimers.put(playerUUID, System.currentTimeMillis());
    }

    private boolean isInCombat(UUID playerUUID) {
        Long combatTime = combatTimers.get(playerUUID);
        return combatTime != null && (System.currentTimeMillis() - combatTime) < (COMBAT_COOLDOWN_SECONDS * 1000);
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (isInCombat(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage("§cYou are in combat! You must wait " + COMBAT_COOLDOWN_SECONDS +
                    " seconds after your last PvP action to use commands.");
        }
    }

    // ===== PORTAL RESTRICTIONS =====
    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        World fromWorld = event.getFrom().getWorld();
        World toWorld = event.getTo() != null ? event.getTo().getWorld() : null;
        if (fromWorld == null || toWorld == null) return;
        if (!fromWorld.getName().equals("the_end") && toWorld.getName().equals("world_the_end")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cYou are not allowed to enter The End!");
        }
    }

    // ===== REPEATING TASKS =====
    private void startRepeatingTasks() {
        new BukkitRunnable() {
            @Override
            public void run() {
                combatElytraRemovalTask();
            }
        }.runTaskTimer(plugin, 1L, 1L);
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

    private boolean isSword(ItemStack item) {
        if (item == null) return false;
        return Arrays.asList(
                Material.WOODEN_SWORD,
                Material.STONE_SWORD,
                Material.IRON_SWORD,
                Material.GOLDEN_SWORD,
                Material.DIAMOND_SWORD,
                Material.NETHERITE_SWORD
        ).contains(item.getType());
    }
}