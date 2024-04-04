package com.emicb.containertracker.listeners;

import com.emicb.containertracker.ContainerTracker;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Container;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.ArrayList;
import java.util.Set;
import java.util.logging.Logger;

public class InventoryCloseListener implements Listener {
    // Set up config
    private final FileConfiguration config = ContainerTracker.getInstance().getConfig();

    @EventHandler
    public void OnInventoryClose(InventoryCloseEvent event) {
        // Set up logger
        Logger log = Logger.getLogger("Minecraft");
        if (config.getBoolean("debug")) {
            log.info("[ContainerTracker] Inventory Close Event triggered");
        }

        Inventory inventory = event.getInventory();
        Player sender = (Player) event.getPlayer();
        // Exit if not viewing a container
        // TODO: Double chest not making it past this check?
        if (!(inventory.getHolder() instanceof Container)) {
            return;
        }

        Container container = (Container) inventory.getHolder();

        // Get data to store
        if (config.getBoolean("debug")) {
            log.info("[ContainerTracker] Logging Information:\n"
                    + "Timestamp: " + System.currentTimeMillis() + "\n"
                    + "Player: " + event.getPlayer().getName() + " : " + event.getPlayer().getUniqueId() + "\n"
                    + "Location: " + container.getLocation()
            );
        }

        // Get inventory contents
        ItemStack[] contents = inventory.getContents();
        if (contents.length == 0) {
            if (config.getBoolean("debug")) {
                log.info("[ContainerTracker] Container contents not logged: Container is empty");
            }
            return;
        }

        //Get inventory type
        InventoryType inventoryType = inventory.getType();
        if(inventoryType == InventoryType.BARREL || inventoryType == InventoryType.SHULKER_BOX){
            // Get puzzle id and type
            Scoreboard senderScoreboard = sender.getScoreboard();
            Objective puzzelIDObjective = senderScoreboard.getObjective("whimc.barrelbot.puzzle_id");
            if(puzzelIDObjective != null) {
                int puzzleID = puzzelIDObjective.getScore(sender).getScore();
                Scoreboard sbMain = Bukkit.getScoreboardManager().getMainScoreboard();
                for (Objective objectivesMain : sbMain.getObjectives()) {
                    for (String entries : sbMain.getEntries()) {
                        Score score = objectivesMain.getScore(entries);
                        if (score.getScore() == puzzleID) {
                            Scoreboard sbObjective = objectivesMain.getScoreboard();
                            for (Objective obj : sbObjective.getObjectives()) {
                                sender.sendMessage(obj.getName());
                            }
                            Objective puzzleTypeObjective = sbObjective.getObjective("whimc.barrelbot.puzzle_type_id");
                            if (puzzleTypeObjective != null) {
                                int puzzleType = puzzleTypeObjective.getScore(sender).getScore();
                                sender.sendMessage(String.valueOf(puzzleType));
                            } else {
                                log.info("[ContainerTracker] Puzzle type not found");
                            }
                        }
                    }
                }
                ContainerTracker.getInstance().getQueryer().storeNewInventory(sender, inventory, puzzleID, -1);
            } else {
                log.info("[ContainerTracker] Puzzle ID not found");
                ContainerTracker.getInstance().getQueryer().storeNewInventory(sender, inventory, -1, -1);
            }
        } else {
            log.info("[ContainerTracker] Container contents not logged: Container not barrel or shulker");
        }
    }
}
