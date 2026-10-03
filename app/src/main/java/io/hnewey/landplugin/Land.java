package io.hnewey.landplugin;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Land {
    Land(Player owner, ChunkKey chunk) {
        this.owner = owner.getUniqueId();
        this.chunks_keys.add(chunk);
    }

    Land(File file) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        this.owner = UUID.fromString(config.getString("owner"));
        this.name = config.getString("name");

        List<String> trusted = config.getStringList("trusted");
        this.trusted = trusted.stream().map(t -> UUID.fromString(t)).collect(Collectors.toSet());

        this.chunks_keys = loadChunks(config);
    }

    public final Player getOwner() { return Bukkit.getPlayer(owner); }
    public final Set<ChunkKey> getChunks_keys() { return chunks_keys; }

    public final String getName() { return name; }
    public void setName(String name) {
        this.name = name;
    }

    public void addChunk(ChunkKey chunk) {
        chunks_keys.add(chunk);
    }

    public void removeChunk(ChunkKey chunk) {
        chunks_keys.remove(chunk);
    }

    public void addPlayer(Player player) {
        trusted.add(player.getUniqueId());
    }

    public void removePlayer(Player player) {
        trusted.remove(player.getUniqueId());
    }

    public void saveLand(File file) {
        FileConfiguration config = new YamlConfiguration();

        config.set("owner", this.owner);
        config.set("trusted", this.trusted.toString());
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
            String world_name = (String) entry.get("world");
            int x = (int) entry.get("x");
            int z = (int) entry.get("z");
            // Create and add chunk key
            ChunkKey ck = new ChunkKey(world_name, x, z);
            chunk_set.add(ck);
        }
        return chunk_set;
    }

    private void saveChunks(FileConfiguration config, Set<ChunkKey> chunk_keys) {
        List<Map<String, Object>> chunk_list = new ArrayList<>();
        for (ChunkKey ck : chunk_keys) {
            Map<String, Object> map = new LinkedHashMap<>();
            // Set world
            map.put("world", ck.world());
            // Set chunk
            map.put("x", ck.x());
            map.put("z", ck.z());
            chunk_list.add(map);
        }
        config.set("chunk_keys", chunk_list);
    }

    private final UUID owner;

    private Set<ChunkKey> chunks_keys = new HashSet<ChunkKey>();
    private Set<UUID> trusted = new HashSet<UUID>();
    private String name;
}
