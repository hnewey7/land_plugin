package io.hnewey.landplugin;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.*;
import java.io.File;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;
import org.bukkit.Location;

public class LandManagerTest {
    private ServerMock server;
    private WorldMock world;

    private LandManager lm;

    @BeforeEach 
    void setup() {
        server = MockBukkit.mock();
        MockBukkit.load(LandPlugin.class);
        world = server.addSimpleWorld("world");

        this.lm = new LandManager(LandPlugin.getInstance());
    }

    @AfterEach 
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void createLand() {
        UUID player = UUID.randomUUID();
        Location loc = new Location(world, 0, 0, 0);

        // Create land
        lm.createLand(player, loc);

        // Get land
        Land land_by_loc = lm.getLandByLocation(loc);
        assertNotNull(land_by_loc);
        assert(land_by_loc.isOwner(player));
        assert(land_by_loc.isInside(loc));

        Set<Land> land_by_owner = lm.getLandByOwner(player);
        assert(land_by_owner.contains(land_by_loc));

        // Delete land
        lm.deleteLand(land_by_loc);

        // Get land
        land_by_loc = lm.getLandByLocation(loc);
        assertNull(land_by_loc);

        land_by_owner = lm.getLandByOwner(player);
        assert(land_by_owner.isEmpty());
    }

    @Test 
    void getAllLand() {
        UUID player_one = UUID.randomUUID();
        UUID player_two = UUID.randomUUID();

        Location loc_one = new Location(world, 0, 0, 0);
        Location loc_two = new Location(world, 100, 0, 100);

        lm.createLand(player_one, loc_one);
        lm.createLand(player_two, loc_two);

        Set<Land> land_set = lm.getAllLand();
        assert(land_set.size() == 2);
    }

    @Test
    void saveAndLoadLand() {
        UUID player = UUID.randomUUID();
        Location loc = new Location(world, 0, 0, 0);

        // Create land
        lm.createLand(player, loc);

        // Save all land
        lm.saveAllLand();

        // Correct folder
        File folder = new File(LandPlugin.getInstance().getDataFolder(), "land");
        assert(folder.exists());

        // Correct number of files
        File[] files = folder.listFiles();
        assert(files.length == 1);

        // Create new land manager
        LandManager new_lm = new LandManager(LandPlugin.getInstance());
        
        // Correct land
        Land land = new_lm.getLandByLocation(loc);
        assertNotNull(land);
        assert(land.isOwner(player));
        assert(land.isInside(loc));
    }
}
