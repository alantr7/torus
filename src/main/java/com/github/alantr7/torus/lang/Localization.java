package com.github.alantr7.torus.lang;

import com.github.alantr7.bukkitplugin.annotations.core.Inject;
import com.github.alantr7.bukkitplugin.annotations.core.Invoke;
import com.github.alantr7.bukkitplugin.annotations.core.Singleton;
import com.github.alantr7.torus.TorusPlugin;
import com.github.alantr7.torus.api.resource.Container;
import com.github.alantr7.torus.api.resource.Resource;
import com.github.alantr7.torus.api.resource.ResourceLocation;
import com.github.alantr7.torus.config.MainConfig;
import com.github.alantr7.torus.log.Category;
import com.github.alantr7.torus.log.TorusLogger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

@Singleton
public class Localization {

    private Locale fallback;

    private Locale locale;

    private final Set<String> keys = new LinkedHashSet<>();

    @Inject
    static Localization instance;

    @Invoke(Invoke.Schedule.AFTER_PLUGIN_ENABLE)
    void init() {
        // load all keys from bundled english file
        fallback = Objects.requireNonNull(loadLocale(TorusPlugin.getInstance().getResource("locales/en.properties"), null));
        keys.addAll(fallback.dictionary.keySet());

        setLocale(fallback);

        // attempt to load a locale
        reload();
    }

    public void reload() {
        if (MainConfig.LOCALE != null) {
            ResourceLocation localeLocation = new ResourceLocation(
              Container.directory(TorusPlugin.getInstance().getDataFolder()), "locales/" + MainConfig.LOCALE + ".properties",
              Container.classpath(TorusPlugin.getInstance()), "locales/" + MainConfig.LOCALE + ".properties"
            );
            Resource resource = localeLocation.getResource();
            if (resource != null && resource.stream != null) {
                Locale locale = loadLocale(resource.stream);
                if (locale != null) {
                    this.locale = locale;
                    TorusLogger.info(Category.GENERAL, "Loaded locale '" + MainConfig.LOCALE + "'.");
                }
            }
        }
    }

    @Nullable
    public Locale loadLocale(InputStream is) {
        return loadLocale(is, fallback);
    }

    private Locale loadLocale(InputStream is, Locale fallback) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            Locale locale = new Locale();
            for (String line; (line = br.readLine()) != null;) {
                if (line.isBlank())
                    continue;

                int separator = line.indexOf('=');
                if (separator == -1) {
                    TorusLogger.error(Category.GENERAL, "Invalid line while parsing locale: '" + line + "'");
                    continue;
                }

                String key = line.substring(0, separator);

                // todo: multiple lines
                String value = ChatColor.translateAlternateColorCodes('&', line.substring(separator + 1));
                locale.dictionary.put(key, value);
            }

            // add missing lines by using the fallback locale
            if (fallback != null) {
                fallback.dictionary.forEach((key, val) -> {
                    if (!locale.dictionary.containsKey(key))
                        locale.dictionary.put(key, val);
                });
            }

            return locale;
        } catch (Exception ex) {
            TorusLogger.error(Category.GENERAL, "Could not load locale: " + ex.getMessage());
        }
        return null;
    }

    public void setLocale(@NotNull Locale locale) {
        this.locale = locale;
    }

    @NotNull
    public static String translate(String key) {
        return instance.locale.dictionary.getOrDefault(key, key);
    }

    @NotNull
    public static Translatable translatable(String key) {
        return new Translatable(key);
    }

}
