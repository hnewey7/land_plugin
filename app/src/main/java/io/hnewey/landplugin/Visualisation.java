package io.hnewey.landplugin;

import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.scheduler.BukkitRunnable;

public class Visualisation implements Listener {
    Visualisation(LandPlugin land_plugin, LandManager land_manager, TrustedPlayerManager trusted_manager) {
        this.plugin = land_plugin;
        this.land_manager = land_manager;
        this.trusted_manager = trusted_manager;
        // Track players already online (e.g. after a /reload)
        for (Player p : Bukkit.getOnlinePlayers()) {
            active_players.add(p.getUniqueId());
        }
        // Start task to check player locations
        this.check_player_locations = createCheckPlayerLocationsTask();
        this.check_player_locations.runTaskTimer(land_plugin, 0L, 20L);
        // Start task to send block data continuously
        this.send_block_data = createSendBlockDataTask();
        this.send_block_data.runTaskTimer(land_plugin, 0L, 20L);
    }

    // On player join
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (p == null) {
            return;
        }
        active_players.add(p.getUniqueId());
    }

    // On player quit
    @EventHandler 
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (p == null) {
            return;
        }
        active_players.remove(p.getUniqueId());

        // Drop everything tracked for this player
        synchronized (mutex) {
            existing_blocks.remove(p.getUniqueId());
            visualised_land.remove(p.getUniqueId());
            player_updates.remove(p.getUniqueId());
            pending_reverts.removeIf(r -> r.player_id().equals(p.getUniqueId()));
        }
    }

    // On player interacting with a visualised block, show the original block again after a delay
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        // Only handle block clicks, and only once (the event fires for both hands)
        if (e.getClickedBlock() == null || e.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Location loc = e.getClickedBlock().getLocation();
        UUID player_id = e.getPlayer().getUniqueId();

        synchronized (mutex) {
            Map<Location, BlockData> player_blocks = existing_blocks.get(player_id);
            if (player_blocks == null || !player_blocks.containsKey(loc)) {
                return;
            }

            // Don't queue another revert while one is already waiting for this block
            if (!pending_reverts.add(new PendingRevert(player_id, loc))) {
                return;
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, new RevertBlock(player_id, loc), REVERT_DELAY);
    }

    // Task for checking location
    private BukkitRunnable createCheckPlayerLocationsTask() {
        return new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID online : active_players) {
                    Player player = Bukkit.getPlayer(online);
                    if (player == null) {
                        continue;
                    }

                    Location loc = player.getLocation();
                    if (loc == null) {
                        continue;
                    }

                    // Get all land to visualise
                    Set<Land> to_visualise = land_manager.getLandInRadius(loc, 5);

                    synchronized (mutex) {
                        Map<Land, Map<Location, BlockData>> visualised = visualised_land.computeIfAbsent(online, k -> new HashMap<>());

                        // Revert land that is visualised but no longer in range
                        for (Land l : new ArrayList<>(visualised.keySet())) {
                            if (!to_visualise.contains(l)) {
                                revertLand(online, l);
                            }
                        }

                        // Bring land in range up to date (new land, changed land or a different player height)
                        for (Land l : to_visualise) {
                            updateLand(online, player, l);
                        }
                    }
                }
            }
        };
    }

    // Task for sending block data continously
    private BukkitRunnable createSendBlockDataTask() {
        return new BukkitRunnable() {
            @Override
            public void run() {
                sendPendingUpdates();
            }
        };
    }

    private void sendPendingUpdates() {
        synchronized (mutex) {
            if (player_updates.isEmpty()) {
                return;
            }

            for (Map.Entry<UUID, Map<Location, BlockData>> entry : new HashMap<>(player_updates).entrySet()) {
                UUID player_id = entry.getKey();

                Player player = Bukkit.getPlayer(player_id);
                if (player == null) {
                    continue;
                }

                for (Map.Entry<Location, BlockData> update : entry.getValue().entrySet()) {
                    player.sendBlockChange(update.getKey(), update.getValue());
                }
                player_updates.remove(player_id);
            }
        }
    }

    // Java has no destructor, so the plugin calls this from onDisable. Stops the tasks and
    // puts every player's world back, as the fake blocks would otherwise stay on their
    // client until the chunks reload.
    public void shutdown() {
        check_player_locations.cancel();
        send_block_data.cancel();

        synchronized (mutex) {
            // Drop unsent draws so only the reverts are sent below
            player_updates.clear();
            pending_reverts.clear();

            for (UUID player_id : new ArrayList<>(visualised_land.keySet())) {
                Map<Land, Map<Location, BlockData>> visualised = visualised_land.get(player_id);
                for (Land l : new ArrayList<>(visualised.keySet())) {
                    revertLand(player_id, l);
                }
            }

            sendPendingUpdates();

            visualised_land.clear();
            existing_blocks.clear();
            active_players.clear();
        }
    }

    // Task for reverting a single block back to original after it was interacted with
    private class RevertBlock implements Runnable {
        RevertBlock(UUID player_id, Location loc) {
            this.player_id = player_id;
            this.loc = loc;
        }

        @Override
        public void run() {
            synchronized (mutex) {
                pending_reverts.remove(new PendingRevert(player_id, loc));
            }
            revert_block(player_id, loc);
        }

        private final UUID player_id;
        private final Location loc;
    }

    // Make what the player sees for a land match what it should be right now. Compares against
    // what was drawn last time, so only changes are sent (and nothing at all if nothing changed).
    private void updateLand(UUID player_id, Player player, Land land) {
        synchronized (mutex) {
            Map<Land, Map<Location, BlockData>> visualised = visualised_land.computeIfAbsent(player_id, k -> new HashMap<>());

            // Determine block type for visualisation
            BlockData alliance_block;
            if (land.isOwner(player_id)) {
                alliance_block = Material.GOLD_BLOCK.createBlockData();
            } else if (trusted_manager.isTrusted(land.getOwnerID(), player)) {
                alliance_block = Material.DIAMOND_BLOCK.createBlockData();
            } else {
                alliance_block = Material.REDSTONE_BLOCK.createBlockData();
            }

            // Work out every block that should be shown at the player's current height
            Map<Location, BlockData> wanted = new HashMap<>();
            for (ChunkCorner cc : land.getCorners()) {
                wanted.putAll(getCornerBlocks(player.getLocation(), cc, alliance_block));
            }

            Map<Location, BlockData> drawn = visualised.getOrDefault(land, Map.of());
            if (wanted.equals(drawn)) {
                return;
            }
            visualised.put(land, wanted);

            // Revert blocks that are no longer wanted (corner gone, or the height moved)
            Set<Location> stale = new HashSet<>(drawn.keySet());
            stale.removeAll(wanted.keySet());
            revertLocations(player_id, stale);

            // Draw blocks that are new or have changed
            for (Map.Entry<Location, BlockData> entry : wanted.entrySet()) {
                if (!entry.getValue().equals(drawn.get(entry.getKey()))) {
                    updateBlock(player_id, entry.getKey(), entry.getValue());
                }
            }
        }
    }

    // The blocks to show for this corner, without sending anything
    private Map<Location, BlockData> getCornerBlocks(Location player_loc, ChunkCorner corner, BlockData alliance_block) {
        // Get world
        World world = player_loc.getWorld();
        int player_height = player_loc.getBlockY();

        Map<Location, BlockData> blocks = new HashMap<>();

        // Corner is GLOWSTONE, at the correct height
        int corner_height = getAirTransition(world, corner.x(), corner.z(), player_height);
        blocks.put(new Location(world, corner.x(), corner_height, corner.z()), Material.GLOWSTONE.createBlockData());

        // Blocks surrounding the corner are the alliance block
        int corner_height_dx = getAirTransition(world, corner.x() + corner.dx(), corner.z(), player_height);
        int corner_height_dz = getAirTransition(world, corner.x(), corner.z() + corner.dz(), player_height);
        blocks.put(new Location(world, corner.x() + corner.dx(), corner_height_dx, corner.z()), alliance_block);
        blocks.put(new Location(world, corner.x(), corner_height_dz, corner.z() + corner.dz()), alliance_block);

        return blocks;
    }

    private void updateBlock(UUID player_id, Location loc, BlockData data) {
        synchronized(mutex) {
            // Get original block at location
            Block block = loc.getBlock();
            if (block == null) {
                return;
            }

            BlockData block_data = block.getBlockData();
            if (block_data == null) {
                return;
            }

            existing_blocks.computeIfAbsent(player_id, k -> new HashMap<>()).put(loc, block_data);

            // Send update for new data
            player_updates.computeIfAbsent(player_id, k -> new HashMap<>()).put(loc, data);
        }
    }

    // Revert every block visualised for a land, using the locations recorded when it was
    // visualised so the player's current height doesn't matter
    private void revertLand(UUID player_id, Land land) {
        synchronized (mutex) {
            Map<Land, Map<Location, BlockData>> visualised = visualised_land.get(player_id);
            if (visualised == null) {
                return;
            }

            Map<Location, BlockData> drawn = visualised.remove(land);
            if (drawn == null) {
                return;
            }

            revertLocations(player_id, new HashSet<>(drawn.keySet()));
        }
    }

    // Revert the given locations, except any still used by another visualised land (shared corners).
    // Removes those still in use from the set passed in.
    private void revertLocations(UUID player_id, Set<Location> locations) {
        synchronized (mutex) {
            Map<Land, Map<Location, BlockData>> visualised = visualised_land.get(player_id);
            if (visualised != null) {
                for (Map<Location, BlockData> other : visualised.values()) {
                    locations.removeAll(other.keySet());
                }
            }

            Map<Location, BlockData> player_blocks = existing_blocks.get(player_id);
            for (Location loc : locations) {
                revert_block(player_id, loc);
                if (player_blocks != null) {
                    player_blocks.remove(loc);
                }
            }
        }
    }

    private void revert_block(UUID player_id, Location loc) {
        synchronized (mutex) {
            // Get existing block
            Map<Location, BlockData> player_blocks = existing_blocks.get(player_id);
            BlockData existing = player_blocks == null ? null : player_blocks.get(loc);
            if (existing == null) {
                return;
            }
            // Add update for player
            player_updates.computeIfAbsent(player_id, k -> new HashMap<>()).put(loc, existing);
        }
    }

    private int getAirTransition(World world, int x, int z, int player_height) {
        int min_height = world.getMinHeight();
        int max_height = world.getMaxHeight() - 1;

        int height = Math.max(min_height, Math.min(player_height, max_height));

        if (world.getBlockAt(x, height, z).getType().isSolid()) {
            // Inside solid blocks, search upwards for the top of the solid column
            while (height < max_height && world.getBlockAt(x, height + 1, z).getType().isSolid()) {
                height++;
            }
        } else {
            // In air, search downwards for the first solid block
            while (height > min_height && !world.getBlockAt(x, height, z).getType().isSolid()) {
                height--;
            }
        }

        // The solid block at the air transition
        return height;
    }

    private static final long REVERT_DELAY = 60L;

    private final LandPlugin plugin;
    private final LandManager land_manager;
    private final TrustedPlayerManager trusted_manager;
    private final BukkitRunnable check_player_locations;
    private final BukkitRunnable send_block_data;

    private final Object mutex = new Object();

    private record PendingRevert(UUID player_id, Location loc) {}

    private Set<PendingRevert> pending_reverts = new HashSet<PendingRevert>();

    private List<UUID> active_players = new ArrayList<UUID>();
    // Pending block changes to send per player, keyed by location so the last write wins
    private Map<UUID, Map<Location, BlockData>> player_updates = new HashMap<UUID, Map<Location, BlockData>>();
    // Original block data at each location, per player viewing the visualisation
    private Map<UUID, Map<Location, BlockData>> existing_blocks = new HashMap<UUID, Map<Location, BlockData>>();
    // Locations changed for each land currently visualised, per player
    private Map<UUID, Map<Land, Map<Location, BlockData>>> visualised_land = new HashMap<UUID, Map<Land, Map<Location, BlockData>>>();
}
