package io.hnewey.landplugin;

import org.bukkit.Location;

import java.util.*;
import java.util.stream.Collectors;
import java.io.File;


public class LandManager {
    private static File land_folder;

    private static Set<Land> land = new HashSet<Land>();

    LandManager(LandPlugin plugin) {
        land_folder = new File(plugin.getDataFolder(), "land");
        if (!land_folder.exists()) {
            land_folder.mkdirs();
        }

        loadAllLand();
    }

    public void createLand(UUID owner, Location centre) {
        land.add(new Land(owner, centre));
    }

    public void deleteLand(UUID owner, Location centre) {
        // Get land by location
        Land l = getLandByLocation(centre);

        if (l.isOwner(owner)) {
            land.remove(l);
        }
    }

    public Land getLandByLocation(Location centre) {
        Optional<Land> potential = land.stream().filter(l -> l.getCentre().equals(centre)).findFirst();
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
            loadLand(file);
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
        land.save(file);
    }
}
