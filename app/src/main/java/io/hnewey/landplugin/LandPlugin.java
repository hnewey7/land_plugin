package io.hnewey.landplugin;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class LandPlugin extends JavaPlugin implements Listener {
    private static LandPlugin instance;
    private static LandManager land_manager;

    public static LandPlugin getInstance() { return instance; }
    public static LandManager getLandManager() { return land_manager; }
    public String getPrefix() { return getConfig().getString("prefix").replace("&", "§"); }

    @Override 
    public void onEnable() {
        instance = this;
        land_manager = new LandManager(instance);

        Bukkit.getPluginManager().registerEvents(this, this);

        LandCommands land_commands = new LandCommands(instance, land_manager);
        getCommand("land").setExecutor(land_commands);
    }

    // Load land size from config and initialise LandManager with it.
}
