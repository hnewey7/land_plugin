package io.hnewey.landplugin;

import java.util.*;

public record ChunkCorner(int x, int z, int dx, int dz) {
    @Override
    public boolean equals(Object o) {
        return o instanceof ChunkCorner cc
                && x == cc.x
                && z == cc.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }

    public ChunkCorner overlay_direction(List<Integer> other) {
        return new ChunkCorner(x, z, other.get(0) != 0 ? other.get(0) : dx, other.get(1) != 0 ? other.get(1) : dz);
    }
}
