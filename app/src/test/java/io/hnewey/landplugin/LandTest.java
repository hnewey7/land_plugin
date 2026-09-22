package io.hnewey.landplugin;

import org.bukkit.Location;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class LandTest {
    private ServerMock server;
    private WorldMock world;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        // Land reads LandPlugin.getInstance().getConfig() in a static initialiser,
        // so the plugin must be loaded before Land is first used.
        MockBukkit.load(LandPlugin.class);
        world = server.addSimpleWorld("world");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private Land landAt(int x, int z) {
        return new Land(UUID.randomUUID(), new Location(world, x, 64, z));
    }

    @Test
    void centreIsInside() {
        Land land = landAt(100, 100);
        assertTrue(land.isInside(new Location(world, 100, 64, 100)));
    }

    @Test
    void edgesAreInclusive() {
        // land_size is 32 in config.yml, so the claim spans centre +/- 16
        Land land = landAt(100, 100);
        assertTrue(land.isInside(new Location(world, 84, 64, 84)));
        assertTrue(land.isInside(new Location(world, 116, 64, 116)));
    }

    @Test
    void justOutsideIsNotInside() {
        Land land = landAt(100, 100);
        assertFalse(land.isInside(new Location(world, 83, 64, 100)));
        assertFalse(land.isInside(new Location(world, 117, 64, 100)));
        assertFalse(land.isInside(new Location(world, 100, 64, 83)));
        assertFalse(land.isInside(new Location(world, 100, 64, 117)));
    }

    @Test
    void worksAcrossNegativeCoordinates() {
        Land land = landAt(-100, -100);
        assertTrue(land.isInside(new Location(world, -100, 64, -100)));
        assertTrue(land.isInside(new Location(world, -84, 64, -116)));
        assertFalse(land.isInside(new Location(world, -83, 64, -100)));
    }

    @Test
    void heightIsIgnored() {
        Land land = landAt(0, 0);
        assertTrue(land.isInside(new Location(world, 0, -60, 0)));
        assertTrue(land.isInside(new Location(world, 0, 300, 0)));
    }

    @Test
    void ownerMatchesByValue() {
        UUID id = UUID.randomUUID();
        Land land = new Land(id, new Location(world, 0, 64, 0));
        assertTrue(land.isOwner(id));
        // Equal UUID, different instance - what you'd get after loading from disk
        assertTrue(land.isOwner(UUID.fromString(id.toString())));
        assertFalse(land.isOwner(UUID.randomUUID()));
    }

    @Test 
    void trusted() {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        Land land = new Land(owner, new Location(world, 0, 64, 0));

        assertFalse(land.isTrusted(owner));
        assertFalse(land.isTrusted(other));

        land.addTrusted(other);

        assertFalse(land.isTrusted(owner));
        assert(land.isTrusted(other));

        land.removeTrusted(other);

        assertFalse(land.isTrusted(owner));
        assertFalse(land.isTrusted(other));
    }
}
