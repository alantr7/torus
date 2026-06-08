package com.github.alantr7.torus.machine;

import com.github.alantr7.torus.exception.SetupException;
import com.github.alantr7.torus.structure.Inspectable;
import com.github.alantr7.torus.structure.LoadContext;
import com.github.alantr7.torus.structure.Status;
import com.github.alantr7.torus.structure.StructureInstance;
import com.github.alantr7.torus.structure.inspection.InspectableDataContainer;
import com.github.alantr7.torus.world.BlockLocation;
import com.github.alantr7.torus.world.TorusChunk;

import java.util.HashSet;
import java.util.Set;

public class UnknownStructureInstance extends StructureInstance implements Inspectable {

    public UnknownStructureInstance(LoadContext context) {
        super(context);
    }

    @Override
    protected void validateCollision() {
    }

    @Override
    protected void setup() throws SetupException {

    }

    private static final int MAXIMUM_STRUCTURE_HALF_WIDTH = 8;

    @Override
    public void onModelSpawn() {
        // Get collision vectors from chunks
        int fromChunkX = (location.x - MAXIMUM_STRUCTURE_HALF_WIDTH) >> 4;
        int fromChunkZ = (location.z - MAXIMUM_STRUCTURE_HALF_WIDTH) >> 4;
        int toChunkX = (location.x + MAXIMUM_STRUCTURE_HALF_WIDTH) >> 4;
        int toChunkZ = (location.z + MAXIMUM_STRUCTURE_HALF_WIDTH) >> 4;

        Set<BlockLocation> vectors = new HashSet<>();
        for (int chunkX = fromChunkX; chunkX <= toChunkX; chunkX++) {
            for (int chunkZ = fromChunkZ; chunkZ <= toChunkZ; chunkZ++) {
                TorusChunk chunk = location.world.getChunkAt(chunkX, chunkZ);
                if (chunk == null || chunk.status != Status.PHYSICAL)
                    continue;

                vectors.addAll(chunk.getOccupationsBy(this));
            }
        }

        int pos = 0;
        byte[] collisionVectors = new byte[vectors.size() * 3];
        for (BlockLocation loc : vectors) {
            collisionVectors[pos++] = (byte) (loc.x - location.x);
            collisionVectors[pos++] = (byte) (loc.y - location.y);
            collisionVectors[pos++] = (byte) (loc.z - location.z);
        }

        setCollisionVectors(collisionVectors);
    }

    @Override
    public InspectableDataContainer setupInspectableData() {
        return new InspectableDataContainer((byte) 1)
          .line(() -> structure.namespacedId);
    }

}
