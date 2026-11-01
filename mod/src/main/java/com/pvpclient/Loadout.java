package com.pvpclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Your picks: collection -> pack name. Builds them into one generated pack. */
public class Loadout {
    public static final String PREFIX = "PvPClient-Loadout";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("pvpclient-loadout.json");

    public static Map<String, String> load() {
        try (Reader r = Files.newBufferedReader(FILE)) {
            Map<String, String> m = GSON.fromJson(r, new TypeToken<LinkedHashMap<String, String>>() {}.getType());
            return m != null ? m : new LinkedHashMap<>();
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    public static void save(Map<String, String> sel) {
        try (Writer w = Files.newBufferedWriter(FILE)) { GSON.toJson(sel, w); } catch (Exception ignored) {}
    }

    public static Path build(Path dir, StyleLibrary lib, Map<String, String> sel) throws IOException {
        Path out = dir.resolve(PREFIX + ".zip");
        try { Files.deleteIfExists(out); }
        catch (IOException locked) { out = dir.resolve(PREFIX + "-" + (System.currentTimeMillis() / 1000) + ".zip"); }
        Set<String> written = new HashSet<>();
        try (ZipOutputStream z = new ZipOutputStream(Files.newOutputStream(out))) {
            put(z, written, "pack.mcmeta",
                "{\"pack\":{\"pack_format\":34,\"description\":\"PvP Client loadout\"}}".getBytes(StandardCharsets.UTF_8));
            for (StyleLibrary.Category c : StyleLibrary.ALL) {
                String packName = sel.get(c.name);
                if (packName == null) continue;
                StylePack p = lib.find(packName);
                if (p == null) continue;
                for (String f : c.files) {
                    byte[] b = p.read(f);
                    if (b == null) continue;
                    put(z, written, StylePack.ROOT + f, b);
                    byte[] meta = p.read(f + ".mcmeta"); // keeps animations (fire etc.)
                    if (meta != null) put(z, written, StylePack.ROOT + f + ".mcmeta", meta);
                }
            }
        }
        // tidy up older generated loadout packs (best effort; locked files are skipped)
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, PREFIX + "*.zip")) {
            for (Path p : ds) if (!p.equals(out)) { try { Files.deleteIfExists(p); } catch (IOException ignored) {} }
        }
        return out;
    }

    private static void put(ZipOutputStream z, Set<String> written, String path, byte[] data) throws IOException {
        if (!written.add(path)) return;
        z.putNextEntry(new ZipEntry(path));
        z.write(data);
        z.closeEntry();
    }
}
