package com.cakeauto.client;

import com.cakeauto.CakeAutoPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class CakeAutoScreen extends Screen {

    private boolean autoEnabled;
    private int searchRadius;

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

        addDrawableChild(ButtonWidget.builder(
                        getAutoLabel(),
                        btn -> {
                            autoEnabled = !autoEnabled;
                            btn.setMessage(getAutoLabel());
                            ClientPlayNetworking.send(new CakeAutoPackets.ToggleAutoPayload(autoEnabled));
                        })
                .dimensions(x + 10, y + 40, WIN_W - 20, 20)
                .build());

        addDrawableChild(new RadiusSlider(x + 10, y + 75, WIN_W - 20, 20, searchRadius));

        addDrawableChild(ButtonWidget.builder(
                        Text.literal("Найти и открыть верстак"),
                        btn -> {
                            ClientPlayNetworking.send(new CakeAutoPackets.AutoOpenTablePayload(searchRadius));
                            close();
                        })
                .dimensions(x + 10, y + 110, WIN_W - 20, 20)
                .build());

        addDrawableChild(ButtonWidget.builder(
                        Text.literal("Закрыть"),
                        btn -> close())
                .dimensions(x + 10, y + 145, WIN_W - 20, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);

        int x = (width - WIN_W) / 2;
        int y = (height - WIN_H) / 2;

        context.fill(x, y, x + WIN_W, y + WIN_H, 0xCC000000);
        context.drawBorder(x, y, WIN_W, WIN_H, 0xFFFFAA00);
        context.drawCenteredTextWithShadow(textRenderer, "CakeAuto", x + WIN_W / 2, y + 10, 0xFFFFAA00);
        context.drawTextWithShadow(textRenderer,
                Text.literal("молоко x3, сахар x2, яйцо x1, пшеница x3"),
                x + 10, y + 28, 0xFFCCCCCC);
        context.drawTextWithShadow(textRenderer,
                Text.literal("Радиус поиска: " + searchRadius + " блоков"),
                x + 10, y + 63, 0xFFCCCCCC);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() { return false; }

    private Text getAutoLabel() {
        String prefix = autoEnabled ? "§a✔ " : "§7✗ ";
        return Text.literal(prefix + "Авто-заполнять рецепт торта");
    }

    private class RadiusSlider extends SliderWidget {
        private static final int MIN = 1;
        private static final int MAX = 20;

        RadiusSlider(int x, int y, int width, int height, int initial) {
            super(x, y, width, height,
                    Text.literal("Радиус: " + initial),
                    (initial - MIN) / (double)(MAX - MIN));
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal("Радиус: " + getVal() + " блоков"));
            searchRadius = getVal();
        }

        @Override
        protected void applyValue() { searchRadius = getVal(); }

        private int getVal() {
            return MIN + (int) Math.round(value * (MAX - MIN));
        }
    }
}
