package com.github.alantr7.torus.model.de_provider;

import com.github.alantr7.torus.machine.OreCrusherInstance;
import com.github.alantr7.torus.machine.SteamTurbineInstance;
import com.github.alantr7.torus.machine.Windmill;
import com.github.alantr7.torus.machine.WindmillInstance;
import com.github.alantr7.torus.model.animation.Animation;
import com.github.alantr7.torus.structure.StructureInstance;
import com.github.alantr7.torus.structure.Structures;
import org.bukkit.entity.Display;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

import java.util.AbstractMap;
import java.util.Map;
import java.util.function.BiFunction;

public class DisplayEntitiesDefaultAnimations {

    public static final BiFunction<StructureInstance, DisplayEntitiesPartModel, Animation> WINDMILL_BLADES_ROT = (structure, part) -> new Animation() {
        float angle = (float) (Math.random() * Math.PI / 2f) * ((WindmillInstance) structure).getEfficiency();
        @Override
        public void tick() {
            part.entityReferences.forEach(ref -> {
                Display entity = ref.getEntity();
                Transformation transform = entity.getTransformation();

                Quaternionf rotation = new Quaternionf(transform.getLeftRotation())
                  .rotateXYZ(0f, 0f, -angle);

                entity.setTransformation(new Transformation(transform.getTranslation(), rotation, transform.getScale(), transform.getRightRotation()));
                entity.setInterpolationDelay(0);
                entity.setInterpolationDuration(20);
            });
            angle = Windmill.MAXIMUM_SPEED * ((WindmillInstance) structure).getEfficiency();
        }
    };

    public static final BiFunction<StructureInstance, DisplayEntitiesPartModel, Animation> WINDMILL_SHAFT_ROT = (structure, part) -> new Animation() {
        float angle = (float) (Math.random() * Math.PI / 2f) * ((WindmillInstance) structure).getEfficiency();
        @Override
        public void tick() {
            part.entityReferences.forEach(ref -> {
                Display entity = ref.getEntity();
                Transformation transform = entity.getTransformation();

                Quaternionf rotation = new Quaternionf(transform.getLeftRotation())
                  .rotateXYZ(0f, angle, 0f);

                entity.setTransformation(new Transformation(transform.getTranslation(), rotation, transform.getScale(), transform.getRightRotation()));
                entity.setInterpolationDelay(0);
                entity.setInterpolationDuration(20);
            });
            angle = Windmill.MAXIMUM_SPEED * ((WindmillInstance) structure).getEfficiency();
        }
    };

    public static final BiFunction<StructureInstance, DisplayEntitiesPartModel, Animation> ORE_CRUSHER_WHEEL_LEFT_ROT = (structure, part) -> new Animation() {
        @Override
        public void tick() {
            part.entityReferences.forEach(ref -> {
                Display display = ref.getEntity();
                Transformation transformation = display.getTransformation();
                display.setTransformation(new Transformation(transformation.getTranslation(), new AxisAngle4f(((OreCrusherInstance) structure).wheelsAngle * 0.9f, 0, 0, 1f), transformation.getScale(), new AxisAngle4f(transformation.getRightRotation())));
                display.setInterpolationDelay(0);
                display.setInterpolationDuration(20);
            });
        }
    };

    public static final BiFunction<StructureInstance, DisplayEntitiesPartModel, Animation> ORE_CRUSHER_WHEEL_RIGHT_ROT = (structure, part) -> new Animation() {
        @Override
        public void tick() {
            part.entityReferences.forEach(ref -> {
                Display display = ref.getEntity();
                Transformation transformation = display.getTransformation();
                display.setTransformation(new Transformation(transformation.getTranslation(), new AxisAngle4f(((OreCrusherInstance) structure).wheelsAngle, 0, 0, 1f), transformation.getScale(), new AxisAngle4f(transformation.getRightRotation())));
                display.setInterpolationDelay(0);
                display.setInterpolationDuration(20);
            });
        }
    };

    public static final BiFunction<StructureInstance, DisplayEntitiesPartModel, Animation> STEAM_TURBINE_SHAFT_ROT = (structure, part) -> new Animation() {
        @Override
        public void tick() {
            part.entityReferences.forEach(ref -> {
                Display display = ref.getEntity();
                Transformation transformation = display.getTransformation();

                Quaternionf rotation = new Quaternionf(transformation.getLeftRotation())
                  .rotateXYZ(0f, (float) Math.toRadians(((SteamTurbineInstance) structure).shaftAngle), 0f);

                display.setTransformation(new Transformation(transformation.getTranslation(), rotation, transformation.getScale(), transformation.getRightRotation()));
                display.setInterpolationDelay(0);
                display.setInterpolationDuration(20);
            });
        }
    };

    private static final Map<String, Map<String, Map.Entry<String, BiFunction<StructureInstance, DisplayEntitiesPartModel, Animation>>>> defaultAnimations = Map.of(
      Structures.ORE_CRUSHER.id, Map.of("wheel_left", new AbstractMap.SimpleEntry<>("wheel_left_spin", ORE_CRUSHER_WHEEL_LEFT_ROT), "wheel_right", new AbstractMap.SimpleEntry<>("wheel_right_spin", ORE_CRUSHER_WHEEL_RIGHT_ROT)),
      Structures.WINDMILL.id, Map.of("blades", new AbstractMap.SimpleEntry<>("blades_spin", WINDMILL_BLADES_ROT), "shaft", new AbstractMap.SimpleEntry("shaft_spin", WINDMILL_SHAFT_ROT)),
      Structures.STEAM_TURBINE.id, Map.of("shaft", new AbstractMap.SimpleEntry<>("shaft_spin", STEAM_TURBINE_SHAFT_ROT))
    );

    public static void inject(StructureInstance instance) {
        Map<String, Map.Entry<String, BiFunction<StructureInstance, DisplayEntitiesPartModel, Animation>>> animations = defaultAnimations.get(instance.structure.id);
        if (animations == null)
            return;

        animations.forEach((partName, entry) -> {
            if (instance.model.getPartByName(partName) instanceof DisplayEntitiesPartModel de) {
                de.predefinedAnimations = Map.of(entry.getKey(), entry.getValue().apply(instance, de));
            }
        });
    }

}
