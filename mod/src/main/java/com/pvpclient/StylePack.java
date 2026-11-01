package com.pvpclient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** A resource pack (zip or folder) in the resourcepacks directory, used as a source of styles. */
public class StylePack {
    public static final String ROOT = "assets/minecraft/textures/";

    public final String name;
    private final Path path;
    private final boolean zip;
    private String prefix = "";
    private final Set<String> entries = new HashSet<>();

    public StylePack(Path path) {
        this.path = path;
        this.zip = Files.isRegularFile(path);
        String n = path.getFileName().toString();
        this.name = zip && n.toLowerCase().endsWith(".zip") ? n.substring(0, n.length() - 4) : n;
        if (zip) {
            try (ZipFile zf = new ZipFile(path.toFile())) {
                zf.stream().map(ZipEntry::getName).forEach(entries::add);
            } catch (IOException ignored) {}
            for (String e : entries) {
                if (e.endsWith("pack.mcmeta")) { prefix = e.substring(0, e.length() - "pack.mcmeta".length()); break; }
            }
        }
    }

    /** rel is relative to assets/minecraft/textures/, e.g. block/obsidian.png */
    public boolean has(String rel) {
        return zip ? entries.contains(prefix + ROOT + rel) : Files.exists(path.resolve(ROOT + rel));
    }

    public byte[] read(String rel) {
        try {
            if (zip) {
                try (ZipFile zf = new ZipFile(path.toFile())) {
                    ZipEntry e = zf.getEntry(prefix + ROOT + rel);
                    if (e == null) return null;
                    try (InputStream in = zf.getInputStream(e)) { return in.readAllBytes(); }
                }
            }
            Path f = path.resolve(ROOT + rel);
            return Files.exists(f) ? Files.readAllBytes(f) : null;
        } catch (IOException e) {
            return null;
        }
    }
}
