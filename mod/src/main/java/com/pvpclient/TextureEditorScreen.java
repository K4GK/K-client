package com.pvpclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.resource.Resource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Pixel editor + PNG import. Type a texture id (e.g. item/diamond_sword),
 * paint it, or drag a PNG file from your PC onto the window.
 * Saved textures go into the pack the next time you press Create & Enable Pack.
 */
public class TextureEditorScreen extends Screen {
    private static final int[] PAL = {
            0x00000000, 0xFF000000, 0xFFFFFFFF, 0xFF808080,
            0xFFC0C0C0, 0xFFFF0000, 0xFFFF8000, 0xFFFFFF00,
            0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFF8000FF,
            0xFFFF66CC, 0xFF804000, 0xFF40FFC0, 0xFFFFD700};

    private final Screen parent;
    private TextFieldWidget idField;
    private String savedId = "item/diamond_sword";
    private BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
    private int sel = 1;
    private String status = "Drag a PNG file onto this window to import it.";
    private int gx, gy, cell;

    public TextureEditorScreen(Screen parent) {
        super(Text.literal("PvP Client - Texture Editor"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (idField != null) savedId = idField.getText();
        clearChildren();
        cell = Math.max(2, Math.min(10, 160 / img.getWidth()));
        gx = width / 2 - 190;
        gy = 54;
        idField = new TextFieldWidget(textRenderer, width / 2 - 100, 28, 200, 18, Text.literal("Texture id"));
        idField.setMaxLength(100);
        idField.setText(savedId);
        addDrawableChild(idField);

        int by = height - 28, bx = width / 2 - 190;
        addDrawableChild(ButtonWidget.builder(Text.literal("Load vanilla"), b -> loadVanilla())
                .dimensions(bx, by, 90, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Save to pack"), b -> save())
                .dimensions(bx + 95, by, 90, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Clear"), b -> {
            img = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        }).dimensions(bx + 190, by, 90, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> close())
                .dimensions(bx + 285, by, 90, 20).build());
    }

    private static String normalize(String id) {
        id = id.trim().replace("minecraft:", "");
        if (id.startsWith("textures/")) id = id.substring(9);
        if (id.endsWith(".png")) id = id.substring(0, id.length() - 4);
        return id;
    }

    private static BufferedImage toArgb(BufferedImage src) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        out.getGraphics().drawImage(src, 0, 0, null);
        return out;
    }

    private void loadVanilla() {
        String id = normalize(idField.getText());
        try {
            Optional<Resource> r = MinecraftClient.getInstance().getResourceManager()
                    .getResource(Identifier.of("minecraft", "textures/" + id + ".png"));
            if (r.isEmpty()) { status = "No texture named " + id; return; }
            try (InputStream in = r.get().getInputStream()) {
                img = toArgb(ImageIO.read(in));
            }
            status = "Loaded " + id + " - paint, then Save to pack.";
            init();
        } catch (Exception e) {
            status = "Could not load: " + e.getMessage();
        }
    }

    private void save() {
        PackBuilder.CUSTOM.put(normalize(idField.getText()), toArgb(img));
        status = "Saved " + normalize(idField.getText()) + " (" + PackBuilder.CUSTOM.size()
                + " custom). Now press Create & Enable Pack.";
    }

    @Override
    public void filesDragged(List<Path> paths) {
        for (Path p : paths) {
            if (!p.toString().toLowerCase().endsWith(".png")) continue;
            try {
                BufferedImage in = ImageIO.read(p.toFile());
                if (in == null) continue;
                img = toArgb(in);
                savedId = normalize(idField.getText());
                PackBuilder.CUSTOM.put(savedId, toArgb(img));
                status = "Imported " + p.getFileName() + " as " + savedId + ". Press Create & Enable Pack.";
                init();
                return;
            } catch (Exception e) {
                status = "Import failed: " + e.getMessage();
            }
        }
    }

    private boolean paint(double mx, double my, int button) {
        int px = (int) Math.floor((mx - gx) / cell), py = (int) Math.floor((my - gy) / cell);
        if (px >= 0 && py >= 0 && px < img.getWidth() && py < img.getHeight()) {
            img.setRGB(px, py, button == 1 ? 0 : PAL[sel]);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        int palX = width / 2 + 40, palY = 54;
        int col = (int) Math.floor((mx - palX) / 20.0), row = (int) Math.floor((my - palY) / 20.0);
        if (col >= 0 && col < 4 && row >= 0 && row < 4) { sel = row * 4 + col; return true; }
        return paint(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (super.mouseDragged(mx, my, button, dx, dy)) return true;
        return paint(mx, my, button);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFF);
        // grid
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int argb = img.getRGB(x, y);
                int bg = ((x + y) % 2 == 0) ? 0xFF3A3A3A : 0xFF2A2A2A;
                ctx.fill(gx + x * cell, gy + y * cell, gx + (x + 1) * cell, gy + (y + 1) * cell, bg);
                if ((argb >>> 24) != 0)
                    ctx.fill(gx + x * cell, gy + y * cell, gx + (x + 1) * cell, gy + (y + 1) * cell, argb | 0xFF000000);
            }
        }
        // palette (first swatch = eraser)
        for (int i = 0; i < PAL.length; i++) {
            int x = width / 2 + 40 + (i % 4) * 20, y = 54 + (i / 4) * 20;
            ctx.fill(x - 1, y - 1, x + 17, y + 17, i == sel ? 0xFFFFFFFF : 0xFF000000);
            ctx.fill(x, y, x + 16, y + 16, (PAL[i] >>> 24) == 0 ? 0xFF555555 : PAL[i]);
        }
        ctx.drawTextWithShadow(textRenderer, "Left click paint, right click erase", width / 2 + 40, 140, 0xAAAAAA);
        ctx.drawCenteredTextWithShadow(textRenderer, status, width / 2, height - 42, 0xAAAAAA);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }
}
