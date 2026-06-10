package com.emicb.containertracker.listeners;

import com.emicb.containertracker.ContainerTracker;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.logging.Logger;

public class PlayerInteractListener implements Listener {
    // Set up config
    private final FileConfiguration config = ContainerTracker.getInstance().getConfig();

    // Set up logger
    private final Logger log = Logger.getLogger("Minecraft");
    private static final String PRESSURE_PLATE = "PRESSURE_PLATE";
    private static final String LEVER = "LEVER";
    private static final String BUTTON = "BUTTON";

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        String regionNames = getRegionNamesAt(player.getLocation());

        // Get the death cause
        String causeType = "UNKNOWN";
        if (player.getLastDamageCause() != null) {
            causeType = player.getLastDamageCause().getCause().toString();
        }

        String interactionType = "DEATH " + causeType;

        ContainerTracker.getInstance().getQueryer()
                .logNewPhysicalInteraction(player, interactionType, regionNames);

        if (config.getBoolean("debug")) {
            log.info("[ContainerTracker] " + player.getName() + " died (" + causeType + ") and it was logged.");
        }
    }

    @EventHandler
    public void onPlayerPunchPlayer(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        Player target = (Player) event.getEntity();

        String regionNames = getRegionNamesAt(player.getLocation());

        String interactionType = "PUNCH " + target.getName();

        ContainerTracker.getInstance().getQueryer().logNewPhysicalInteraction(player, interactionType, regionNames);

        if (config.getBoolean("debug")) {
            log.info("[ContainerTracker] " + player.getName() + " punched " + target.getName());
        }
    }

    @EventHandler
    public void OnPlayerInteract(PlayerInteractEvent event) {
        if (config.getBoolean("debug")) {
            log.info("[ContainerTracker] Interact Event triggered");
        }

        if (event.getClickedBlock() == null) {
            if (config.getBoolean("debug")) {
                /*log.info("[ContainerTracker] Interact Event: clicked air");*/
            }
            ContainerTracker.getInstance().getQueryer().logNewPhysicalInteraction(event.getPlayer(), event.getClickedBlock(), "");
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        Material blockMaterial = clickedBlock.getType();
        String blockName = blockMaterial.toString().toUpperCase();

        if (!(blockName.contains(PRESSURE_PLATE) || blockName.contains(LEVER) || blockName.contains(BUTTON))) {
            if (config.getBoolean("debug")) {
                /*log.info("[ContainerTracker] Interact Event ignored: action was " + blockName);*/
            }
            return;
        }

        String regionNames = getRegionNamesAt(clickedBlock.getLocation());

        if (config.getBoolean("debug")) {
            log.info("[ContainerTracker] Logging Information:\n"
                    + "Timestamp: " + System.currentTimeMillis() + "\n"
                    + "Player: " + event.getPlayer().getName() + " : " + event.getPlayer().getUniqueId() + "\n"
                    + "Location: " + event.getPlayer().getLocation() + "\n"
                    + "Block Type: " + event.getClickedBlock().getType() + "\n"
                    + "Region Name: " + regionNames + "\n"
            );
        }
        ContainerTracker.getInstance().getQueryer().logNewPhysicalInteraction(event.getPlayer(), event.getClickedBlock(), regionNames);
    }

    private String getRegionNamesAt(org.bukkit.Location bukkitLocation) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        Location adaptedLocation = BukkitAdapter.adapt(bukkitLocation);
        RegionQuery query = container.createQuery();
        ApplicableRegionSet set = query.getApplicableRegions(adaptedLocation);

        StringBuilder regionNamesBuilder = new StringBuilder();
        for (ProtectedRegion region : set) {
            regionNamesBuilder.append(region.getId()).append(" ");
        }
        return regionNamesBuilder.toString().trim();
    }
}
