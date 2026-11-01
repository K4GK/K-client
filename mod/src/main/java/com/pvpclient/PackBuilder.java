package com.pvpclient;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Generates a small resource pack zip from toggled PvP tweaks. */
public class PackBuilder {
    public static final int[] COLORS = {0xFFFFFF, 0xFF3333, 0x33FF33, 0x33FFFF, 0xFFFF33, 0xFF66CC};
    /** Textures made in the editor or imported: id like item/diamond_sword -> image */
    public static final Map<String, BufferedImage> CUSTOM = new LinkedHashMap<>();

    public static final String[] COLOR_NAMES = {"White", "Red", "Green", "Cyan", "Yellow", "Pink"};

    public static Path build(Path dir, String name, boolean lowFire, boolean noPumpkin,
                             boolean crosshair, int colorIndex, boolean dot) throws IOException {
        Files.createDirectories(dir);
        Path out = dir.resolve(name + ".zip");
        try {
            Files.deleteIfExists(out);
        } catch (IOException locked) { // pack currently loaded (Windows file lock)
            name = name + "-" + (System.currentTimeMillis() / 1000);
            out = dir.resolve(name + ".zip");
        }
        try (ZipOutputStream z = new ZipOutputStream(Files.newOutputStream(out))) {
            put(z, "pack.mcmeta",
                "{\"pack\":{\"pack_format\":34,\"description\":\"Made with PvP Client\"}}"
                    .getBytes(StandardCharsets.UTF_8));
            if (lowFire) put(z, "assets/minecraft/textures/block/fire_1.png", png(blank(16, 16)));
            if (noPumpkin) put(z, "assets/minecraft/textures/misc/pumpkinblur.png", png(blank(16, 16)));
            if (crosshair) put(z, "assets/minecraft/textures/gui/sprites/hud/crosshair.png",
                    png(crosshair(COLORS[colorIndex], dot)));
            for (Map.Entry<String, BufferedImage> e : CUSTOM.entrySet())
                put(z, "assets/minecraft/textures/" + e.getKey() + ".png", png(e.getValue()));
        }
        return out;
    }

    private static BufferedImage blank(int w, int h) {
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB); // fully transparent
    }

    private static BufferedImage crosshair(int rgb, boolean dot) {
        BufferedImage img = blank(15, 15);
        int c = 0xFF000000 | rgb;
        if (dot) {
            for (int x = 6; x <= 8; x++) for (int y = 6; y <= 8; y++) img.setRGB(x, y, c);
        } else {
            for (int i = 0; i < 15; i++) {
                if (i >= 6 && i <= 8) continue; // gap in the middle
                img.setRGB(i, 7, c);
                img.setRGB(7, i, c);
            }
        }
        return img;
    }

    private static byte[] png(BufferedImage img) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        ImageIO.write(img, "png", b);
        return b.toByteArray();
    }

    private static void put(ZipOutputStream z, String path, byte[] data) throws IOException {
        z.putNextEntry(new ZipEntry(path));
        z.write(data);
        z.closeEntry();
    }
}
