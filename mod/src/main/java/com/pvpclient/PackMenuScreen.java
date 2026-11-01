package com.pvpclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.text.Text;

import java.util.*;

public class PackMenuScreen extends Screen {
    private final Screen parent;
    private final ResourcePackManager manager;
    private final LinkedHashSet<String> enabled = new LinkedHashSet<>();
    private final List<ResourcePackProfile> available = new ArrayList<>();
    private final Map<String, List<String>> presets = Presets.load();
    private int page = 0;
    private static final int PER_PAGE = 4;

    public PackMenuScreen(Screen parent) {
        super(Text.literal("PvP Client - Texture Packs"));
        this.parent = parent;
        this.manager = MinecraftClient.getInstance().getResourcePackManager();
        manager.scanPacks();
        enabled.addAll(manager.getEnabledIds());
        for (ResourcePackProfile p : manager.getProfiles()) {
            if (!p.isPinned()) available.add(p); // skip vanilla/built-in pinned packs
        }
    }

    @Override
    protected void init() {
        clearChildren();
        int cx = width / 2;
        int y = 40;
        int start = page * PER_PAGE;
        for (int i = start; i < Math.min(available.size(), start + PER_PAGE); i++) {
            ResourcePackProfile p = available.get(i);
            boolean on = enabled.contains(p.getId());
            addDrawableChild(ButtonWidget.builder(
                    Text.literal((on ? "[ON]  " : "[OFF] ") + p.getDisplayName().getString()),
                    b -> { toggle(p.getId()); init(); }
            ).dimensions(cx - 120, y, 240, 20).build());
            y += 24;
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> { if (page > 0) { page--; init(); } })
                .dimensions(cx - 120, y + 4, 40, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
            if ((page + 1) * PER_PAGE < available.size()) { page++; init(); }
        }).dimensions(cx + 80, y + 4, 40, 20).build());

        // Presets: save current selection as "pvp", or load it back
        addDrawableChild(ButtonWidget.builder(Text.literal("Save as preset"), b -> {
            presets.put("pvp", new ArrayList<>(enabled));
            Presets.save(presets);
        }).dimensions(cx - 120, y + 32, 118, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Load preset"), b -> {
            List<String> saved = presets.get("pvp");
            if (saved != null) { enabled.clear(); enabled.addAll(saved); init(); }
        }).dimensions(cx + 2, y + 32, 118, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Apply"), b -> apply())
                .dimensions(cx - 120, y + 58, 118, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> close())
                .dimensions(cx + 2, y + 58, 118, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Style Manager"),
                b -> MinecraftClient.getInstance().setScreen(new StyleManagerScreen(this)))
                .dimensions(cx - 120, y + 84, 118, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Pack Builder"),
                b -> MinecraftClient.getInstance().setScreen(new PackBuilderScreen(this)))
                .dimensions(cx + 2, y + 84, 118, 20).build());
    }

    private void toggle(String id) {
        if (!enabled.remove(id)) enabled.add(id); // newly enabled packs go on top of the stack
    }

    private void apply() {
        MinecraftClient mc = MinecraftClient.getInstance();
        manager.setEnabledProfiles(enabled);
        mc.options.refreshResourcePacks(manager);
        mc.reloadResources();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 16, 0xFFFFFF);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }
}
