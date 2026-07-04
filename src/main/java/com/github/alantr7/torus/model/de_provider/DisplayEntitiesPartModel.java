package com.github.alantr7.torus.model.de_provider;

import com.github.alantr7.torus.TorusPlugin;
import com.github.alantr7.torus.model.PartModel;
import com.github.alantr7.torus.model.PartModelTemplate;
import com.github.alantr7.torus.model.animation.Animation;
import io.papermc.paper.entity.TeleportFlag;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DisplayEntitiesPartModel extends PartModel {

    public final Display parent;

    public final List<EntityReference> entityReferences;

    public Map<String, Animation> predefinedAnimations = Collections.emptyMap();

    public DisplayEntitiesPartModel(PartModelTemplate template, Display parent, List<EntityReference> entityReferences) {
        super(template);
        this.parent = parent;
        this.entityReferences = entityReferences;
    }

    @Override
    public void setLocation(@NotNull Location location) {
        if (TorusPlugin.usesPaperAPI()) {
            parent.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN, TeleportFlag.EntityState.RETAIN_PASSENGERS);
        } else {
            List<Entity> passengers = new ArrayList<>(parent.getPassengers());
            parent.eject();
            parent.teleport(location);
            for (Entity en : passengers) {
                parent.addPassenger(en);
            }
        }
    }

    @Override
    public void setRotation(float horizontal, float vertical) {
        for (EntityReference ref : entityReferences) {
            ref.getEntity().setRotation(horizontal, vertical);
        }
    }

    @Override
    public void remove() {
        parent.remove();
        entityReferences.forEach(ref -> {
            if (ref.getEntity() != null)
                ref.entity.remove();
        });
    }
}
