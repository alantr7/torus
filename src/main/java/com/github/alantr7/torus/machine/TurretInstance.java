package com.github.alantr7.torus.machine;

import com.github.alantr7.torus.TorusPlugin;
import com.github.alantr7.torus.exception.SetupException;
import com.github.alantr7.torus.gui.structure.TurretGUI;
import com.github.alantr7.torus.structure.property.PropertyType;
import com.github.alantr7.torus.structure.socket.EnergySocket;
import com.github.alantr7.torus.structure.socket.ItemSocket;
import com.github.alantr7.torus.utils.MathUtils;
import com.github.alantr7.torus.world.Direction;
import com.github.alantr7.torus.structure.EnergyContainer;
import com.github.alantr7.torus.structure.LoadContext;
import com.github.alantr7.torus.structure.StructureInstance;
import com.github.alantr7.torus.structure.Structures;
import com.github.alantr7.torus.structure.builder.StructureBodyDef;
import com.github.alantr7.torus.structure.StructurePart;
import com.github.alantr7.torus.structure.data.Data;
import com.github.alantr7.torus.world.BlockLocation;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.function.Predicate;

public class TurretInstance extends StructureInstance implements EnergyContainer {

    protected StructurePart head;

    protected EnergySocket inEnergy;

    protected ItemSocket inItem;

    @Getter
    protected Data<Integer> storedEnergy = dataContainer.persist("energy", Data.Type.INT, 0);

    public static final byte TARGET_PLAYERS     = 0b0001;

    public static final byte TARGET_MONSTERS    = 0b0010;

    public static final byte TARGET_ANIMALS     = 0b0100;

    protected Data<Byte> targets = dataContainer.persist("targets", Data.Type.BYTE, TARGET_MONSTERS);

    private Predicate<Entity> targetFilter;

    TurretInstance(LoadContext context) {
        super(context);
    }

    public TurretInstance(BlockLocation location, StructureBodyDef bodyDef, Direction direction) {
        super(Structures.TURRET, location, bodyDef, direction);
    }

    protected LivingEntity target;

    @Override
    public void tick(boolean isVirtual) {
        inEnergy.maintainEnergy(this);
        if (getStoredEnergy().get() < structure.getProperty("energy_settings.consumption", PropertyType.INT))
            return;

        Collection<Entity> entities = location.world.getBukkit().getNearbyEntities(location.toBukkit().add(.5, 0, .5), structure.getProperty("special_settings.target_range_horizontal", PropertyType.FLOAT), structure.getProperty("special_settings.target_range_vertical", PropertyType.FLOAT), structure.getProperty("special_settings.target_range_horizontal", PropertyType.FLOAT), targetFilter);
        if (entities.isEmpty())
            return;

        target = (LivingEntity) entities.iterator().next();
        if (target == null || target.isDead() || target.getHealth() == 0)
            return;

        consumeEnergy(structure.getProperty("energy_settings.consumption", PropertyType.INT));

        Bukkit.getScheduler().runTaskLater(TorusPlugin.getInstance(), () -> {
            location.world.getBukkit().playSound(location.toBukkit().add(.5, 0, .5), Sound.ENTITY_SHULKER_SHOOT, 1f, 0.2f);

            Location diff = target.getLocation().add(target.getVelocity()).subtract(location.toBukkit().add(.5, 0, .5));
            diff.setY(0);

            Vector knockback = diff.toVector().normalize().multiply(0.4f);
            knockback.setY(0.1f);

            Location laserPosition = diff.clone().multiply(.5).add(location.toBukkit().add(.5, 0, .5));
            laserPosition.setY(location.y + 1.375f);
            ItemDisplay laser = (ItemDisplay) location.world.getBukkit().spawnEntity(laserPosition, EntityType.ITEM_DISPLAY);
            laser.setItemStack(new ItemStack(Material.REDSTONE_BLOCK));
            laser.setPersistent(false);

            Transformation laserTransform = laser.getTransformation();
            float[] scale = new float[]{0.05f, 0.05f, (float) diff.length() - 1f};
            float angle = (float) Math.atan2(target.getLocation().getZ() - (0.5f + location.z), target.getLocation().getX() - (0.5f + location.x));

            laserTransform.getScale().set(scale);
            laser.setTransformation(laserTransform);

            laserPosition.setYaw((float) Math.toDegrees(angle) + 90f);
            laserPosition.setPitch(0f);
            laser.teleport(laserPosition);

            Bukkit.getScheduler().runTaskLater(TorusPlugin.getInstance(), () -> {
                target.damage(8f);
                target.setVelocity(knockback);
                laser.remove();
            }, 3L);

            updateHeadRotation();
            Bukkit.getScheduler().runTaskLater(TorusPlugin.getInstance(), this::updateHeadRotation, 8);
            Bukkit.getScheduler().runTaskLater(TorusPlugin.getInstance(), this::updateHeadRotation, 12);
            Bukkit.getScheduler().runTaskLater(TorusPlugin.getInstance(), this::updateHeadRotation, 16);
        }, (int) (Math.random() * 5));
    }

    public void updateHeadRotation() {
        double angleH = Math.atan2(target.getLocation().getX() - (location.x + .5), target.getLocation().getZ() - (location.z + .5));
        double angleV = Math.atan2(target.getLocation().getY() - (location.y + .5), target.getLocation().distance(location.toBukkit()));

        // TODO: Fix vertical rotation by adding "origin" property for rotation to DISPLAY_ENTITIES renderers
        model.getPartByName("head").setRotation(180f - (float) Math.toDegrees(angleH), 0);
    }

    @Override
    public boolean onPlayerInteract(PlayerInteractEvent event, BlockLocation location) {
        Player player = event.getPlayer();
        if (!testOwnership(player))
            return true;

        new TurretGUI(player, this).open();
        return true;
    }

    public byte getTargets() {
        return targets.get();
    }

    public void setTargets(byte flags) {
        targets.update(flags);
        updateFilter();
        save();
    }

    private void updateFilter() {
        byte flags = targets.get();
        targetFilter = entity -> {
            if (entity.getScoreboardTags().contains("torus_entity"))
                return false;

            if (entity instanceof Monster || entity instanceof Slime)
                return MathUtils.hasFlag(flags, TARGET_MONSTERS);

            if (entity instanceof Animals)
                return MathUtils.hasFlag(flags, TARGET_ANIMALS);

            if (entity instanceof Player)
                return MathUtils.hasFlag(flags, TARGET_PLAYERS) && !entity.getUniqueId().equals(getOwnerId());

            return false;
        };
    }

    @Override
    protected void setup() throws SetupException {
        head = requirePart("head");
        inEnergy = (EnergySocket) requireSocket("in_energy");
        inEnergy.maximumInput = structure.getProperty("energy_settings.maximum_input", PropertyType.INT);
        inItem = (ItemSocket) requireSocket("in_item");
        updateFilter();
    }

    @Override
    public int getEnergyCapacity() {
        return structure.getProperty("energy_settings.capacity", PropertyType.INT);
    }

}