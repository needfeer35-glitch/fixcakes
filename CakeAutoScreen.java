package com.cakeauto.client;

import com.cakeauto.CakeAutoPackets;
import net.fabricmc.fabric.api.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class CakeAutoScreen extends Screen {

    private boolean autoEnabled;
    private int searchRadius;

    // UI layout constants
    private static final int WIN_W = 220;
    private static final int WIN_H = 200;

    public CakeAutoScreen(boolean autoEnabled, int searchRadius) {
        super(Text.literal("CakeAuto"));
        this.autoEnabled = autoEnabled;
        this.searchRadius = searchRadius;
    }

    @Override
    protected void init() {
        int x = (width - WIN_W) / 2;
        int y = (height - WIN_H) / 2;

        // ── Checkbox / toggle: auto-fill crafting grid ──────────────────────
        addDrawableChild(ButtonWidget.builder(
                        getAutoLabel(),
                        btn -> {
                            autoEnabled = !autoEnabled;
                            btn.setMessage(getAutoLabel());
                            ClientPlayNetworking.send(new CakeAutoPackets.ToggleAutoPayload(autoEnabled));
                        })
                .dimensions(x + 10, y + 40, WIN_W - 20, 20)
                .build());

        // ── Radius slider ────────────────────────────────────────────────────
        addDrawableChild(new RadiusSlider(x + 10, y + 75, WIN_W - 20, 20, searchRadius));

        // ── Button: Auto-find and open crafting table ────────────────────────
        addDrawableChild(ButtonWidget.builder(
                        Text.literal("🔍 Найти и открыть верстак"),
                        btn -> {
                            ClientPlayNetworking.send(new CakeAutoPackets.AutoOpenTablePayload(searchRadius));
                            close();
                        })
                .dimensions(x + 10, y + 110, WIN_W - 20, 20)
                .build());

        // ── Button: Open crafting table (manual / nearby) ───────────────────
        addDrawableChild(ButtonWidget.builder(
                        Text.literal("Открыть верстак с тортом"),
                        btn -> {
                            // Same as auto-open but also closes GUI
                            ClientPlayNetworking.send(new CakeAutoPackets.AutoOpenTablePayload(searchRadius));
                            close();
                        })
                .dimensions(x + 10, y + 145, WIN_W - 20, 20)
                .build());

        // ── Button: Close ────────────────────────────────────────────────────
        addDrawableChild(ButtonWidget.builder(
                        Text.literal("Закрыть"),
                        btn -> close())
                .dimensions(x + 10, y + 170, WIN_W - 20, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Dim background
        renderBackground(context, mouseX, mouseY, delta);

        int x = (width - WIN_W) / 2;
        int y = (height - WIN_H) / 2;

        // Window background
        context.fill(x, y, x + WIN_W, y + WIN_H, 0xCC000000);
        context.drawBorder(x, y, WIN_W, WIN_H, 0xFFFFAA00);

        // Title
        context.drawCenteredTextWithShadow(textRenderer,
                "🎂 CakeAuto", x + WIN_W / 2, y + 10, 0xFFFFAA00);

        // Recipe hint
        context.drawTextWithShadow(textRenderer,
                Text.literal("молоко×3, сахар×2, яйцо×1, пшеница×3"),
                x + 10, y + 28, 0xFFCCCCCC);

        // Radius label
        context.drawTextWithShadow(textRenderer,
                Text.literal("Радиус поиска верстака: " + searchRadius + " блоков"),
                x + 10, y + 63, 0xFFCCCCCC);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private Text getAutoLabel() {
        String prefix = autoEnabled ? "§a✔ " : "§7✗ ";
        return Text.literal(prefix + "Автоматически заполнять рецепт торта");
    }

    // ── Inner slider for radius ──────────────────────────────────────────────
    private class RadiusSlider extends SliderWidget {
        private static final int MIN = 1;
        private static final int MAX = 20;

        RadiusSlider(int x, int y, int width, int height, int initial) {
            super(x, y, width, height,
                    Text.literal("Радиус: " + initial),
                    (initial - MIN) / (double) (MAX - MIN));
        }

        @Override
        protected void updateMessage() {
            int v = getRadiusValue();
            setMessage(Text.literal("Радиус: " + v + " блоков"));
            searchRadius = v;
        }

        @Override
        protected void applyValue() {
            searchRadius = getRadiusValue();
        }

        private int getRadiusValue() {
            return MIN + (int) Math.round(value * (MAX - MIN));
        }
    }
}
