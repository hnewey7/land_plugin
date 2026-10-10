package io.hnewey.landplugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Land {
    Land(Player owner, ChunkKey chunk) {
        this.id = UUID.randomUUID();
        this.owner = owner.getUniqueId();
        this.world = chunk.world();
        this.chunks_keys.add(chunk);
    }

    Land(UUID owner_id, String name, String world, Set<ChunkKey> chunks) {
        this.id = UUID.randomUUID();
        this.owner = owner_id;
        this.name = name;
        this.world = world;
        this.chunks_keys = new HashSet<ChunkKey>(chunks);
    }

    Land(File file) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        this.id = UUID.fromString(config.getString("id"));
        this.owner = UUID.fromString(config.getString("owner"));
        this.world = config.getString("world");
        this.name = config.getString("name", "");

        this.chunks_keys = loadChunks(config);
    }

    public final UUID getID() { return id; }
    public final Player getOwner() { return Bukkit.getPlayer(owner); }
    public final UUID getOwnerID() { return owner; }
    public final String getWorld() { return world; }

    public boolean isOwner(Player player) { return player.getUniqueId().equals(owner); } 
    public boolean isOwner(UUID player_id) { return player_id.equals(owner); }

    public final String getName() { return name; }
    public void setName(String name) {
        this.name = name;
    }

    public final Set<ChunkKey> getChunkKeys() { return chunks_keys; }
    public void setChunkKeys(Set<ChunkKey> chunk_keys) {
        this.chunks_keys = new HashSet<ChunkKey>(chunk_keys);
        this.corner_cache = null;
    }

    /**
     * Centre of the land, taken as the middle of the claimed chunk closest to the
     * average chunk position, so it is always inside the land even if it is oddly shaped.
     * Returns null if the land has no chunks or its world isn't loaded.
     */
    public Location getCentre() {
        if (chunks_keys.isEmpty()) {
            return null;
        }

        double avg_x = chunks_keys.stream().mapToInt(ChunkKey::x).average().orElse(0);
        double avg_z = chunks_keys.stream().mapToInt(ChunkKey::z).average().orElse(0);

        ChunkKey closest = Collections.min(chunks_keys, Comparator.comparingDouble(
                ck -> Math.pow(ck.x() - avg_x, 2) + Math.pow(ck.z() - avg_z, 2)));

        return closest.getCentre();
    }

    public void addChunk(ChunkKey chunk) {
        chunks_keys.add(chunk);
        corner_cache = null;
    }

    public void removeChunk(ChunkKey chunk) {
        chunks_keys.remove(chunk);
        corner_cache = null;
    }

    public void saveLand(File file) {
        FileConfiguration config = new YamlConfiguration();

        config.set("id", this.id.toString());
        config.set("owner", this.owner.toString());
        config.set("world", this.world);
        config.set("name", this.name);

        saveChunks(config, chunks_keys);

        try {
            config.save(file);
        } catch (IOException e) {
            LandPlugin.getInstance().getLogger().severe("Failed to save claim file " + file.getName() + ": " + e.getMessage());
        }
    }

    // Corners are checked often by the visualisation, so only recalculate when the chunks change
    public Set<ChunkCorner> getCorners() {
        if (corner_cache == null) {
            corner_cache = computeCorners();
        }
        return corner_cache;
    }

    private Set<ChunkCorner> computeCorners() {
        Map<ChunkCorner, List<ChunkEdge>> corner_edge_map = new HashMap<ChunkCorner, List<ChunkEdge>>();
        for (ChunkKey ck : chunks_keys) {
            List<ChunkEdge> chunk_edges = ck.getEdges();
            for (ChunkEdge ce : chunk_edges) {
                // Corners may not be in the map yet, so these can be null
                List<ChunkEdge> start_edges = corner_edge_map.get(ce.start);
                List<ChunkEdge> end_edges = corner_edge_map.get(ce.end);

                // Remove if already contains edge (it is shared with another chunk, so not a boundary)
                if (start_edges != null && end_edges != null && start_edges.contains(ce) && end_edges.contains(ce)) {
                    start_edges.remove(ce);
                    if (start_edges.isEmpty()) {
                        corner_edge_map.remove(ce.start);
                    }
                    end_edges.remove(ce);
                    if (end_edges.isEmpty()) {
                        corner_edge_map.remove(ce.end);
                    }
                    // Edge cancelled out, don't add it again
                    continue;
                }                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  

                // Add edges to map
                corner_edge_map.computeIfAbsent(ce.start, k -> new ArrayList<ChunkEdge>()).add(ce);
                corner_edge_map.computeIfAbsent(ce.end, k -> new ArrayList<ChunkEdge>()).add(ce);
            }
        }

        // Create new condensed corners
        Set<ChunkCorner> corners = new HashSet<ChunkCorner>();
        for (Map.Entry<ChunkCorner, List<ChunkEdge>> entry : corner_edge_map.entrySet()) {
            ChunkCorner cc = entry.getKey();
            List<ChunkEdge> connections = entry.getValue();

            // Find two edges meeting at an angle. A straight run of boundary has none,
            // so it is not a corner. Corners touching diagonally have four edges.
            ChunkEdge first = null;
            ChunkEdge second = null;
            for (int i = 0; i < connections.size() && first == null; i++) {
                for (int j = i + 1; j < connections.size(); j++) {
                    if (!connections.get(i).isAligned(connections.get(j))) {
                        first = connections.get(i);
                        second = connections.get(j);
                        break;
                    }
                }
            }

            if (first == null) {
                continue;
            }

            // Point the corner along both edges, away from itself. ChunkCorner is a record,
            // so each overlay returns a new corner.
            corners.add(cc.overlay_direction(first.awayFrom(cc)).overlay_direction(second.awayFrom(cc)));
        }
        return corners;
    }
 
    private Set<ChunkKey> loadChunks(FileConfiguration config) {
        Set<ChunkKey> chunk_set = new HashSet<ChunkKey>();
        for (Map<?, ?> entry : config.getMapList("chunks")) {
            // Get details
            int x = ((Number) entry.get("x")).intValue();
            int z = ((Number) entry.get("z")).intValue();
            // Create and add chunk key
            ChunkKey ck = new ChunkKey(this.world, x, z);
            chunk_set.add(ck);
        }
        return chunk_set;
    }

    private void saveChunks(FileConfiguration config, Set<ChunkKey> chunk_keys) {
        List<Map<String, Object>> chunk_list = new ArrayList<>();
        for (ChunkKey ck : chunk_keys) {
            Map<String, Object> map = new LinkedHashMap<>();
            // Set chunk
            map.put("x", ck.x());
            map.put("z", ck.z());
            chunk_list.add(map);
        }
        config.set("chunks", chunk_list);
    }

    private Set<ChunkCorner> corner_cache = null;

    private final UUID id;
    private final UUID owner;
    private final String world;

    private Set<ChunkKey> chunks_keys = new HashSet<ChunkKey>();
    private String name = "";
}
