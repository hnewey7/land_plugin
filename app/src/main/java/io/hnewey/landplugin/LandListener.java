package io.hnewey.landplugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

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
        Location loc = e.getBlockPlaced().getLocation();

        if (!land_manager.hasAccess(player, loc)) {
            player.sendMessage("§4Unable to place block in another player's land!");
            e.setCancelled(true);
            return;
        }

        ItemStack item = (e.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND) ? player.getInventory().getItemInOffHand() : e.getItemInHand();

        if (block_manager.isLandBlock(item)) {
            plugin.getLogger().info(player.getName() + " has placed a land block.");

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
                player.sendMessage("§4Unable to claim land in another player's land!");
                e.setCancelled(true);
                return;
            }

            this.land_manager.createLand(player.getUniqueId(), loc);
            player.sendMessage("§2You have claimed land at: " + String.format("§7X: §a%d§7, Y: §a%d§7, Z: §a%d", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));

            e.getBlockPlaced().setBlockData(Material.AIR.createBlockData());
        }
    }

    @EventHandler 
    public void onBlockBreak(BlockBreakEvent e) {
        Player player = e.getPlayer();
        Location loc = e.getBlock().getLocation();

        if (!land_manager.hasAccess(player, loc)) {
            player.sendMessage("§4Unable to break blocks in another player's land!");
            e.setCancelled(true);
            return;
        }
    }

    @EventHandler 
    public void onBlockIgnite(BlockIgniteEvent e) {
        Player player = e.getPlayer();
        Location loc = e.getBlock().getLocation();

        if (!land_manager.hasAccess(player, loc)) {
            player.sendMessage("§4Unable to ignite blocks in another player's land!");
            e.setCancelled(true);
            return;
        }
    }

    @EventHandler 
    public void onSignChange(SignChangeEvent e) {
        Player player = e.getPlayer();
        Location loc = e.getBlock().getLocation();

        if (!land_manager.hasAccess(player, loc)) {
            player.sendMessage("§4Unable to change signs in another player's land!");
            e.setCancelled(true);
            return;
        }
    }

    @EventHandler 
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        Block block = e.getClickedBlock();
        Location loc = block.getLocation();

        if (block.getState() instanceof org.bukkit.block.Container) {
            if (!land_manager.hasAccess(player, loc)) {
                player.sendMessage("§4Unable to open containers in another player's land!");
                e.setCancelled(true);
                return;
            }
        } else {
            Material material = block.getType();
            String block_name = material.name();

            if (block_name.contains("DOOR") || block_name.contains("GATE") || block_name.contains("TRAPDOOR") || block_name.contains("BUTTON") || block_name.contains("LEVER") || block_name.contains("PLATE")) {
                if (!land_manager.hasAccess(player, loc)) {
                    player.sendMessage("§4Unable to perform action in another player's land!!");
                    e.setCancelled(true);
                    return;
                }
            } else if (e.getAction() == org.bukkit.event.block.Action.PHYSICAL && block_name.contains("FARMLAND")) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler 
    public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
        Player player = e.getPlayer();
        Entity entity = e.getRightClicked();
        Location loc = entity.getLocation();

        if (entity instanceof org.bukkit.entity.ItemFrame || entity instanceof org.bukkit.entity.ArmorStand || entity instanceof org.bukkit.entity.Animals || entity instanceof org.bukkit.entity.Villager) {
            if (!land_manager.hasAccess(player, loc)) {
                player.sendMessage("§4Unable to interact with entity in another player's land!");
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler 
    public void onBedEnter(PlayerBedEnterEvent e) {
        Player player = e.getPlayer();
        Location loc = e.getBed().getLocation();

        if (!land_manager.hasAccess(player, loc)) {
            player.sendMessage("§4Unable to enter a bed in another player's land!");
            e.setCancelled(true);
            return;
        }
    }

    @EventHandler 
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        Player player = e.getPlayer();
        Location loc = e.getBlock().getLocation();

        if (!land_manager.hasAccess(player, loc)) {
            player.sendMessage("§4Unable to empty a bucket in another player's land!");
            e.setCancelled(true);
            return;
        }
    }

    @EventHandler 
    public void onBucketFill(PlayerBucketFillEvent e) {
        Player player = e.getPlayer();
        Location loc = e.getBlock().getLocation();

        if (!land_manager.hasAccess(player, loc)) {
            player.sendMessage("§4Unable to fill a bucket in another player's land!");
            e.setCancelled(true);
            return;
        }
    }

    @EventHandler 
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent e) {
        Player player = e.getPlayer();
        Location loc = e.getRightClicked().getLocation();

        if (!land_manager.hasAccess(player, loc)) {
            player.sendMessage("§4Unable to manipulate armor stand in another player's land!");
            e.setCancelled(true);
            return;
        }
    }

    @EventHandler 
    public void onMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();

        // Get land
        Land before = land_manager.getLandByLocation(e.getFrom());
        Land after = land_manager.getLandByLocation(e.getTo());

        // Entering
        if (after != null && after != before) {
            // Get owner
            String owner_name = Bukkit.getOfflinePlayer(after.getOwner()).getName();

            // Get colour code to display
            String colour_code;
            if (after.isOwner(player.getUniqueId())) {
                colour_code = "§6";
            } else if (after.isTrusted(player.getUniqueId())) {
                colour_code = "§s";
            } else {
                colour_code = "§m";
            }

            player.sendMessage("§7You have entered the land of " + colour_code + owner_name);
            return;
        }

        // Leaving
        if (after == null && after != before) {
            // Get owner
            String owner_name = Bukkit.getOfflinePlayer(before.getOwner()).getName();

            // Get colour code to display
            String colour_code;
            if (before.isOwner(player.getUniqueId())) {
                colour_code = "§6";
            } else if (before.isTrusted(player.getUniqueId())) {
                colour_code = "§s";
            } else {
                colour_code = "§m";
            }

            player.sendMessage("§7You have left the land of " + colour_code + owner_name);
            return;
        }
    }

    @EventHandler 
    public void onTeleport(PlayerTeleportEvent e) {
        Player player = e.getPlayer();

        // Get land
        Land before = land_manager.getLandByLocation(e.getFrom());
        Land after = land_manager.getLandByLocation(e.getTo());

        // Entering
        if (after != null && after != before) {
            // Get owner
            String owner_name = Bukkit.getOfflinePlayer(after.getOwner()).getName();

            // Get colour code to display
            String colour_code;
            if (after.isOwner(player.getUniqueId())) {
                colour_code = "§6";
            } else if (after.isTrusted(player.getUniqueId())) {
                colour_code = "§s";
            } else {
                colour_code = "§m";
            }

            player.sendMessage("§7You have entered the land of " + colour_code + owner_name);
            return;
        }

        // Leaving
        if (after == null && after != before) {
            // Get owner
            String owner_name = Bukkit.getOfflinePlayer(before.getOwner()).getName();

            // Get colour code to display
            String colour_code;
            if (before.isOwner(player.getUniqueId())) {
                colour_code = "§6";
            } else if (before.isTrusted(player.getUniqueId())) {
                colour_code = "§s";
            } else {
                colour_code = "§m";
            }

            player.sendMessage("§7You have left the land of " + colour_code + owner_name);
            return;
        }
    }

    @EventHandler 
    public void onHangingPlace(HangingPlaceEvent e) {
        Player player = e.getPlayer();
        Location loc = e.getBlock().getLocation();

        if (!land_manager.hasAccess(player, loc)) {
            player.sendMessage("§4Unable to place hanging items in another player's land!");
            e.setCancelled(true);
            return;
        }
    }
    
    @EventHandler 
    public void onHangingBlockBreak(HangingBreakByEntityEvent e) {
        if (e.getRemover() instanceof Player player) {
            Location loc = player.getLocation();

            if (!land_manager.hasAccess(player, loc)) {
                player.sendMessage("§4Unable to break items in another player's land!");
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler 
    public void onPvE(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player player && !(e.getEntity() instanceof Monster)) {
            Entity other = e.getEntity();
            Location loc = other.getLocation();

            if (!land_manager.hasAccess(player, loc)) {
                player.sendMessage("§4Unable to damage entity in another player's land!");
                e.setCancelled(true);
                return;
            }
        } else if (e.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player player) {
            Entity other = e.getEntity();
            Location loc = other.getLocation();

            if (!land_manager.hasAccess(player, loc)) {
                player.sendMessage("§4Unable to damage entity in another player's land!");
                e.setCancelled(true);
                return;
            }
        }
    }
}
