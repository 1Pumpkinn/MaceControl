package net.macecontrol;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PluginDataManager {

    private final net.macecontrol.Main plugin;
    private final Map<UUID, Integer> playerMaceCount = new HashMap<>();
    private int totalMacesCrafted = 0;

    public PluginDataManager(net.macecontrol.Main plugin) {
        this.plugin = plugin;
    }

    // ===== MACE COUNTING =====
    public Map<UUID, Integer> getPlayerMaceCount() {
        return playerMaceCount;
    }

    public void incrementPlayerMaceCount(UUID playerId) {
        playerMaceCount.put(playerId, playerMaceCount.getOrDefault(playerId, 0) + 1);
    }

    public int getTotalMacesCrafted() {
        return totalMacesCrafted;
    }

    public void incrementTotalMaces() {
        totalMacesCrafted++;
    }

    }

