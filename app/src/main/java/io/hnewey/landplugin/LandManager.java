package io.hnewey.landplugin;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.world.chunk.Chunk;

import java.io.File;
import java.util.*;

public class LandManager {
    LandManager(LandPlugin plugin) {
        land_folder = new File(plugin.getDataFolder(), "land");
        if (!land_folder.exists()) {
            land_folder.mkdirs();
        }

        List<String> worlds = plugin.getConfig().getStringList("enabled_worlds");
        if (worlds.size() < 1) {
            worlds.add("world");
        }
        this.enabled_worlds = worlds;

        loadAllLand();
    }

    public static ChunkKey generateChunkKey(Location loc) {
        // Get world
        World world = loc.getWorld();
        if (world == null) {
            return null;
        }

        // Get chunk index
        int x = loc.getBlockX() >> 4;
        int z = loc.getBlockZ() >> 4;

        return new ChunkKey(world.getName(), x, z);
    }

    public boolean addLand(Player owner, Location loc) {
        // Get chunk key
        ChunkKey ck = generateChunkKey(loc);
        if (land_by_chunk.containsKey(ck)) {
            return false;
        }

        // Check for surrounding land
        List<ChunkKey> surrounding = generateSurroundingChunkKeys(loc);
        List<Land> surrounding_land = getLandByChunks(surrounding).stream().filter(l -> l.isOwner(owner)).toList();

        if (surrounding_land.isEmpty()) {
            // Create new land and update chunk map
            createLand(owner, loc);
            return true;
        } else {
            // Select land with name
            Land selected = surrounding_land.getFirst();
            for (Land candidate : surrounding_land) {
                if (!candidate.getName().isBlank()) {
                    selected = candidate;
                    break;
                }
            }

            selected.addChunk(ck);
            land_by_chunk.put(ck, selected);

            // Merge any other touching land into the selected land
            for (Land other : new LinkedHashSet<Land>(surrounding_land)) {
                if (other == selected) {
                    continue;
                }
                for (ChunkKey other_ck : other.getChunkKeys()) {
                    selected.addChunk(other_ck);
                    land_by_chunk.put(other_ck, selected);
                }
                all_land.remove(other);
                land_by_owner.get(owner.getUniqueId()).remove(other);
                deleteLandFile(other);
            }
            saveLand(selected);
            return true;
        }
    }

    public boolean removeLand(Location loc) {
        // Get chunk key
        ChunkKey ck = generateChunkKey(loc);
        
        // Get land
        Land land =  land_by_chunk.get(ck);
        if (land == null) {
            return false;
        }

        // Get land owner
        UUID owner_id = land.getOwnerID();

        // Remove chunk key from map and land
        land_by_chunk.remove(ck);
        land.removeChunk(ck);

        // Get components from land
        List<Set<ChunkKey>> components = findComponents(land.getChunkKeys());
        if (components.size() == 0) {
            all_land.remove(land);
            
            if (land_by_owner.get(owner_id).size() == 1) {
                land_by_owner.remove(owner_id);
            } else {
                land_by_owner.get(owner_id).remove(land);
            }

            deleteLandFile(land);
            return true;
        } else if (components.size() == 1) {
            saveLand(land);
            return true;
        } else {
            // Keep largest piece of land
            components.sort((a, b) -> b.size() - a.size());
            land.setChunkKeys(components.getFirst());
            saveLand(land);

            for (int i = 1; i < components.size(); i++) {
                // Keep split lands unnamed if the original was unnamed
                String new_name = land.getName().isBlank() ? "" : land.getName() + String.format("#%d", i);
                Land new_land = new Land(land.getOwnerID(), new_name, land.getWorld(), components.get(i));
                all_land.add(new_land);
                land_by_owner.computeIfAbsent(owner_id, k -> new HashSet<>()).add(new_land);

                for (ChunkKey component_ck : components.get(i)) {
                    land_by_chunk.put(component_ck, new_land);
                }
                saveLand(new_land);
            }
            return true;
        }
    }

    public Land getLand(ChunkKey ck) {
        return land_by_chunk.get(ck);
    }

    public Set<Land> getLandByOwner(Player owner) {
        return land_by_owner.get(owner.getUniqueId());
    }

    public void saveAllLand() {
        for (Land l : all_land) {
            saveLand(l);
        }
    }

    public boolean hasAccess(Player player, Location loc) {
        ChunkKey ck = generateChunkKey(loc);
        Land l = getLand(ck);
        if (l == null) {
            return true;
        }
        return l.isOwner(player) || LandPlugin.getTrustedPlayerManager().isTrusted(l.getOwnerID(), player);
    }

    public boolean isOwner(Player player, Location loc) {
        ChunkKey ck = generateChunkKey(loc);
        Land l = getLand(ck);
        if (l == null) {
            return false;
        }
        return l.isOwner(player);
    }

    public boolean isWorldEnabled(World world) {
        return this.enabled_worlds.contains(world.getName());
    }

