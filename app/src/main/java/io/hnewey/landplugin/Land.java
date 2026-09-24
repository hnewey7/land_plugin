package io.hnewey.landplugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.*;
import java.util.stream.Collectors;
import java.io.File;
import java.io.IOException;

public class Land {
    private record LandCorner(int x, int z) {}

    private final UUID id;
    private final UUID owner;
    private final Location centre;
    private final int size;
    private final World world;
    private final List<LandCorner> corners;

    private Set<UUID> trusted;

    Land(UUID owner, Location centre, int size) {
        this.id = UUID.randomUUID();
        this.owner = owner;
        this.trusted = new HashSet<UUID>();
        this.centre = centre;
        this.size = size;
        this.world = centre.getWorld();
        this.corners = calculateCorners(centre);
    }

    Land(UUID owner, Set<UUID> trusted, Location centre, int size) {
        this.id = UUID.randomUUID();
        this.owner = owner;
        this.trusted = new HashSet<UUID>(trusted);
        this.centre = centre;
        this.size = size;
        this.world = centre.getWorld();
        this.corners = calculateCorners(centre);
    }

    Land(File file) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        this.id = UUID.fromString(config.getString("id"));
        this.owner = UUID.fromString(config.getString("owner"));

        List<String> trusted = config.getStringList("trusted");
        this.trusted = trusted.stream().map(t -> UUID.fromString(t)).collect(Collectors.toSet());

        World world = Bukkit.getWorld(config.getString("world"));
        if (world == null) {
            world = Bukkit.getWorld("world");
        }
        this.world = world;

        this.centre = new Location(world, config.getInt("centre_x"), config.getInt("centre_y"), config.getInt("centre_z"));
        this.size = config.getInt("size");

        this.corners = calculateCorners(centre);
    }

    // Land(LandGroup group, Location centre) {
    //     this.owner = group.getOwner();
    //     this.trusted = group.getTrusted();
    //     this.corners = calculateCorners(centre);
    // }

    public UUID getUuid() { return this.id; }
    public Location getCentre() { return this.centre; }

    public boolean isInside(Location loc) {
        // Get location coords
        int x = loc.getBlockX();
        int z = loc.getBlockZ();

        // Get claim corners
        LandCorner min_corner = corners.get(0);
        LandCorner max_corner = corners.get(3);

        return x >= min_corner.x() && x <= max_corner.x() && z >= min_corner.z() && z <= max_corner.z();
    }

    public boolean isOwner(UUID owner) { return this.owner.equals(owner); }
    public boolean isTrusted(UUID player) { return this.trusted.contains(player); }

    public void addTrusted(UUID player) { this.trusted.add(player); }
    public void removeTrusted(UUID player) { this.trusted.remove(player); }

    private List<LandCorner> calculateCorners(Location centre) {
        List<LandCorner> corners = new ArrayList<LandCorner>();

        // Get centre coords
        int x = centre.getBlockX();
        int z = centre.getBlockZ();

        // Calculate corners
        int min_x = x - size / 2;
        int min_z = z - size / 2;
        int max_x = min_x + size - 1;
        int max_z = min_z + size - 1;

        // Add corners
        corners.add(new LandCorner(min_x, min_z));
        corners.add(new LandCorner(min_x, max_z));
        corners.add(new LandCorner(max_x, min_z));
        corners.add(new LandCorner(max_x, max_z));

        return corners;
    }

    public void save(File file) {
        FileConfiguration config = new YamlConfiguration();
        
        config.set("id", this.id.toString());
        config.set("owner", this.owner.toString());
        config.set("trusted", this.trusted.stream().map(t -> t.toString()).collect(Collectors.toList()));

        config.set("world", this.world.getName());
        config.set("centre_x", this.centre.getBlockX());
        config.set("centre_y", this.centre.getBlockY());
        config.set("centre_z", this.centre.getBlockZ());
        config.set("size", this.size);

        try {
            config.save(file);
        } catch (IOException e) {
            LandPlugin.getInstance().getLogger().severe("Failed to save claim file " + file.getName() + ": " + e.getMessage());
        }
    }
}
