package io.hnewey.landplugin;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;
import java.io.File;


public class LandManager {
    private File land_folder;
    private File group_folder;

    private record ChunkKey(UUID world_id, int chunk_x, int chunk_z) {}

    private Map<UUID, Set<Land>> land_by_owner = new HashMap<UUID, Set<Land>>();
    private Map<ChunkKey, Set<Land>> land_by_chunk = new HashMap<ChunkKey, Set<Land>>();
    private Map<UUID, Land> land_by_id = new HashMap<UUID, Land>();
    private Map<UUID, LandGroup> land_groups = new HashMap<UUID, LandGroup>();
    private Set<Land> all_land = new HashSet<>();

    private final List<String> enabled_worlds;
    private final int size;

    LandManager(LandPlugin plugin) {
        land_folder = new File(plugin.getDataFolder(), "land");
        if (!land_folder.exists()) {
            land_folder.mkdirs();
        }

        group_folder = new File(plugin.getDataFolder(), "groups");
        if (!group_folder.exists()) {
            group_folder.mkdirs();
        }

        int size = plugin.getConfig().getInt("land_size", 32);
        if (size < 16) {
            size = 16;
        }
        this.size = size;

        List<String> worlds = plugin.getConfig().getStringList("enabled_worlds");
        if (worlds.size() < 1) {
            worlds.add("world");
        }
        this.enabled_worlds = worlds;

        loadAllLand();
    }

    public int getLandSize() { return size; }

    public boolean hasAccess(Player player, Location loc) {
        Land land = getLandByLocation(loc);
        if (land == null) {
            return true;
        }
        return land.isOwner(player.getUniqueId()) || isTrusted(land, player.getUniqueId());
    }

    public LandGroup getGroup(Land land) {
        UUID id = land.getLandGroup();
        return id == null ? null : land_groups.get(id);
    }

    public boolean isTrusted(Land land, UUID player) {
        LandGroup group = getGroup(land);
        return group != null && group.isTrusted(player);
    }

    public String getGroupName(Land land) {
        LandGroup group = getGroup(land);
        if (group != null && group.hasName()) {
            return group.getName();
        }
        return land.getWorld().getName();
    }

    public List<Land> getLandInGroup(LandGroup group) {
        List<Land> lands = new ArrayList<>();
        for (UUID id : group.getLandIds()) {
            Land l = land_by_id.get(id);
            if (l != null) {
                lands.add(l);
            }
        }
        return lands;
    }

    public List<LandGroup> getGroupsByOwner(UUID owner) {
        Set<LandGroup> groups = new LinkedHashSet<>();
        for (Land l : getLandByOwner(owner)) {
            LandGroup group = getGroup(l);
            if (group != null) {
                groups.add(group);
            }
        }
        return new ArrayList<>(groups);
    }

    public boolean isWorldEnabled(World world) {
        return this.enabled_worlds.contains(world.getName());
    }

    public boolean isForeignLand(Player player, Location loc) {
        Land land = getLandByLocation(loc);
        if (land == null) {
            return false;
        }
        return !land.isOwner(player.getUniqueId());
    }