    public boolean isForeignLand(Player player, Location loc) {
        ChunkKey ck = generateChunkKey(loc);
        Land l = land_by_chunk.get(ck);
        if (l == null) {
            return false;
        }

        return !l.isOwner(player);
    }

    public List<String> generateLandMap(Player player, int radius_x, int radius_z) {
        ChunkKey centre = generateChunkKey(player.getLocation());
        List<String> result = new ArrayList<String>();

        for (int dz = -radius_z; dz <= radius_z; dz++) {
            StringBuilder row = new StringBuilder();
            for (int dx = -radius_x; dx <= radius_x; dx++) {
                ChunkKey ck = new ChunkKey(centre.world(), centre.x() + dx, centre.z() + dz);
                Land land = getLand(ck);
                
                if (dx == 0 && dz == 0) {
                    row.append("§f+");
                } else if (land == null) {
                    row.append("§8\\");
                } else if (land.isOwner(player)) {
                    row.append("§6\\");
                } else if (LandPlugin.getTrustedPlayerManager().isTrusted(land.getOwnerID(), player)) {
                    row.append("§b\\");
                } else {
                    row.append("§c\\");
                }
            }
            result.add(row.toString());
        }
        return result;
    }

    private void loadAllLand() {
        File[] files = land_folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                try {
                    loadLand(file);
                }
                catch (RuntimeException e) {
                    LandPlugin.getInstance().getLogger().warning("Skipping corrupt land file " + file.getName() + ": " + e);
                }
            }
        }
    }

    private void loadLand(File file) {
        Land l = new Land(file);
        if (l.getChunkKeys().isEmpty()) {
            // Nothing to claim, discard the stale file
            file.delete();
            return;
        }
        all_land.add(l);
        land_by_owner.computeIfAbsent(l.getOwnerID(), k -> new HashSet<>()).add(l);
        for (ChunkKey ck : l.getChunkKeys()) {
            land_by_chunk.put(ck, l);
        }
    }

    private void createLand(Player owner, Location loc) {
        ChunkKey ck = generateChunkKey(loc);
        Land land = new Land(owner, ck);
        land_by_chunk.put(ck, land);
        land_by_owner.computeIfAbsent(owner.getUniqueId(), k -> new HashSet<>()).add(land);
        all_land.add(land);
        saveLand(land);
    }

    private void saveLand(Land l) {
        if (l == null) {
            return;
        }

        File file = new File(land_folder, l.getID().toString() + ".yml");
        l.saveLand(file);
    }

    private void deleteLandFile(Land l) {
        File file = new File(land_folder, l.getID().toString() + ".yml");
        file.delete();
    }

    private List<Set<ChunkKey>> findComponents(Set<ChunkKey> chunks) {
        List<Set<ChunkKey>> result = new ArrayList<Set<ChunkKey>>();
        Set<ChunkKey> unvisited = new HashSet<ChunkKey>(chunks);

        // Check all chunks
        while (!unvisited.isEmpty()) {
            Set<ChunkKey> component = new HashSet<ChunkKey>(); // Connected component
            Deque<ChunkKey> queue = new ArrayDeque<ChunkKey>(); // Queue to search for current component
            ChunkKey start = unvisited.iterator().next();
            unvisited.remove(start);
            queue.add(start);

            // Check all connected chunk keys for more connections, removing
            // from unvisited as you check.
            while (!queue.isEmpty()) {
                ChunkKey current = queue.poll();
                component.add(current);
                for (ChunkKey ck : generateSurroundingChunkKeys(current)) {
                    if (unvisited.remove(ck)) {
                        queue.add(ck);
                    }
                }
            }
            result.add(component);
        }
        return result;
    }

    private static List<ChunkKey> generateSurroundingChunkKeys(ChunkKey ck) {
        List<ChunkKey> surrounding = new ArrayList<ChunkKey>();
        surrounding.add(new ChunkKey(ck.world(), ck.x() + 1, ck.z()));
        surrounding.add(new ChunkKey(ck.world(), ck.x() - 1, ck.z()));
        surrounding.add(new ChunkKey(ck.world(), ck.x(), ck.z() + 1));
        surrounding.add(new ChunkKey(ck.world(), ck.x(), ck.z() - 1));
        return surrounding;
    }

    private static List<ChunkKey> generateSurroundingChunkKeys(Location loc) {
        ChunkKey ck = generateChunkKey(loc);
        return generateSurroundingChunkKeys(ck);
    }

    private List<Land> getLandByChunks(List<ChunkKey> chunks) {
        List<Land> land_list = new ArrayList<Land>();
        for (ChunkKey ck : chunks) {
            Land land = land_by_chunk.get(ck);
            if (land != null) {
                land_list.add(land);
            }
        }
        return land_list;
    }

    private File land_folder;

    private final List<String> enabled_worlds;

    private Map<ChunkKey, Land> land_by_chunk = new HashMap<ChunkKey, Land>();
    private Map<UUID, Set<Land>> land_by_owner = new HashMap<UUID, Set<Land>>();
    private Set<Land> all_land = new HashSet<Land>();
}
