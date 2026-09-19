package io.hnewey.landplugin;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class LandPlugin extends JavaPlugin implements Listener {
    private static LandPlugin instance;

    public static LandPlugin getInstance() { return instance; }

    @Override 
    public void onEnable() {
        instance = this;
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler 
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage("Hello!");
    }

    // Load land size from config and initialise LandManager with it.
}
