package net.macecontrol.utils;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;

public class DisableBedBombing implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();

        // Check if player is right-clicking a bed
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK &&
                event.getClickedBlock() != null &&
                isBed(event.getClickedBlock().getType())) {

            // Check if player is in Nether or End
            if (world.getEnvironment() == World.Environment.NETHER ||
                    world.getEnvironment() == World.Environment.THE_END) {

                // Cancel the event to prevent bed explosion
                event.setCancelled(true);

            }
        }
    }

    /**
     * Check if the material is a bed
     */
    private boolean isBed(Material material) {
        switch (material) {
            case WHITE_BED:
            case ORANGE_BED:
            case MAGENTA_BED:
            case LIGHT_BLUE_BED:
            case YELLOW_BED:
            case LIME_BED:
            case PINK_BED:
            case GRAY_BED:
            case LIGHT_GRAY_BED:
            case CYAN_BED:
            case PURPLE_BED:
            case BLUE_BED:
            case BROWN_BED:
            case GREEN_BED:
            case RED_BED:
            case BLACK_BED:
                return true;
            default:
                return false;
        }
    }
}