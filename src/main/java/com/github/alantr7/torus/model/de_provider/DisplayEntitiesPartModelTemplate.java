package com.github.alantr7.torus.model.de_provider;

import com.github.alantr7.torus.TorusPlugin;
import com.github.alantr7.torus.model.*;
import com.github.alantr7.torus.model.animation.Animation;
import com.github.alantr7.torus.model.animation.AnimationProvider;
import com.github.alantr7.torus.world.Direction;
import com.github.alantr7.torus.world.Pitch;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DisplayEntitiesPartModelTemplate extends PartModelTemplate {

    public final List<PartModelElementDisplayRenderer> parts = new ArrayList<>();

    public final int teleportDuration;

    public DisplayEntitiesPartModelTemplate(String name) {
        this(name, new Vector3f(.5f, 0, .5f), 0, Collections.emptyMap());
    }

    public DisplayEntitiesPartModelTemplate(String name, Vector3f offset, int teleportDuration, Map<String, AnimationProvider<PartModel, Animation>> animationMap) {
        super(name, offset, animationMap);
        this.teleportDuration = teleportDuration;
    }

    public void add(PartModelElementDisplayRenderer part) {
        parts.add(part);
    }

    @Override
    public PartModel build(Location location, Direction direction, Pitch pitch) {
        location = location.clone().add(offset.x, offset.y, offset.z);

        List<Display> entities = new ArrayList<>();
        ItemDisplay parent = location.getWorld().spawn(location, ItemDisplay.class);
        parent.setTeleportDuration(teleportDuration);

        for (PartModelElementDisplayRenderer part : parts) {
            Vector3f rotatedOffset = new Vector3f(part.offset[0], part.offset[1], part.offset[2]);

            Display entity = location.getWorld().spawn(location, part.entityType);
            transformEntity(entity, rotatedOffset, part, direction.rotH, pitch.rotV);
            entity.setViewRange(part.viewRange);

            parent.addPassenger(entity);
            entities.add(entity);
        }

        return new DisplayEntitiesPartModel(this, parent, entities.stream().map(EntityReference::new).toList());
    }

    private void transformEntity(Display entity, Vector3f translation, PartModelElementDisplayRenderer part, float rotH, float rotV) {
        entity.setPersistent(false);
        if (part.entityType == ItemDisplay.class) {
            ((ItemDisplay) entity).setItemStack(((PartModelElementItemDisplayRenderer) part).itemStack.clone());
        } else if (part.entityType == BlockDisplay.class) {
            ((BlockDisplay) entity).setBlock(((PartModelElementBlockDisplayRenderer) part).blockData.clone());
        }
        entity.getPersistentDataContainer().set(new NamespacedKey(TorusPlugin.getInstance(), "purpose"), PersistentDataType.STRING, "structure_model");

        Transformation transformation = entity.getTransformation();
        transformation.getTranslation().set(rotV == 0 ? translation : new Vector3f(translation).add(0, -0.5f, -0.5f * rotV / Math.abs(rotV)));
        transformation.getScale().set(part.scale);
        transformation.getLeftRotation().set(part.rotation);
        entity.setTransformation(transformation);
        entity.setRotation(rotH, rotV);
    }

}
