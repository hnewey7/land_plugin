package io.hnewey.landplugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;

public record ChunkKey(String world, int x, int z) {
    /**
     * Location at the middle of this chunk, standing on the highest block there.
     * Returns null if the world isn't loaded.
     */
    public Location getCentre() {
        World w = Bukkit.getWorld(world);
        if (w == null) {
            return null;
        }

        int block_x = x * 16 + 8;
        int block_z = z * 16 + 8;
        int block_y = w.getHighestBlockYAt(block_x, block_z) + 1;

        return new Location(w, block_x, block_y, block_z);
    }

    public List<ChunkEdge> getEdges() {
        List<ChunkCorner> corners = getCorners();

        List<ChunkEdge> edges = new ArrayList<ChunkEdge>();
        // Corners are ordered (x,z), (x,z+1), (x+1,z), (x+1,z+1), so go round the outside
        edges.add(new ChunkEdge(corners.get(0), corners.get(1)));
        edges.add(new ChunkEdge(corners.get(1), corners.get(3)));
        edges.add(new ChunkEdge(corners.get(3), corners.get(2)));
        edges.add(new ChunkEdge(corners.get(2), corners.get(0)));
        return edges;
    }

    public List<ChunkCorner> getCorners() {
        List<ChunkCorner> corners = new ArrayList<ChunkCorner>();
        corners.add(new ChunkCorner(x << 4, z << 4, 1, 1));
        corners.add(new ChunkCorner(x << 4, (z + 1) << 4, 1, -1));
        corners.add(new ChunkCorner((x + 1) << 4, z << 4, -1, 1));
        corners.add(new ChunkCorner((x + 1) << 4, (z + 1) << 4, -1, -1));
        return corners;
    }
}
