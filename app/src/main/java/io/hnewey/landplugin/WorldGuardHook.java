package io.hnewey.landplugin;

import org.bukkit.Location;
import org.bukkit.World;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;

public class WorldGuardHook {
    public static boolean isInWorldRegion(Location loc) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        return query.getApplicableRegions(BukkitAdapter.adapt(loc)).size() > 0;
    }

    public static boolean isOverlappingWorldRegion(Location loc, int size) {
        World world = loc.getWorld();
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager region_manager = container.get(BukkitAdapter.adapt(world));

        if (region_manager == null) {
            return false;
        }

        BlockVector3 min = BlockVector3.at(loc.getBlockX() - size, world.getMinHeight(), loc.getBlockZ() - size);
        BlockVector3 max = BlockVector3.at(loc.getBlockX() + size, world.getMaxHeight(), loc.getBlockZ() + size);
        ProtectedRegion query_region = new ProtectedCuboidRegion("__land_overlap_query__", min, max);

        return !region_manager.getApplicableRegions(query_region).getRegions().isEmpty();
    }
}
