package io.hnewey.landplugin;

import org.bukkit.Location;

import java.util.*;
import java.util.stream.Collectors;
import java.io.File;


public class LandManager {
    private File land_folder;

    private Set<Land> land = new HashSet<Land>();
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
        land.add(new Land(owner, centre, size));
    }

    public void deleteLand(Land l) {
        land.remove(l);
    }

    public void deleteLand(UUID owner, Location centre) {
        // Get land by location
        Land l = getLandByCentre(centre);
        if (l == null) {
            return;
        }

        if (l.isOwner(owner)) {
            // Remove land
            land.remove(l);
            // Remove file
            File file = new File(land_folder, l.getUuid().toString() + ".yml");
            if (file != null) {
                file.delete();
            }
        }
    }

    public Land getLandByCentre(Location centre) {
        Optional<Land> potential = land.stream().filter(l -> l.getCentre().getBlockX() == centre.getBlockX() && l.getCentre().getBlockY() == centre.getBlockY() && l.getCentre().getBlockZ() == centre.getBlockZ()).findFirst();
        if (potential.isPresent()) {
            return potential.get();
        } else {
            return null;
        }
    }

    public Land getLandByLocation(Location loc) {
        Optional<Land> potential = land.stream().filter(l -> l.isInside(loc)).findFirst();
        if (potential.isPresent()) {
            return potential.get();
        } else {
            return null;
        }
    }

    public Set<Land> getLandByOwner(UUID owner) {
        return land.stream().filter(l -> l.isOwner(owner) == true).collect(Collectors.toSet());
    }

    public Set<Land> getLandByTrusted(UUID player) {
        return land.stream().filter(l -> l.isTrusted(player) == true).collect(Collectors.toSet());
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
        land.add(new Land(file));
    }

    public void saveAllLand() {
        for (Land l : land) {
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
}
