package com.github.alantr7.torus.world;

import org.bukkit.Location;

public class Cuboid {

    public final BlockLocation min, max;

    public Cuboid(BlockLocation min, BlockLocation max) {
        this.min = min;
        this.max = max;
    }

    public boolean contains(Location location) {
        return contains(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public boolean contains(int x, int y, int z) {
        return x >= min.x && x <= max.x && y >= min.y && y <= max.y && z >= min.z && z <= max.z;
    }

}
