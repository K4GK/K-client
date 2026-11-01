package com.pvpclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Saves named lists of enabled resource pack ids to config/pvpclient-presets.json */
public class Presets {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("pvpclient-presets.json");

    public static Map<String, List<String>> load() {
        try (Reader r = Files.newBufferedReader(FILE)) {
            Map<String, List<String>> m = GSON.fromJson(r, new TypeToken<LinkedHashMap<String, List<String>>>() {}.getType());
            return m != null ? m : new LinkedHashMap<>();
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    public static void save(Map<String, List<String>> presets) {
        try (Writer w = Files.newBufferedWriter(FILE)) {
            GSON.toJson(presets, w);
        } catch (Exception ignored) {}
    }
}
