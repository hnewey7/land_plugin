package io.hnewey.landplugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class LandGroup {
    private final UUID id;
    private final UUID owner;

    private String name;

    private final Set<UUID> land_set = new HashSet<UUID>();
    private final Set<UUID> trusted = new HashSet<UUID>();

    public LandGroup(UUID owner, Land land) {
        this.id = UUID.randomUUID();
        this.owner = owner;
        this.land_set.add(land.getUuid());
    }

    LandGroup(File file) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        this.id = UUID.fromString(config.getString("id"));
        this.owner = UUID.fromString(config.getString("owner"));
        this.name = config.getString("name");

        for (String l : config.getStringList("lands")) {
            this.land_set.add(UUID.fromString(l));
        }
        for (String t : config.getStringList("trusted")) {
            this.trusted.add(UUID.fromString(t));
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LandGroup other && id.equals(other.getID());
    }

    @Override
    public int hashCode() { return id.hashCode(); }

    public UUID getID() { return id; }
    public UUID getOwner() { return owner; }
    public boolean isOwner(UUID player) { return owner.equals(player); }

    public String getName() { return name; }
    public boolean hasName() { return name != null && !name.isEmpty(); }
    public void setName(String name) { this.name = name; }

    public Set<UUID> getLandIds() { return Collections.unmodifiableSet(land_set); }
    public boolean isEmpty() { return land_set.isEmpty(); }

    public void addLand(Land land) {
        land_set.add(land.getUuid());
    }

    public void removeLand(Land land) {
        land_set.remove(land.getUuid());
    }

    public void removeLand(UUID land_id) {
        land_set.remove(land_id);
    }

    // Merges lands and trusted players. Keeps this group's name unless it has none.
    public void mergeLandGroup(LandGroup other) {
        land_set.addAll(other.land_set);
        trusted.addAll(other.trusted);
        if (!hasName() && other.hasName()) {
            name = other.name;
        }
    }

    public boolean isTrusted(UUID player) { return trusted.contains(player); }
    public void addTrusted(UUID player) { trusted.add(player); }
    public void removeTrusted(UUID player) { trusted.remove(player); }

    public void save(File file) {
        FileConfiguration config = new YamlConfiguration();

        config.set("id", id.toString());
        config.set("owner", owner.toString());
        config.set("name", name);
        config.set("lands", land_set.stream().map(UUID::toString).collect(Collectors.toList()));
        config.set("trusted", trusted.stream().map(UUID::toString).collect(Collectors.toList()));

        try {
            config.save(file);
        } catch (IOException e) {
            LandPlugin.getInstance().getLogger().severe("Failed to save land group file " + file.getName() + ": " + e.getMessage());
        }
    }
}
