package io.hnewey.landplugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

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
}
