package com.pvpclient;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Scans resourcepacks/ and works out which pack offers a style for which collection. */
public class StyleLibrary {
    public static class Category {
        public final String name;
        public final String[] files; // relative to assets/minecraft/textures/
        Category(String name, String... files) { this.name = name; this.files = files; }
    }

    public static final Category[] ALL = {
        new Category("Crystal", "entity/end_crystal/end_crystal.png", "entity/end_crystal/end_crystal_beam.png"),
        new Category("Obsidian", "block/obsidian.png"),
        new Category("Anchor", "block/respawn_anchor_top.png", "block/respawn_anchor_top_off.png",
                "block/respawn_anchor_bottom.png", "block/respawn_anchor_side0.png", "block/respawn_anchor_side1.png",
                "block/respawn_anchor_side2.png", "block/respawn_anchor_side3.png", "block/respawn_anchor_side4.png"),
        new Category("Glowstone", "block/glowstone.png"),
        new Category("Totem", "item/totem_of_undying.png"),
        new Category("Gaps & Pearls", "item/golden_apple.png", "item/ender_pearl.png"),
        new Category("Sword", "item/diamond_sword.png", "item/netherite_sword.png", "item/iron_sword.png"),
        new Category("Axe", "item/diamond_axe.png", "item/netherite_axe.png"),
        new Category("Mace", "item/mace.png"),
        new Category("Armor", "models/armor/diamond_layer_1.png", "models/armor/diamond_layer_2.png",
                "models/armor/netherite_layer_1.png", "models/armor/netherite_layer_2.png"),
        new Category("Shield", "entity/shield_base.png", "entity/shield_base_nopattern.png"),
        new Category("HUD", "gui/sprites/hud/hotbar.png", "gui/sprites/hud/hotbar_selection.png",
                "gui/sprites/hud/crosshair.png", "gui/sprites/hud/heart/full.png"),
        new Category("Fire", "block/fire_0.png", "block/fire_1.png"),
        new Category("Sky", "environment/sun.png", "environment/moon_phases.png", "environment/clouds.png"),
        new Category("Cobwebs", "block/cobweb.png"),
        new Category("Stone", "block/stone.png", "block/deepslate.png"),
        new Category("Grass & Snow", "block/grass_block_top.png", "block/grass_block_side.png", "block/snow.png"),
    };

    private final List<StylePack> packs = new ArrayList<>();
    private final Map<String, List<StylePack>> byCategory = new HashMap<>();

    public StyleLibrary(Path dir) {
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            for (Path p : ds) {
                String n = p.getFileName().toString();
                if (n.startsWith(Loadout.PREFIX) || n.startsWith("MyPvPPack")) continue;
                boolean ok = Files.isDirectory(p) || n.toLowerCase().endsWith(".zip");
                if (ok) packs.add(new StylePack(p));
            }
        } catch (IOException ignored) {}
        packs.sort(Comparator.comparing(p -> p.name.toLowerCase()));
        for (Category c : ALL) {
            List<StylePack> list = new ArrayList<>();
            for (StylePack p : packs) {
                for (String f : c.files) if (p.has(f)) { list.add(p); break; }
            }
            byCategory.put(c.name, list);
        }
    }

    public List<StylePack> stylesFor(Category c) { return byCategory.get(c.name); }

    public StylePack find(String name) {
        for (StylePack p : packs) if (p.name.equals(name)) return p;
        return null;
    }
}
