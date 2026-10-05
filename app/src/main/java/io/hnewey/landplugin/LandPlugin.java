package io.hnewey.landplugin;

import org.bukkit.plugin.java.JavaPlugin;

public class LandPlugin extends JavaPlugin {
    private static LandPlugin instance;
    private static LandManager land_manager;
    private static LandBlockManager block_manager;
    private static TrustedPlayerManager trusted_player_manager;

    private boolean worldguard_enabled;

    public static LandPlugin getInstance() { return instance; }
    public static LandManager getLandManager() { return land_manager; }
    public static LandBlockManager getBlockManager() { return block_manager; }
    public static TrustedPlayerManager getTrustedPlayerManager() { return trusted_player_manager; }

    public boolean getWorldGuardEnabled() { return this.worldguard_enabled; }
    public String getPrefix() { return getConfig().getString("prefix", "&2&lLand &8&l| &r").replace("&", "§"); }

    @Override 
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        land_manager = new LandManager(this);
        block_manager = new LandBlockManager(this);
        trusted_player_manager = new TrustedPlayerManager(this);

        getServer().getPluginManager().registerEvents(new LandListener(this, land_manager, trusted_player_manager, block_manager), this);

        worldguard_enabled = getServer().getPluginManager().getPlugin("WorldGuard") != null;

        LandCommands land_commands = new LandCommands(this, land_manager, trusted_player_manager, block_manager);
        getCommand("land").setExecutor(land_commands);
    }

    @Override 
    public void onDisable() {
        if (land_manager != null) {
            land_manager.saveAllLand();
        }

        if (trusted_player_manager != null) {
            trusted_player_manager.saveTrusted();
        }
    }
}
