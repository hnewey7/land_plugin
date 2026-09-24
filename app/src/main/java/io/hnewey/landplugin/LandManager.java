package io.hnewey.landplugin;

import org.bukkit.Chunk;
import org.bukkit.Location;

import java.util.*;
import java.util.stream.Collectors;
import java.io.File;


public class LandManager {
    private File land_folder;

    private record ChunkKey(UUID world_id, int chunk_x, int chunk_z) {}

    private Map<UUID, Set<Land>> land_by_owner = new HashMap<UUID, Set<Land>>();
    private Map<ChunkKey, Set<Land>> land_by_chunk = new HashMap<ChunkKey, Set<Land>>();
    private Set<Land> all_land = new HashSet<>();

    private final int size;

    LandManager(LandPlugin plugin) {
        land_folder = new File(plugin.getDataFolder(), "land");
        if (!land_folder.exists()) {
            land_folder.mkdirs();
        }

        int size = plugin.getConfig().getInt("land_size", 32);
        if (size < 16) {
            size = 16;
        }
        this.size = size;

        loadAllLand();
    }

    public void createLand(UUID owner, Location centre) {
        // Create land object and add to stores
        Land l = new Land(owner, centre, size);
        addLand(l);
    }

    private void addLand(Land l) {
        // Update land by owner
        land_by_owner.computeIfAbsent(l.getOwner(), k -> new HashSet<>()).add(l);

        // Update land by chunk
        Set<ChunkKey> chunks = getChunks(l);
        for (ChunkKey ck : chunks) {
            land_by_chunk.computeIfAbsent(ck, k -> new HashSet<>()).add(l);
        }

        // Update all land
        all_land.add(l);
    }

    public void deleteLand(Land l) {
        // Remove from land by owner
        Set<Land> land_set = land_by_owner.get(l.getOwner());
        if (land_set != null) {
            if (land_set.size() == 1) {
                land_by_owner.remove(l.getOwner());
            } else {
                land_set.remove(l);
            }
        }

        // Remove from land by chunks
        Set<ChunkKey> chunks = getChunks(l);
        for (ChunkKey ck : chunks) {
            land_by_chunk.remove(ck);
        }

        // Remove from all land
        all_land.remove(l);

        // Remove file
        File file = new File(land_folder, l.getUuid().toString() + ".yml");
        if (file != null) {
            file.delete();
        }
    }

    public Set<Land> getAllLand() {
        return all_land;
    }

    public Land getLandByLocation(Location loc) {
        // Get chunk key from location
        ChunkKey ck = new ChunkKey(loc.getWorld().getUID(), loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
        for (Land l : land_by_chunk.getOrDefault(ck, Set.of())) {
            if (l.isInside(loc)) {
                return l;
            }
        }
        return null;
    }

    public Set<Land> getLandByOwner(UUID owner) {
        return Collections.unmodifiableSet(land_by_owner.getOrDefault(owner, Set.of()));
    }

    private void loadAllLand() {
        File[] files = land_folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }

        for (File file : files) {
            try {
                loadLand(file);
            }
            catch (RuntimeException e) {
                LandPlugin.getInstance().getLogger().warning("Skipping corrup land file " + file.getName() + ": " + e);
            }
        }
    }

    private void loadLand(File file) {
        // Create land object and add to stores
        Land l = new Land(file);
        addLand(l);
    }

    public void saveAllLand() {
        for (Land l : getAllLand()) {
            saveLand(l);
        }
    }

    private void saveLand(Land land) {
        if (land == null) {
            return;
        }

        File file = new File(land_folder, land.getUuid().toString() + ".yml");
        if (file != null) {
            land.save(file);
        }
    }

    private Set<ChunkKey> getChunks(Land l) {
        // Get min and max chunks
        int min_cx = l.getMinX() >> 4;
        int min_cz = l.getMinZ() >> 4;
        int max_cx = l.getMaxX() >> 4;
        int max_cz = l.getMaxZ() >> 4;

        // Create chunk records
        Set<ChunkKey> chunks = new HashSet<>();
        for (int ci = min_cx; ci <= max_cx; ci++) {
            for (int cj = min_cz; cj <= max_cz; cj++) {
                chunks.add(new ChunkKey(l.getWorldUuid(), ci, cj));
            }
        }
        return chunks;
    }
}
