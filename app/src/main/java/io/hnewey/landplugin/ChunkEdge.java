package io.hnewey.landplugin;

import java.util.*;

public class ChunkEdge {
    ChunkEdge(ChunkCorner start, ChunkCorner end) {
        this.start = start;
        this.end = end;
    }

    // Edges are equal whichever way round they run, so the shared edge of two chunks matches
    @Override
    public boolean equals(Object o) {
        return o instanceof ChunkEdge other
                && ((start.equals(other.start) && end.equals(other.end))
                || (start.equals(other.end) && end.equals(other.start)));
    }

    @Override
    public int hashCode() {
        // Symmetric so the direction of the edge doesn't matter
        return start.hashCode() + end.hashCode();
    }

    // True if both edges lie along the same line, whichever way they point
    public boolean isAligned(ChunkEdge other) {
        return dx() * other.dz() - dz() * other.dx() == 0;
    }

    public List<Double> unit_vector() {
        return Arrays.asList(dx() / length(), dz() / length());
    }

    // Direction (-1, 0 or 1 on each axis) running along this edge away from the given end
    public List<Integer> awayFrom(ChunkCorner corner) {
        int sign = corner.equals(start) ? 1 : -1;
        return Arrays.asList(sign * (int) Math.signum(dx()), sign * (int) Math.signum(dz()));
    }

    public double direction() {
        return Math.atan2(dz(), dx());
    }

    public double length_squared() {
        return dz() * dz() + dx() * dx();
    }

    public ChunkCorner start;
    public ChunkCorner end;

    private double length() {
        return Math.sqrt(length_squared());
    }

    private double dx() {
        return this.end.x() - this.start.x();
    }

    private double dz() {
        return this.end.z() - this.start.z();
    }
}
