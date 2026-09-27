package io.hnewey.landplugin;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class LandListener implements Listener {
    private final LandPlugin plugin;
    private final LandManager land_manager;
    private final LandBlockManager block_manager;

    public LandListener(LandPlugin plugin, LandManager land_manager, LandBlockManager block_manager) {
        this.plugin = plugin;
        this.land_manager = land_manager;
        this.block_manager = block_manager;
    }

    @EventHandler 
    public void onBlockPlace(BlockPlaceEvent e) {
        Player player = e.getPlayer();
        ItemStack item = (e.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND) ? player.getInventory().getItemInOffHand() : e.getItemInHand();
        
        plugin.getLogger().info(player.getName() + " has placed a land block.");

        if (block_manager.isLandBlock(item)) {
            Location loc = e.getBlockPlaced().getLocation();

            if (!land_manager.isWorldEnabled(loc.getWorld())) {
                player.sendMessage("§4Land claims are not enabled in this world!");
                e.setCancelled(true);
                return;
            }

            if (plugin.getWorldGuardEnabled() && WorldGuardHook.isInWorldRegion(loc)) {
                player.sendMessage("§4Land claims are not enabled in this region!");
                e.setCancelled(true);
                return;
            }

            if (land_manager.isForeignLand(player, loc)) {
                player.sendMessage("§4Unable to claim land as it overlaps with another player's land!");
                e.setCancelled(true);
                return;
            }

            this.land_manager.createLand(player.getUniqueId(), loc);
            player.sendMessage("§2You have claimed land at: " + String.format("§7X: §a%d§7, Y: §a%d§7, Z: §a%d", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
            
            e.getBlockPlaced().setBlockData(Material.AIR.createBlockData());
        }
    }
}