    public boolean isOverlappingForeignLand(Player player, Location loc) {
        Set<Land> overlapping = getOverlappingLand(loc);
        if (overlapping.isEmpty()) {
            return false;
        }

        for (Land l : overlapping) {
            if (!l.isOwner(player.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    public void createLand(UUID owner, Location centre) {
        // Get overlapping land
        Set<Land> overlapping = getOverlappingLand(centre);

        // Get land groups of overlapping land
        Set<LandGroup> overlapping_groups = new LinkedHashSet<>();
        for (Land o : overlapping) {
            LandGroup group = getGroup(o);
            if (group != null) {
                overlapping_groups.add(group);
            }
        }

        // Create new land object
        Land l = new Land(owner, centre, size);

        LandGroup selected = null;
        if (overlapping_groups.isEmpty()) {
            selected = new LandGroup(owner, l);
            land_groups.put(selected.getID(), selected);
        } else {
            // Prefer the first land group with a name
            for (LandGroup group : overlapping_groups) {
                if (selected == null || (!selected.hasName() && group.hasName())) {
                    selected = group;
                }
            }

            // Add land to selected
            selected.addLand(l);

            // Merge other land groups into selected and repoint their land
            overlapping_groups.remove(selected);
            for (LandGroup group : overlapping_groups) {
                for (Land member : getLandInGroup(group)) {
                    member.setLandGroup(selected.getID());
                    saveLand(member);
                }
                selected.mergeLandGroup(group);
                land_groups.remove(group.getID());
                new File(group_folder, group.getID().toString() + ".yml").delete();
            }
        }

        l.setLandGroup(selected.getID());

        // Add to stores
        addLand(l);

        saveLand(l);
        saveGroup(selected);
    }

    private void addLand(Land l) {
        // Update land by owner
        land_by_owner.computeIfAbsent(l.getOwner(), k -> new HashSet<>()).add(l);

        // Update land by chunk
        Set<ChunkKey> chunks = getChunks(l);
        for (ChunkKey ck : chunks) {
            land_by_chunk.computeIfAbsent(ck, k -> new HashSet<>()).add(l);
        }

        // Update land by id
        land_by_id.put(l.getUuid(), l);

        // Update all land
        all_land.add(l);
    }

    public void deleteLand(Land l) {
        // Remove from land by owner
        Set<Land> land_set = land_by_owner.get(l.getOwner());
        if (land_set != null) {
            land_set.remove(l);
            if (land_set.isEmpty()) {
                land_by_owner.remove(l.getOwner());
            }
        }

        // Remove from land by chunks
        Set<ChunkKey> chunks = getChunks(l);
        for (ChunkKey ck : chunks) {
            Set<Land> chunk_land_set = land_by_chunk.get(ck);
            if (chunk_land_set == null) continue;
            chunk_land_set.remove(l);
            if (chunk_land_set.isEmpty()) {
                land_by_chunk.remove(ck);
            }
        }

        // Remove from all land
        all_land.remove(l);
        land_by_id.remove(l.getUuid());

        // Remove from group, deleting the group if it is now empty
        LandGroup group = getGroup(l);
        if (group != null) {
            group.removeLand(l);
            if (group.isEmpty()) {
                land_groups.remove(group.getID());
                new File(group_folder, group.getID().toString() + ".yml").delete();
            } else {
                saveGroup(group);
            }
        }

        // Remove file
        File file = new File(land_folder, l.getUuid().toString() + ".yml");
        if (file != null) {
            file.delete();
        }
    }

    public Set<Land> getAllLand() {
        return Collections.unmodifiableSet(all_land);
    }

    public Land getLandByLocation(Location loc) {
        // Get chunk key from location
        ChunkKey ck = new ChunkKey(loc.getWorld().getUID(), loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
        for (Land l : land_by_chunk.getOrDefault(ck, Set.of())) {
            if (l.isInside(loc)) {
                return l;
            }
        }
        return null;
    }

    public Set<Land> getOverlappingLand(Location loc) {
        Set<Land> overlapping = new HashSet<>();

        // Get corners of potential land
        List<Land.LandCorner> corners = Land.calculateCorners(loc, size);

        // Iterate through each corner
        for (Land.LandCorner corner : corners) {
            // Get land at each corner
            Location corner_loc = new Location(loc.getWorld(), corner.x(), loc.getY(), corner.z());
            Land l = getLandByLocation(corner_loc);
            // If land, add to overlapping
            if (l != null) {
                overlapping.add(l);
            }
        }
        return overlapping;
    }

    public Set<Land> getLandByOwner(UUID owner) {
        return Collections.unmodifiableSet(land_by_owner.getOrDefault(owner, Set.of()));
    }

    private void loadAllLand() {
        File[] files = land_folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                try {
                    loadLand(file);
                }
                catch (RuntimeException e) {
                    LandPlugin.getInstance().getLogger().warning("Skipping corrupt land file " + file.getName() + ": " + e);
                }
            }
        }

        loadAllGroups();
        reconcileGroups();
    }

    private void loadAllGroups() {
        File[] files = group_folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }

        for (File file : files) {
            try {
                LandGroup group = new LandGroup(file);
                land_groups.put(group.getID(), group);
            }
            catch (RuntimeException e) {
                LandPlugin.getInstance().getLogger().warning("Skipping corrupt land group file " + file.getName() + ": " + e);
            }
        }
    }

    // Makes sure every land has a valid group, migrating old per-land trusted/name data, and drops empty groups.
    private void reconcileGroups() {
        for (Land l : all_land) {
            LandGroup group = getGroup(l);

            if (group == null) {
                // Land from before groups existed (or with a missing group file): give it its own group
                group = new LandGroup(l.getOwner(), l);
                for (UUID t : l.getLegacyTrusted()) {
                    group.addTrusted(t);
                }
                String legacy_name = l.getLegacyName();
                if (legacy_name != null && !legacy_name.equals(l.getWorld().getName())) {
                    group.setName(legacy_name);
                }
                land_groups.put(group.getID(), group);
                l.setLandGroup(group.getID());
            } else {
                group.addLand(l);
            }
        }

        // Remove land ids that no longer exist and groups left with no land
        Iterator<LandGroup> it = land_groups.values().iterator();
        while (it.hasNext()) {
            LandGroup group = it.next();
            for (UUID id : new ArrayList<>(group.getLandIds())) {
                Land l = land_by_id.get(id);
                if (l == null || !group.getID().equals(l.getLandGroup())) {
                    group.removeLand(id);
                }
            }
            if (group.isEmpty()) {
                it.remove();
                new File(group_folder, group.getID().toString() + ".yml").delete();
            }
        }

        saveAllLand();
    }

    private void loadLand(File file) {
        // Create land object and add to stores
        Land l = new Land(file);
        addLand(l);
    }

    public void saveAllLand() {
        for (Land l : getAllLand()) {
            saveLand(l);
        }
        for (LandGroup group : land_groups.values()) {
            saveGroup(group);
        }
    }

    public void saveGroup(LandGroup group) {
        if (group == null) {
            return;
        }
        group.save(new File(group_folder, group.getID().toString() + ".yml"));
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

    private Set<ChunkKey> getChunks(Land l) {
        // Get min and max chunks
        int min_cx = l.getMinX() >> 4;
        int min_cz = l.getMinZ() >> 4;
        int max_cx = l.getMaxX() >> 4;
        int max_cz = l.getMaxZ() >> 4;

        // Create chunk records
        Set<ChunkKey> chunks = new HashSet<>();
        for (int ci = min_cx; ci <= max_cx; ci++) {
            for (int cj = min_cz; cj <= max_cz; cj++) {
                chunks.add(new ChunkKey(l.getWorldUuid(), ci, cj));
            }
        }
        return chunks;
    }
}
