package com.pvpclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.*;

/**
 * Style Manager: pick one style per collection (Crystal, Totem, Sword...) from ANY pack in
 * your resourcepacks folder, then Apply to build them into a single loadout pack.
 */
public class StyleManagerScreen extends Screen {
    private static final int CAT_X = 10, CAT_W = 100, CAT_Y = 40, CAT_H = 14;
    private static final int CARD_X = 120, CARD_Y = 62, CARD_W = 60, CARD_H = 64;

    private final Screen parent;
    private final StyleLibrary lib;
    private final Map<String, String> sel = Loadout.load();
    private final Map<String, Identifier> previews = new HashMap<>();
    private final Map<Identifier, int[]> sizes = new HashMap<>();
    private int catIdx = 0, catScroll = 0, rowScroll = 0, counter = 0;
    private TextFieldWidget search;
    private String status = "Put texture packs in your resourcepacks folder, pick styles, then Apply.";

    public StyleManagerScreen(Screen parent) {
        super(Text.literal("PvP Client - Style Manager"));
        this.parent = parent;
        this.lib = new StyleLibrary(MinecraftClient.getInstance().getResourcePackDir());
    }

    @Override
    protected void init() {
        String old = search != null ? search.getText() : "";
        clearChildren();
        search = new TextFieldWidget(textRenderer, CARD_X, 36, 160, 16, Text.literal("Search"));
        search.setText(old);
        search.setPlaceholder(Text.literal("Search styles..."));
        addDrawableChild(search);
        int by = height - 26;
        addDrawableChild(ButtonWidget.builder(Text.literal("Apply loadout"), b -> apply())
                .dimensions(CARD_X, by, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Reset all"), b -> { sel.clear(); status = "Reset - press Apply."; })
                .dimensions(CARD_X + 105, by, 70, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> close())
                .dimensions(CARD_X + 180, by, 60, 20).build());
    }

    private StyleLibrary.Category cat() { return StyleLibrary.ALL[catIdx]; }

    private List<StylePack> visible() {
        String q = search == null ? "" : search.getText().toLowerCase();
        List<StylePack> out = new ArrayList<>();
        for (StylePack p : lib.stylesFor(cat())) if (p.name.toLowerCase().contains(q)) out.add(p);
        return out;
    }

    private int cols() { return Math.max(1, (width - CARD_X - 10) / CARD_W); }
    private int rows() { return Math.max(1, (height - CARD_Y - 34) / CARD_H); }

    private Identifier preview(StylePack p) {
        String key = p.name + "|" + cat().name;
        if (previews.containsKey(key)) return previews.get(key);
        Identifier id = null;
        for (String f : cat().files) {
            byte[] b = p.read(f);
            if (b == null) continue;
            try {
                NativeImage ni = NativeImage.read(new ByteArrayInputStream(b));
                id = Identifier.of("pvpclient", "preview/p" + (counter++));
                MinecraftClient.getInstance().getTextureManager().registerTexture(id, new NativeImageBackedTexture(ni));
                sizes.put(id, new int[]{ni.getWidth(), ni.getHeight()});
            } catch (Exception ignored) { id = null; }
            break;
        }
        previews.put(key, id);
        return id;
    }

    private void apply() {
        MinecraftClient mc = MinecraftClient.getInstance();
        try {
            Path zip = Loadout.build(mc.getResourcePackDir(), lib, sel);
            Loadout.save(sel);
            ResourcePackManager m = mc.getResourcePackManager();
            m.scanPacks();
            LinkedHashSet<String> ids = new LinkedHashSet<>(m.getEnabledIds());
            ids.removeIf(i -> i.startsWith("file/" + Loadout.PREFIX));
            ids.add("file/" + zip.getFileName());
            m.setEnabledProfiles(ids);
            mc.options.refreshResourcePacks(m);
            mc.reloadResources();
            status = "Applied " + sel.size() + " style(s).";
        } catch (Exception e) {
            status = "Failed: " + e.getMessage();
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (mx >= CAT_X && mx < CAT_X + CAT_W && my >= CAT_Y) {
            int i = catScroll + (int) ((my - CAT_Y) / CAT_H);
            if (i >= 0 && i < StyleLibrary.ALL.length) { catIdx = i; rowScroll = 0; return true; }
        }
        if (mx >= CARD_X && my >= CARD_Y) {
            int col = (int) ((mx - CARD_X) / CARD_W), row = (int) ((my - CARD_Y) / CARD_H);
            if (col < cols() && row < rows()) {
                int idx = (rowScroll + row) * cols() + col; // 0 = Vanilla
                List<StylePack> list = visible();
                if (idx == 0) { sel.remove(cat().name); return true; }
                if (idx - 1 < list.size()) { sel.put(cat().name, list.get(idx - 1).name); return true; }
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        if (mx < CARD_X) {
            catScroll = Math.max(0, Math.min(StyleLibrary.ALL.length - 1, catScroll - (int) Math.signum(v)));
        } else {
            int total = (visible().size() + 1 + cols() - 1) / cols();
            rowScroll = Math.max(0, Math.min(Math.max(0, total - rows()), rowScroll - (int) Math.signum(v)));
        }
        return true;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawTextWithShadow(textRenderer, "STYLE MANAGER", 10, 14, 0xFFFFFF);

        // collections
        for (int i = catScroll, y = CAT_Y; i < StyleLibrary.ALL.length && y < height - 30; i++, y += CAT_H) {
            StyleLibrary.Category c = StyleLibrary.ALL[i];
            if (i == catIdx) ctx.fill(CAT_X - 2, y - 2, CAT_X + CAT_W, y + CAT_H - 3, 0x80408CFF);
            String tag = sel.containsKey(c.name) ? "* " : "";
            ctx.drawTextWithShadow(textRenderer, tag + c.name + " " + lib.stylesFor(c).size(), CAT_X, y, 0xDDDDDD);
        }

        // style cards
        List<StylePack> list = visible();
        ctx.drawTextWithShadow(textRenderer, cat().name + " - " + list.size() + " styles", CARD_X + 170, 40, 0xAAAAAA);
        int cols = cols(), rows = rows();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int idx = (rowScroll + r) * cols + c;
                if (idx > list.size()) break;
                StylePack p = idx == 0 ? null : list.get(idx - 1);
                int x = CARD_X + c * CARD_W, y = CARD_Y + r * CARD_H;
                boolean chosen = p == null ? !sel.containsKey(cat().name) : p.name.equals(sel.get(cat().name));
                ctx.fill(x, y, x + CARD_W - 4, y + CARD_H - 4, chosen ? 0xFF2F6FCF : 0xA0101820);
                String label = p == null ? "Vanilla" : p.name;
                if (p != null) {
                    Identifier id = preview(p);
                    if (id != null) {
                        int[] s = sizes.get(id);
                        int rh = (s[1] > s[0] && s[1] % s[0] == 0) ? s[0] : s[1];
                        float k = 36f / Math.max(s[0], rh);
                        ctx.drawTexture(id, x + 8, y + 4, Math.max(1, (int) (s[0] * k)), Math.max(1, (int) (rh * k)),
                                0f, 0f, s[0], rh, s[0], s[1]);
                    }
                }
                ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(label, CARD_W - 8), x + 2, y + CARD_H - 18, 0xFFFFFF);
            }
        }
        ctx.drawTextWithShadow(textRenderer, status, CARD_X + 250, height - 20, 0xAAAAAA);
    }

    @Override
    public void removed() {
        for (Identifier id : previews.values())
            if (id != null) MinecraftClient.getInstance().getTextureManager().destroyTexture(id);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }
}
