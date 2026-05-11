package com.github.alantr7.torus.model;

import com.github.alantr7.bukkitplugin.annotations.core.Invoke;
import com.github.alantr7.bukkitplugin.annotations.core.RequiresPlugin;
import com.github.alantr7.bukkitplugin.annotations.core.Singleton;
import com.github.alantr7.torus.TorusPlugin;
import com.github.alantr7.torus.api.TorusAPI;
import com.github.alantr7.torus.api.resource.Resource;
import com.github.alantr7.torus.api.resource.ResourceLocation;
import com.github.alantr7.torus.integration.modelengine.model_provider.ModelEngineRendererConfigLoader;
import com.github.alantr7.torus.log.Category;
import com.github.alantr7.torus.log.TorusLogger;
import com.github.alantr7.torus.model.controller.ModelCase;
import com.github.alantr7.torus.model.controller.ModelController;
import com.github.alantr7.torus.model.de_provider.*;
import com.github.alantr7.torus.structure.Structure;
import com.github.alantr7.torus.structure.state.State;
import com.github.alantr7.torus.structure.state.StateType;
import com.github.alantr7.torus.updater.UpdateUtils_0_5_2;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.joml.Vector3f;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class ModelLoader {

    private final Map<String, RendererConfigLoader> renderers = new HashMap<>();

    {
        renderers.put("display_entities", new DisplayEntitiesRendererConfigLoader());
    }

    public ModelTemplate load(File file, Map<String, String> variables) {
        return load(file, YamlConfiguration.loadConfiguration(file), variables);
    }

    public ModelTemplate load(Resource resource, Map<String, String> variables) {
        try (Reader reader = new InputStreamReader(resource.stream)) {
            return load(resource.file, YamlConfiguration.loadConfiguration(reader), variables);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public ModelTemplate load(File file, FileConfiguration yaml, Map<String, String> variables) {
        ModelTemplate template = new ModelTemplate(yaml.getInt("model_version", 1));
        if (file != null) {
            UpdateUtils_0_5_2.updateModelFileFormatFromV1ToV2(file, yaml);
        }

        for (String partName : yaml.getKeys(false)) {
            ConfigurationSection section = yaml.getConfigurationSection(partName);
            if (section == null)
                continue;

            String rendererId = section.getString("renderer", "").toLowerCase();
            RendererConfigLoader loader = renderers.get(rendererId);

            if (loader == null) {
                TorusLogger.error(Category.MODELS, "Invalid renderer: " + rendererId);
                continue;
            }

            List<Float> offsetRaw = section.getFloatList("offset");
            Vector3f offset;
            if (offsetRaw.size() != 3) {
                if (!offsetRaw.isEmpty()) {
                    TorusLogger.error(Category.MODELS, "Invalid offset: " + Arrays.toString(offsetRaw.toArray(Float[]::new)));
                }
                offset = rendererId.equals("display_entities") ? new Vector3f(.5f, 0, .5f) : new Vector3f();
            } else {
                offset = new Vector3f(offsetRaw.get(0), offsetRaw.get(1), offsetRaw.get(2));
            }

            PartModelTemplate partModelTemplate = loader.load(section, partName, offset, variables);
            if (partModelTemplate != null) {
                template.add(partModelTemplate);
            }
        }

        return template;
    }

    private static final Pattern STATE_CONFIG_PATTERN = Pattern.compile("[a-z]+=[a-zA-Z0-9]+");
    private static final Map<StateType<?>, Function<String, Object>> stateParsers = Map.of(
      StateType.BOOLEAN,    Boolean::parseBoolean,
      StateType.INT,        Integer::parseInt
    );

    @SuppressWarnings({"unchecked", "deprecation"})
    public ModelController loadController(Structure structure, ConfigurationSection modelControllerSection) {
        if (modelControllerSection == null)
            return null;

        String rawModelType = modelControllerSection.getString("type");
        ConfigurationSection casesSection = modelControllerSection.getConfigurationSection("cases");

        if (rawModelType == null || casesSection == null)
            return null;

        ModelType modelType;
        if (rawModelType.equalsIgnoreCase("SINGLEPART")) {
            modelType = ModelType.SINGLEPART;
        } else if (rawModelType.equalsIgnoreCase("MULTIPART")) {
            modelType = ModelType.MULTIPART;
        } else return null;

        LinkedList<ModelCase> cases = new LinkedList<>();

        for (String rawStateSet : casesSection.getKeys(false)) {
            ConfigurationSection caseSection = casesSection.getConfigurationSection(rawStateSet);
            String rawModel = caseSection.getString("model");
            String animations = caseSection.getString("animations");

            Map<State<Object>, Object> states = new HashMap<>();

            // Load state conditions
            if (rawStateSet.startsWith("state[")) {
                int stateStartPos = rawStateSet.indexOf('[');
                String rawStates = rawStateSet.substring(stateStartPos + 1, rawStateSet.length() - 1);

                Matcher stateMatcher = STATE_CONFIG_PATTERN.matcher(rawStates);
                while (stateMatcher.find()) {
                    String[] stateKeyValuePair = rawStates.substring(stateMatcher.start(), stateMatcher.end()).split("=");

                    String key = stateKeyValuePair[0];
                    String rawValue = stateKeyValuePair[1];

                    State<?> state = structure.getAllowedStates().get(key);
                    if (state == null) {
                        TorusLogger.error(Category.MODELS, "Unrecognized state: " + key);
                        continue;
                    }

                    Function<String, Object> parser = stateParsers.get(state.type);
                    if (parser == null) {
                        TorusLogger.error(Category.MODELS, "There is no parser for state type: " + state.type);
                        continue;
                    }

                    Object value;
                    try {
                        value = parser.apply(rawValue);
                    } catch (Exception e) {
                        TorusLogger.error(Category.MODELS, "Error during parsing: '" + rawValue + "' is not a valid " + state.type);
                        continue;
                    }

                    states.put((State<Object>) state, value);
                }
            }

            if (!caseSection.isSet("model")) {
                TorusLogger.error(Category.MODELS, "Model path is not set.");
                continue;
            }

            Map<String, String> variables = new HashMap<>();
            if (caseSection.isConfigurationSection("variables")) {
                ConfigurationSection variablesSection = caseSection.getConfigurationSection("variables");
                for (String varName : variablesSection.getKeys(false)) {
                    variables.put(varName, variablesSection.getString(varName));
                }
            }


            ModelTemplate template = TorusPlugin.getInstance().getModelManager().getCached(rawModel);
            if (template == null || !variables.isEmpty()) {
                ResourceLocation modelLocation = new ResourceLocation(
                  structure.addon.externalContainer, "models/" + rawModel + ".model",
                  structure.addon.classpathContainer, "configs/torus/models/" + rawModel + ".model"
                );

                Resource modelResource = modelLocation.getResource();
                if (modelResource == null) {
                    TorusLogger.error(Category.MODELS, "Model does not exist at set path for '" + structure.namespacedId + "'");
                    continue;
                }

                template = TorusAPI.getModelLoader().load(modelResource, variables);
                if (template == null) {
                    TorusLogger.error(Category.MODELS, "Could not load model template.");
                    continue;
                }

                if (variables.isEmpty()) {
                    TorusPlugin.getInstance().getModelManager().cache(rawModel, template);
                }
            }

            if (rawStateSet.equals("fallback")) {
                cases.addLast(new ModelCase(states, template, animations));
            } else {
                cases.add(Math.max(0, cases.size() - 1), new ModelCase(states, template, animations));
            }
        }

        return new ModelController(modelType, cases);
    }

    public void registerRendererConfigLoader(RendererConfigLoader configLoader) {
        renderers.put(configLoader.id, configLoader);
    }

    @RequiresPlugin("ModelEngine")
    @Invoke(Invoke.Schedule.AFTER_PLUGIN_ENABLE)
    public void registerModelEngineProvider() {
        registerRendererConfigLoader(new ModelEngineRendererConfigLoader());
        TorusLogger.info(Category.GENERAL, "Successful integration with ModelEngine.");
    }

}
