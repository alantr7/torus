package com.github.alantr7.torus.machine;

import com.github.alantr7.torus.TorusPlugin;
import com.github.alantr7.torus.structure.Structure;
import com.github.alantr7.torus.structure.StructureInstance;
import com.github.alantr7.torus.world.BlockLocation;
import com.github.alantr7.torus.world.Direction;
import com.github.alantr7.torus.world.Pitch;
import org.jetbrains.annotations.NotNull;

public class UnknownStructure extends Structure {

    public UnknownStructure(String namespacedId, String id, int numericId) {
        super(TorusPlugin.DEFAULT_ADDON, namespacedId, id, "Missing Structure", UnknownStructureInstance.class);
        this.numericId = numericId;
    }

    @Override
    protected StructureInstance instantiate(@NotNull BlockLocation location, Direction direction, Pitch pitch) {
        return null;
    }

}
