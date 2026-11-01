package com.pvpclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.text.Text;

import java.nio.file.Path;
import java.util.LinkedHashSet;

public class PackBuilderScreen extends Screen {
    private final Screen parent;
    private boolean lowFire = true, noPumpkin = true, crosshair = true, dot = false;
    private int color = 0;
    private String status = "";

    public PackBuilderScreen(Screen parent) {
        super(Text.literal("PvP Client - Pack Builder"));
        this.parent = parent;
    }

    private static String onOff(boolean b) { return b ? "ON" : "OFF"; }

    @Override
    protected void init() {
        clearChildren();
        int cx = width / 2, y = 40;
        addDrawableChild(ButtonWidget.builder(Text.literal("Hide fire overlay: " + onOff(lowFire)),
                b -> { lowFire = !lowFire; init(); }).dimensions(cx - 120, y, 240, 20).build());
        y += 24;
        addDrawableChild(ButtonWidget.builder(Text.literal("Hide pumpkin blur: " + onOff(noPumpkin)),
                b -> { noPumpkin = !noPumpkin; init(); }).dimensions(cx - 120, y, 240, 20).build());
        y += 24;
        addDrawableChild(ButtonWidget.builder(Text.literal("Custom crosshair: " + onOff(crosshair)),
                b -> { crosshair = !crosshair; init(); }).dimensions(cx - 120, y, 240, 20).build());
        y += 24;
        addDrawableChild(ButtonWidget.builder(Text.literal("Crosshair color: " + PackBuilder.COLOR_NAMES[color]),
                b -> { color = (color + 1) % PackBuilder.COLORS.length; init(); }).dimensions(cx - 120, y, 240, 20).build());
        y += 24;
        addDrawableChild(ButtonWidget.builder(Text.literal("Crosshair style: " + (dot ? "Dot" : "Cross")),
                b -> { dot = !dot; init(); }).dimensions(cx - 120, y, 240, 20).build());
        y += 28;
        addDrawableChild(ButtonWidget.builder(Text.literal("Edit / import textures (" + PackBuilder.CUSTOM.size() + ")"),
                b -> MinecraftClient.getInstance().setScreen(new TextureEditorScreen(this)))
                .dimensions(cx - 120, y, 240, 20).build());
        y += 28;
        addDrawableChild(ButtonWidget.builder(Text.literal("Create & Enable Pack"), b -> create())
                .dimensions(cx - 120, y, 118, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> close())
                .dimensions(cx + 2, y, 118, 20).build());
    }

    private void create() {
        MinecraftClient mc = MinecraftClient.getInstance();
        try {
            Path zip = PackBuilder.build(mc.getResourcePackDir(), "MyPvPPack",
                    lowFire, noPumpkin, crosshair, color, dot);
            ResourcePackManager m = mc.getResourcePackManager();
            m.scanPacks();
            LinkedHashSet<String> ids = new LinkedHashSet<>(m.getEnabledIds());
            ids.add("file/" + zip.getFileName().toString());
            m.setEnabledProfiles(ids);
            mc.options.refreshResourcePacks(m);
            mc.reloadResources();
            status = "Created " + zip.getFileName();
        } catch (Exception e) {
            status = "Failed: " + e.getMessage();
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 16, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, status, width / 2, height - 30, 0xAAAAAA);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }
}
