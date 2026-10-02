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
    public record LandCorner(int x, int z) {}

    private final UUID id;
    private final UUID owner;
    private final Location centre;
    private final int size;
    private final World world;
    private final List<LandCorner> corners;

    private UUID land_group;

    // Pre-group data, only read from old land files so it can be migrated into a group.
    private final Set<UUID> legacy_trusted;
    private final String legacy_name;

    Land(UUID owner, Location centre, int size) {
        this.id = UUID.randomUUID();
        this.owner = owner;
        this.centre = centre;
        this.size = size;
        this.world = centre.getWorld();
        this.corners = calculateCorners(centre, size);
        this.legacy_trusted = Set.of();
        this.legacy_name = null;
    }

    Land(File file) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        this.id = UUID.fromString(config.getString("id"));
        this.owner = UUID.fromString(config.getString("owner"));

        String group = config.getString("land_group");
        this.land_group = group == null ? null : UUID.fromString(group);

        List<String> trusted = config.getStringList("trusted");
        this.legacy_trusted = trusted.stream().map(t -> UUID.fromString(t)).collect(Collectors.toSet());
        this.legacy_name = config.getString("name");

        World world = Bukkit.getWorld(config.getString("world"));
        if (world == null) {
            world = Bukkit.getWorld("world");
        }
        this.world = world;

        this.centre = new Location(world, config.getInt("centre_x"), config.getInt("centre_y"), config.getInt("centre_z"));
        this.size = config.getInt("size");

        this.corners = calculateCorners(centre, size);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Land other && id.equals(other.id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }

    public UUID getUuid() { return this.id; }
    public UUID getWorldUuid() { return this.world.getUID(); }
    public World getWorld() { return this.world; }
    public UUID getOwner() { return this.owner; }
    public Location getCentre() { return this.centre; }
    public UUID getLandGroup() { return this.land_group; }

    public Set<UUID> getLegacyTrusted() { return this.legacy_trusted; }
    public String getLegacyName() { return this.legacy_name; }

    public int getMinX() { return corners.get(0).x(); }
    public int getMinZ() { return corners.get(0).z(); }
    public int getMaxX() { return corners.get(3).x(); }
    public int getMaxZ() { return corners.get(3).z(); }

    public void setLandGroup(UUID land_group) {
        this.land_group = land_group;
    }

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

    public static List<LandCorner> calculateCorners(Location centre, int size) {
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
        config.set("land_group", this.land_group == null ? null : this.land_group.toString());

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
