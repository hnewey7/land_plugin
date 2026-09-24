package io.hnewey.landplugin;

import org.bukkit.plugin.java.JavaPlugin;

public class LandPlugin extends JavaPlugin {
    private static LandPlugin instance;
    private static LandManager land_manager;
    private static LandBlockManager block_manager;

    public static LandPlugin getInstance() { return instance; }
    public static LandManager getLandManager() { return land_manager; }
    public static LandBlockManager getBlockManager() { return block_manager; }
    public String getPrefix() { return getConfig().getString("prefix", "&2&lLand &8&l| &r").replace("&", "§"); }

    @Override 
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        land_manager = new LandManager(instance);
        block_manager = new LandBlockManager();

        LandCommands land_commands = new LandCommands(instance, land_manager, block_manager);
        getCommand("land").setExecutor(land_commands);
    }

    @Override 
    public void onDisable() {
        if (land_manager != null) {
            land_manager.saveAllLand();
        }
    }
}
