package com.github.alantr7.torus.machine;

import com.github.alantr7.torus.exception.SetupException;
import com.github.alantr7.torus.structure.*;
import com.github.alantr7.torus.structure.builder.StructureBodyDef;
import com.github.alantr7.torus.structure.builder.StructurePartDef;
import com.github.alantr7.torus.structure.builder.StructureSocketDef;
import com.github.alantr7.torus.structure.data.Data;
import com.github.alantr7.torus.structure.socket.FluidSocket;
import com.github.alantr7.torus.structure.socket.Socket;
import com.github.alantr7.torus.world.BlockLocation;
import com.github.alantr7.torus.world.Direction;
import com.github.alantr7.torus.world.Fluid;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class PressureVentInstance extends StructureInstance implements FluidContainer {

    private Data<Integer> buffer = dataContainer.persist("buffer", Data.Type.INT, 0);

    private FluidSocket outFluid;

    private boolean hasWorked;

    PressureVentInstance(LoadContext context) {
        super(context);
    }

    public PressureVentInstance(Structure structure, BlockLocation location, StructureBodyDef bodyDef, Direction direction) {
        super(structure, location, bodyDef, direction);
    }

    @Override
    protected void setup() throws SetupException {
        if (getSocket("out_fluid") == null) {
            StructurePart part = new StructurePart(this, new BlockLocation(location.world, 0, 0, 0), "out_fluid");
            parts.put(part.name, part);
            outFluid = (FluidSocket) createSocket(Socket.Medium.FLUID, part, direction.getLeft().mask(), Socket.FlowDirection.OUT);
        }
        outFluid = requireSocket("out_fluid", FluidSocket.class);
        outFluid.maximumOutput = 1000;
    }

    @Override
    public void tick(boolean isVirtual) {
        boolean hasPipes = outFluid.isConnected(direction.getLeft());

        if (!hasPipes && buffer.get() != 0) {
            for (int i = 0; i < 3; i++) {
                location.world.getBukkit().spawnParticle(
                  Particle.CAMPFIRE_SIGNAL_SMOKE,
                  location.getRelative(direction.getLeft()).toBukkitCentered().add(0, 0.45f, 0),
                  0,
                  0, 0.2, 0
                );
            }
            buffer.update(0);
            location.world.getBukkit().playSound(location.toBukkitCentered(), Sound.BLOCK_LAVA_EXTINGUISH, 0.5f, 1.5f);
        }

        else if (hasPipes && hasWorked) {
            location.world.getBukkit().playSound(location.toBukkitCentered(), Sound.BLOCK_LAVA_EXTINGUISH, 0.5f, 1.5f);
            hasWorked = false;
        }
    }

    public int vent(int amount) {
        int previousAmount = buffer.get();
        int newBufferAmount = Math.min(previousAmount + amount, getFluidCapacity());
        if (newBufferAmount != previousAmount) {
            hasWorked = true;
            buffer.update(newBufferAmount);
        }
        return newBufferAmount - previousAmount;
    }

    @Override
    public @Nullable Fluid getFluid() {
        return Fluid.STEAM;
    }

    @Override
    public int getFluidCapacity() {
        return 1000;
    }

    @Override
    public int getStoredFluid() {
        return buffer.get();
    }

    @Override
    public void setStoredFluid(int fluid) {
        buffer.update(fluid);
    }

}
