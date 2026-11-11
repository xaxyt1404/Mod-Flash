package com.flash.client;

import com.flash.net.FlashNetwork.ChooseDimension;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Menú sencillo para elegir destino al cruzar la brecha dimensional. */
public class DimensionScreen extends Screen {
    private boolean chosen;

    public DimensionScreen() {
        super(Component.translatable("gui.flash.dimension.title"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 - 55;

        addRenderableWidget(new StringWidget(cx - 110, y, 220, 12, this.title, this.font));
        addRenderableWidget(option(cx, y + 20, "gui.flash.dimension.overworld", 0));
        addRenderableWidget(option(cx, y + 44, "gui.flash.dimension.nether", 1));
        addRenderableWidget(option(cx, y + 68, "gui.flash.dimension.end", 2));
        addRenderableWidget(Button.builder(Component.translatable("gui.flash.dimension.cancel"), b -> onClose())
                .bounds(cx - 50, y + 98, 100, 20).build());
    }

    private Button option(int cx, int y, String key, int index) {
        return Button.builder(Component.translatable(key), b -> {
            chosen = true;
            ClientPlayNetworking.send(new ChooseDimension(index));
            super.onClose();
        }).bounds(cx - 75, y, 150, 20).build();
    }

    @Override
    public void onClose() {
        if (!chosen) ClientPlayNetworking.send(new ChooseDimension(-1));   // cerró sin elegir
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
