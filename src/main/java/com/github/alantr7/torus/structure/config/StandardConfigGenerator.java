package com.github.alantr7.torus.structure.config;

import com.github.alantr7.torus.log.Category;
import com.github.alantr7.torus.log.TorusLogger;
import com.github.alantr7.torus.structure.Structure;
import com.github.alantr7.torus.structure.property.Property;
import com.github.alantr7.torus.structure.property.PropertyType;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.*;

public class StandardConfigGenerator {

    public static final StandardConfigGenerator INSTANCE = new StandardConfigGenerator();

    private static final Map<String, List<String>> comments = Map.of(
            "config_version", List.of("Do not change this value - it is used for automatic config updates."),
            "general_settings.placement_offset", List.of("When machine is placed on a block, it will be offset by this vector.",
                    "This vector is facing the positive Z direction. Rotation will be automatically applied."),
            "general_settings.heavy", List.of("If set to true, structure can be broken only with a hammer.")
    );

    private static final Map<PropertyType<?>, PropertyMapper<?>> mappers = new HashMap<>();
    static {
        map(PropertyType.INT, MemorySection::set);
        map(PropertyType.BOOLEAN, MemorySection::set);
        map(PropertyType.FLOAT, MemorySection::set);
        map(PropertyType.STRING, MemorySection::set);
        map(PropertyType.STRING_LIST, MemorySection::set);
        map(PropertyType.VECTOR3I, (section, key, val) -> section.set(key, List.of(val.x, val.y, val.z)));
        map(PropertyType.VECTOR3F, (section, key, val) -> section.set(key, List.of(val.x, val.y, val.z)));
    }

    private static <T> void map(PropertyType<T> type, PropertyMapper<T> mapper) {
        mappers.put(type, mapper);
    }

    @SuppressWarnings("all")
    public YamlConfiguration generate(Structure structure) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("config_version", 2);
        config.set("general_settings.enabled", structure.isEnabled);

        for (Property<?> property : structure.getProperties()) {
            PropertyMapper mapper = mappers.get(property.type);
            if (mapper != null) {
                mapper.map(config, property.name, property.value);
            } else {
                TorusLogger.error(Category.STRUCTURES, "There is no property mapper for type: " + property.type.name);
            }
        }

        comments.forEach(config::setComments);
        return config;
    }

    @FunctionalInterface
    interface PropertyMapper<T> {

        void map(YamlConfiguration config, String data, T value);

    }

}
