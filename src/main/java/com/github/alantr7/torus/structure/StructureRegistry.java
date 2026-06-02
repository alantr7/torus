package com.github.alantr7.torus.structure;

import com.github.alantr7.bukkitplugin.annotations.core.Invoke;
import com.github.alantr7.bukkitplugin.annotations.core.InvokePeriodically;
import com.github.alantr7.bukkitplugin.annotations.core.Singleton;
import com.github.alantr7.bytils.buffer.ByteArrayReader;
import com.github.alantr7.bytils.buffer.ByteArrayWriter;
import com.github.alantr7.torus.TorusPlugin;
import com.github.alantr7.torus.api.addon.ConfigType;
import com.github.alantr7.torus.api.resource.ResourceLocation;
import com.github.alantr7.torus.log.Category;
import com.github.alantr7.torus.log.TorusLogger;
import com.github.alantr7.torus.model.controller.ModelController;
import com.github.alantr7.torus.updater.UpdateUtils_0_6_1;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

@Singleton
public class StructureRegistry {

    private int nextStructureId = 2;

    private final Map<String, Structure> loaded = new HashMap<>();

    private final Map<Integer, Structure> loadedByNumericIds = new LinkedHashMap<>();

    private final Map<String, Integer> structuresIds = new HashMap<>();

    private final Map<String, String> changedIds = Map.of(
      "torus:item_cable", "torus:item_conduit"
    );

    private final Set<Structure> saveQuery = new LinkedHashSet<>();

    @Invoke(Invoke.Schedule.BEFORE_PLUGIN_ENABLE)
    private void init() {
        load();
    }

    private void load() {
        File file = new File(TorusPlugin.getInstance().getDataFolder(), "id_map.dat");
        if (!file.exists())
            return;

        try {
            ByteArrayReader reader = new ByteArrayReader(Files.readAllBytes(file.toPath()));
            while (reader.hasNext()) {
                int numericId = reader.readU2();
                String id = reader.readShortString();

                structuresIds.put(changedIds.getOrDefault(id, id), numericId);
                if (numericId >= nextStructureId) {
                    nextStructureId = numericId + 1;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @InvokePeriodically(interval = 20 * 60)
    @Invoke(Invoke.Schedule.AFTER_PLUGIN_DISABLE)
    public void save() {
        if (saveQuery.isEmpty())
            return;

        File file = new File(TorusPlugin.getInstance().getDataFolder(), "id_map.dat");
        file.getParentFile().mkdirs();

        try {
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            raf.seek(raf.length());

            ByteArrayWriter writer = new ByteArrayWriter();

            for (Structure str : saveQuery) {
                writer.writeU2(str.numericId);
                writer.writeShortString(str.namespacedId);
            }

            saveQuery.clear();

            raf.write(writer.getBuffer());
            raf.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void registerAndInitialize(Structure structure) {
        if (structure.numericId != -1) {
            throw new RuntimeException("Structure must not have numeric id already assigned when registering it");
        }

        if (structuresIds.containsKey(structure.namespacedId)) {
            structure.numericId = structuresIds.get(structure.namespacedId);
        } else {
            structure.numericId = nextStructureId++;
            structuresIds.put(structure.namespacedId, structure.numericId);
            saveQuery.add(structure);
        }

        structure.initDefaultProperties();

        if (structure.addon.allowsExternalConfig(ConfigType.STRUCTURE)) {
            File configFile = new File(structure.addon.configsDirectory, structure.id + ".yml");
            File pre_0_6_1_config = new File(structure.addon.configsDirectory, structure.id + ".config.yml");
            if (pre_0_6_1_config.exists()) {
                UpdateUtils_0_6_1.updateStructureConfig(pre_0_6_1_config, configFile);
            }

            structure.reloadConfig();
        }

        // Load model controller
        try {
            ResourceLocation controllerLocation = new ResourceLocation(
              structure.addon.externalContainer, "models/" + structure.id + ".yml",
              structure.addon.classpathContainer, "configs/torus/models/" + structure.id + ".yml"
            );
            if (!controllerLocation.exists()) {
                throw new Exception("Model controller file does not exist for structure '" + structure + "'!");
            }
            ModelController modelController = TorusPlugin.getInstance().getModelLoader().loadController(
              structure,
              YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(controllerLocation.getResource()).stream))
            );
            if (modelController != null) {
                structure.setModelController(modelController);
            }
        } catch (Exception exc) {
            TorusLogger.error(Category.MODELS, "Invalid model controller for structure '" + structure.id + "'");
            exc.printStackTrace();
        }

        structure.addon.registerContent(structure);
        loaded.put(structure.namespacedId, structure);
        loadedByNumericIds.put(structure.numericId, structure);
    }

    public Structure getStructure(String id) {
        return loaded.get(id);
    }

    public Structure getStructure(int id) {
        return loadedByNumericIds.get(id);
    }

    public Set<String> getStructuresIds() {
        return structuresIds.keySet();
    }

    public Set<Map.Entry<String, Integer>> getStructuresIdsMap() {
        return structuresIds.entrySet();
    }

    public Collection<Structure> getStructures() {
        return loaded.values();
    }

}
