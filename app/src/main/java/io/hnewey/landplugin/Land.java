package io.hnewey.landplugin;

import org.bukkit.Bukkit;
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

    public final String getName() { return name; }
    public void setName(String name) {
        this.name = name;
    }

    public final Set<ChunkKey> getChunkKeys() { return chunks_keys; }
    public void setChunkKeys(Set<ChunkKey> chunk_keys) {
        this.chunks_keys = new HashSet<ChunkKey>(chunk_keys);
    }

    public void addChunk(ChunkKey chunk) {
        chunks_keys.add(chunk);
    }

    public void removeChunk(ChunkKey chunk) {
        chunks_keys.remove(chunk);
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

    private final UUID id;
    private final UUID owner;
    private final String world;

    private Set<ChunkKey> chunks_keys = new HashSet<ChunkKey>();
    private String name = "";
}
