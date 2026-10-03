package io.hnewey.landplugin;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Land {
    public Land(Player owner, Chunk chunk) {
        this.owner = owner.getUniqueId();
        this.chunks.add(chunk);
    }

    public Land(File file) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        this.owner = UUID.fromString(config.getString("owner"));
        this.name = config.getString("name");

        List<String> trusted = config.getStringList("trusted");
        this.trusted = trusted.stream().map(t -> UUID.fromString(t)).collect(Collectors.toSet());

        this.chunks = loadChunks(config);
    }

    public final Player getOwner() { return Bukkit.getPlayer(owner); }
    public final Set<Chunk> getSets() { return chunks; }

    public final String getName() { return name; }
    public void setName(String name) {
        this.name = name;
    }

    public void addChunk(Chunk chunk) {
        chunks.add(chunk);
    }

    public void removeChunk(Chunk chunk) {
        chunks.remove(chunk);
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

        saveChunks(config, chunks);

        try {
            config.save(file);
        } catch (IOException e) {
            LandPlugin.getInstance().getLogger().severe("Failed to save claim file " + file.getName() + ": " + e.getMessage());
        }
    }

    private Set<Chunk> loadChunks(FileConfiguration config) {
        Set<Chunk> chunk_set = new HashSet<Chunk>();
        for (Map<?, ?> entry : config.getMapList("chunks")) {
            World world = Bukkit.getWorld((String) entry.get("world"));
            if (world == null) {
                continue;
            }

            int x = (int) entry.get("x");
            int y = (int) entry.get("y");
            int z = (int) entry.get("z");
            Block block = world.getBlockAt(x, y, z);
            if (block == null) {
                continue;
            }

            chunk_set.add(block.getChunk());
        }
        return chunk_set;
    }

    private void saveChunks(FileConfiguration config, Set<Chunk> chunks) {
        List<Map<String, Object>> chunk_list = new ArrayList<>();
        for (Chunk c : chunks) {
            Map<String, Object> map = new LinkedHashMap<>();
            // Set world
            map.put("world", c.getWorld().getName());
            // Get block
            Block b = c.getBlock(0, 0, 0);
            map.put("x", b.getX());
            map.put("y", b.getY());
            map.put("z", b.getZ());
            chunk_list.add(map);
        }
        config.set("chunks", chunk_list);
    }

    private final UUID owner;

    private Set<Chunk> chunks = new HashSet<Chunk>();
    private Set<UUID> trusted = new HashSet<UUID>();
    private String name;
}
