package io.hnewey.landplugin;

import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
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
        if (player == null) {
            return;
        }

        Block block = e.getBlockPlaced();
        if (block == null) {
            return;
        }
        
        Location loc = block.getLocation();
        if (loc == null) {
            return;
        }

        if (!land_manager.hasAccess(player, loc)) {
            player.sendMessage("§7Unable to place block in §canother player's§7 land!");
            e.setCancelled(true);
            return;
        }

        ItemStack item = (e.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND) ? player.getInventory().getItemInOffHand() : e.getItemInHand();

        if (block_manager.isLandBlock(item)) {
            plugin.getLogger().info(player.getName() + " has placed a land block.");

            if (!land_manager.isWorldEnabled(loc.getWorld())) {
                player.sendMessage("§7Land claims are §cnot enabled§7 in this world!");
                e.setCancelled(true);
                return;
            }

            if (plugin.getWorldGuardEnabled() && WorldGuardHook.isInWorldRegion(loc)) {
                player.sendMessage("§7Land claims are §cnot enabled§7 in this region!");
                e.setCancelled(true);
                return;
            }

            if (plugin.getWorldGuardEnabled() && WorldGuardHook.isOverlappingWorldRegion(loc, land_manager.getLandSize())) {
                player.sendMessage("§7Land claim §cnot allowed§7 to overlap this region!");
                e.setCancelled(true);
                return;
            }

            if (land_manager.isForeignLand(player, loc)) {
                player.sendMessage("§7Unable to claim land in §canother player's§7 land!");
                e.setCancelled(true);
                return;
            }

            if (land_manager.isOverlappingForeignLand(player, loc)) {
                player.sendMessage("§7Unable to claim land overlapping §canother player's§7 land!");
                e.setCancelled(true);
                return;
            }

            this.land_manager.createLand(player.getUniqueId(), loc);
            player.sendMessage("§7You have claimed land at: " + String.format("§7X: §6%d§7, Y: §6%d§7, Z: §6%d", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));

            e.getBlockPlaced().setBlockData(Material.AIR.createBlockData());
        }
    }

    @EventHandler 
    public void onBlockBreak(BlockBreakEvent e) {
        Player player = e.getPlayer();
        if (player == null) {
            return;
        }

        Block block = e.getBlock();
        if (block == null) {
            return;
        }
        
        Location loc = block.getLocation();
        if (loc == null) {
            return;
        }

        if (!land_manager.hasAccess(player, loc)) {
            player.sendMessage("§7Unable to break blocks in §canother player's§7 land!");
            e.setCancelled(true);
            return;
        }
    }

    @EventHandler 
    public void onBlockIgnite(BlockIgniteEvent e) {
        Player player = e.getPlayer();
        if (player == null) {
            return;
        }
        
        Block block = e.getBlock();
        if (block == null) {
            return;
        }
        
        Location loc = block.getLocation();
        if (loc == null) {
            return;
        }

        if (!land_manager.hasAccess(player, loc)) {
            player.sendMessage("§7Unable to ignite blocks in §canother player's§7 land!");
            e.setCancelled(true);
            return;
        }
    }

    @EventHandler 
    public void onSignChange(SignChangeEvent e) {
        Player player = e.getPlayer();
        if (player == null) {
            return;
        }
        
        Block block = e.getBlock();
        if (block == null) {
            return;
        }
        
        Location loc = block.getLocation();
        if (loc == null) {
            return;
        }

        if (!land_manager.hasAccess(player, loc)) {
            player.sendMessage("§7Unable to change signs in §canother player's§7 land!");
            e.setCancelled(true);
            return;
        }
    }

    @EventHandler 
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (player == null) {
            return;
        }
        
        Block block = e.getClickedBlock();
        if (block == null) {
            return;
        }
        
        Location loc = block.getLocation();
        if (loc == null) {
            return;
        }

        if (block.getState() instanceof org.bukkit.block.Container) {
            if (!land_manager.hasAccess(player, loc)) {
                player.sendMessage("§7Unable to open containers in §canother player's§7 land!");
                e.setCancelled(true);
                return;
            }
        } else {
            Material material = block.getType();
            String block_name = material.name();

            if (block_name.contains("DOOR") || block_name.contains("GATE") || block_name.contains("TRAPDOOR") || block_name.contains("BUTTON") || block_name.contains("LEVER") || block_name.contains("PLATE")) {
                if (!land_manager.hasAccess(player, loc)) {
                    player.sendMessage("§7Unable to perform action in §canother player's§7 land!");
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
        if (player == null) {
            return;
        }

        Entity entity = e.getRightClicked();
        if (entity == null) {
            return;
        }

        Location loc = entity.getLocation();
        if (loc == null) {
            return;
        }

        if (entity instanceof org.bukkit.entity.ItemFrame || entity instanceof org.bukkit.entity.ArmorStand || entity instanceof org.bukkit.entity.Animals || entity instanceof org.bukkit.entity.Villager) {
            if (!land_manager.hasAccess(player, loc)) {
                player.sendMessage("§7Unable to interact with entity in §canother player's§7 land!");
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler 
    public void onBedEnter(PlayerBedEnterEvent e) {
        Player player = e.getPlayer();
        if (player == null) {
            return;
        }

        Block bed = e.getBed();
        if (bed == null) {
            return;
        }

        Location loc = bed.getLocation();
        if (loc == null) {
            return;
        }

        if (!land_manager.hasAccess(player, loc)) {
            player.sendMessage("§7Unable to enter a bed in §canother player's§7 land!");
            e.setCancelled(true);
            return;
        }
    }

    @EventHandler 
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        Player player = e.getPlayer();
        if (player == null) {
            return;
        }

        Block block = e.getBlock();
        if (block == null) {
            return;
        }
        
        Location loc = block.getLocation();
        if (loc == null) {
            return;
        }

        if (!land_manager.hasAccess(player, loc)) {
            player.sendMessage("§7Unable to empty a bucket in §canother player's§7 land!");
            e.setCancelled(true);
            return;
        }
    }

    @EventHandler 
    public void onBucketFill(PlayerBucketFillEvent e) {
        Player player = e.getPlayer();
        
        Block block = e.getBlock();
        if (block == null) {
            return;
        }
        
        Location loc = block.getLocation();
        if (loc == null) {
            return;
        }

        if (!land_manager.hasAccess(player, loc)) {
            player.sendMessage("§7Unable to fill a bucket in §canother player's§7 land!");
            e.setCancelled(true);
            return;
        }
    }

    @EventHandler 
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent e) {
        Player player = e.getPlayer();
        
        ArmorStand armor = e.getRightClicked();
        if (armor == null) {
            return;
        }
        
        Location loc = armor.getLocation();
        if (loc == null) {
            return;
        }

        if (!land_manager.hasAccess(player, loc)) {
            player.sendMessage("§7Unable to manipulate armor stand in §canother player's§7 land!");
            e.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (e.getTo() == null) {
            return;
        }
        handleLandChange(e.getPlayer(), e.getFrom(), e.getTo());
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        if (e.getTo() == null) {
            return;
        }
        handleLandChange(e.getPlayer(), e.getFrom(), e.getTo());
    }

    // Sends enter/leave messages when the player moves between land groups (not between lands of the same group)
    private void handleLandChange(Player player, Location from, Location to) {
        Land before = land_manager.getLandByLocation(from);
        Land after = land_manager.getLandByLocation(to);

        LandGroup before_group = before == null ? null : land_manager.getGroup(before);
        LandGroup after_group = after == null ? null : land_manager.getGroup(after);

        if (Objects.equals(before_group, after_group) && (before == null) == (after == null)) {
            return;
        }

        // Entering
        if (after != null && !Objects.equals(before_group, after_group)) {
            player.sendMessage("§7You have entered " + describeLand(player, after));
            return;
        }

        // Leaving
        if (after == null && before != null) {
            player.sendMessage("§7You have left " + describeLand(player, before));
        }
    }

    private String describeLand(Player player, Land land) {
        String owner_name = Bukkit.getOfflinePlayer(land.getOwner()).getName();

        // Get colour code to display
        String colour_code;
        if (land.isOwner(player.getUniqueId())) {
            colour_code = "§6";
        } else if (land_manager.isTrusted(land, player.getUniqueId())) {
            colour_code = "§b";
        } else {
            colour_code = "§c";
        }

        String result = "the land of " + colour_code + owner_name;
        LandGroup group = land_manager.getGroup(land);
        if (group != null && group.hasName()) {
            result += " §8(§7" + group.getName() + "§8)";
        }
        return result;
    }

    @EventHandler 
    public void onHangingPlace(HangingPlaceEvent e) {
        Player player = e.getPlayer();
        
        Block block = e.getBlock();
        if (block == null) {
            return;
        }
        
        Location loc = block.getLocation();
        if (loc == null) {
            return;
        }

        if (!land_manager.hasAccess(player, loc)) {
            player.sendMessage("§7Unable to place hanging items in §canother player's§7 land!");
            e.setCancelled(true);
            return;
        }
    }
    
    @EventHandler 
    public void onHangingBlockBreak(HangingBreakByEntityEvent e) {
        if (e.getRemover() instanceof Player player) {
            Location loc = player.getLocation();
            if (loc == null) {
                return;
            }

            if (!land_manager.hasAccess(player, loc)) {
                player.sendMessage("§7Unable to break items in §canother player's§7 land!");
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler 
    public void onPvE(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player player && !(e.getEntity() instanceof Monster)) {
            Entity other = e.getEntity();
            if (other == null) {
                return;
            }

            Location loc = other.getLocation();
            if (loc == null) {
                return;
            }

            if (!land_manager.hasAccess(player, loc)) {
                player.sendMessage("§7Unable to damage entity in §canother player's§7 land!");
                e.setCancelled(true);
                return;
            }
        } else if (e.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player player) {
            Entity other = e.getEntity();
            if (other == null) {
                return;
            }

            Location loc = other.getLocation();
            if (loc == null) {
                return;
            }

            if (!land_manager.hasAccess(player, loc)) {
                player.sendMessage("§7Unable to damage entity in §canother player's§7 land!");
                e.setCancelled(true);
                return;
            }
        }
    }
}
