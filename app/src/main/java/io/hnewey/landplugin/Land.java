package io.hnewey.landplugin;

import org.bukkit.Location;

import java.util.*;

public class Land {
    private record LandCorner(int x, int z) {}

    private static final int size = LandPlugin.getInstance().getConfig().getInt("land_size", 32);

    private final UUID owner;
    private final List<LandCorner> corners;

    private Set<UUID> trusted;

    Land(UUID owner, Location centre) {
        this.owner = owner;
        this.trusted = new HashSet<UUID>();
        this.corners = calculateCorners(centre);
    }

    // Land(LandGroup group, Location centre) {
    //     this.owner = group.getOwner();
    //     this.trusted = group.getTrusted();
    //     this.corners = calculateCorners(centre);
    // }

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

    private static List<LandCorner> calculateCorners(Location centre) {
        List<LandCorner> corners = new ArrayList<LandCorner>();

        if (size <= 0) {
            return corners; 
        }

        // Get centre coords
        int x = centre.getBlockX();
        int z = centre.getBlockZ();

        // Calculate corners
        corners.add(new LandCorner((int)x - size / 2, (int)z - size / 2));
        corners.add(new LandCorner((int)x - size / 2, (int)z + size / 2));
        corners.add(new LandCorner((int)x + size / 2, (int)z - size / 2));
        corners.add(new LandCorner((int)x + size / 2, (int)z + size / 2));

        return corners;
    }
}
