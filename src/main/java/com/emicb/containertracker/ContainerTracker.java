package com.emicb.containertracker;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.emicb.containertracker.commands.PuzzleProgressCommand;
import com.emicb.containertracker.commands.ToggleDebug;
import com.emicb.containertracker.listeners.InventoryCloseListener;
import com.emicb.containertracker.listeners.PlayerInteractListener;
import com.emicb.containertracker.utils.sql.Queryer;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.logging.Logger;

public final class ContainerTracker extends JavaPlugin {
    private static ContainerTracker plugin;
    private HashMap<World, HashMap<Player, HashMap<String, Integer>>> puzzleProgress;
    private HashMap<Player, Integer> playerCurrentPuzzle;
    private Queryer queryer;
    private Logger log;

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        PluginManager pluginManager = Bukkit.getPluginManager();
        plugin = this;

        // Set up logger
        log = Logger.getLogger("Minecraft");

        this.queryer = new Queryer(this, q -> {
            // If we couldn't connect to the database disable the plugin
            if (q == null) {
                log.severe("[ContainerTracker] Plugin Disabled: could not establish MySQL connection!");
                return;
            }
        });

        // Instantiate puzzle tracking hashmap
        puzzleProgress = new HashMap<>();
        playerCurrentPuzzle = new HashMap<>();
        // Link listeners
        pluginManager.registerEvents(new InventoryCloseListener(), this);
        pluginManager.registerEvents(new PlayerInteractListener(), this);
        registerChatListener();
        // Link commands
        // TODO: change command structure to sub-commands
        getServer().getPluginCommand("ct-toggle-debug").setExecutor(new ToggleDebug());
        getServer().getPluginCommand("puzzle-progress").setExecutor(new PuzzleProgressCommand(this));
        // TODO: add enable / disable logging commands

        log.info("[ContainerTracker] Started successfully!");
    }
    /**
     * Defines system chat listener for barrelbot completion
     */
    public void registerChatListener(){
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Server.SYSTEM_CHAT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                final String BARRELBOTFLAG = "[Barrelbot]";
                final String JSONMESSAGEKEY = "extra";
                final String JSONTEXTKEY = "text";

                String message = "";
                Player recipient = event.getPlayer();
                try {
                    message = event.getPacket().getChatComponents().getValues().get(0).getJson();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if(message.contains(BARRELBOTFLAG)) {
                    log.info("[ContainerTracker] Chat Event triggered");
                    JsonObject jsonMessage;
                    jsonMessage = JsonParser.parseString(message).getAsJsonObject();
                    JsonElement jsonElement = jsonMessage.get(JSONMESSAGEKEY);
                    if(jsonElement != null) {
                        JsonArray jsonArray = jsonElement.getAsJsonArray();
                        //Has username in proper location
                        if(recipient != null) {
                            JsonElement puzzleNameElement = jsonArray.get(jsonArray.size() - 2);
                            JsonObject jsonPuzzleIDObject = (JsonObject) puzzleNameElement;
                            JsonElement puzzleNameTextElement = jsonPuzzleIDObject.get(JSONTEXTKEY);
                            String puzzleNameString = puzzleNameTextElement.getAsString();
                            addPuzzleSuccess(recipient, puzzleNameString);
                            ContainerTracker.getInstance().getQueryer().storeNewBarrelbotOutcome(recipient, true, puzzleNameString);
                        }
                    } else {
                        //Found barrelbot message but not formatted how expected
                        log.info("[ContainerTracker] Barrelbot outcome not tracked, no text found.");
                    }
                }
            }
        });
    }
    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static ContainerTracker getInstance() {
        return plugin;
    }
    public Queryer getQueryer() {
        return this.queryer;
    }

    public void addPuzzleSuccess(Player player, String puzzleName){
        World world = player.getWorld();
        if(!puzzleProgress.containsKey(world)){
            puzzleProgress.put(world, new HashMap<>());
        }
        HashMap<Player, HashMap<String, Integer>> progrssOnWorld = puzzleProgress.get(world);
        if(!progrssOnWorld.containsKey(player)){
            progrssOnWorld.put(player, new HashMap<String, Integer>());
        }
        HashMap<String, Integer> playerProgress =  progrssOnWorld.get(player);
        playerProgress.putIfAbsent(puzzleName, 0);
        Integer timesFinished = playerProgress.get(puzzleName) + 1;
        playerProgress.put(puzzleName, timesFinished);
    }

    public HashMap<String, Integer> getPlayerPuzzleSuccess(Player player){
        World world = player.getWorld();
        if(!puzzleProgress.containsKey(world)){
            log.info("[ContainerTracker] No puzzle has been solved on this world!");
            return null;
        }
        if(!puzzleProgress.get(world).containsKey(player)){
            log.info("[ContainerTracker] Player has not solved a puzzle on this world yet!");
            return null;
        }
        return puzzleProgress.get(world).get(player);
    }
    public void updateCurrentPuzzle(Player player, Integer puzzleID){
        playerCurrentPuzzle.put(player, puzzleID);
    }

    public Integer getPlayerCurrentPuzzle(Player player){
        if(!playerCurrentPuzzle.containsKey(player)){
            log.info("[ContainerTracker] Player has not started a puzzle yet!");
            return null;
        }
        return playerCurrentPuzzle.get(player);
    }
}
