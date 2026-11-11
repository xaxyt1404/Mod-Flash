package com.flash.client;

import com.flash.net.FlashNetwork.ChooseDimension;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Menú sencillo para elegir destino al cruzar la brecha dimensional.
 */
public class DimensionScreen extends Screen {
    private boolean chosen = false;

    public DimensionScreen() {
        super(Component.translatable("gui.flash.dimension.title"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 - 55;

        addRenderableWidget(new StringWidget(cx - 110, y, 220, 12, this.title, this.font));

        ResourceKey<Level> currentDim = (this.minecraft != null && this.minecraft.level != null)
                ? this.minecraft.level.dimension()
                : null;

        int buttonY = y + 20;

        if (currentDim != Level.OVERWORLD) {
            addRenderableWidget(option(cx, buttonY, "gui.flash.dimension.overworld", 0));
            buttonY += 24;
        }
        if (currentDim != Level.NETHER) {
            addRenderableWidget(option(cx, buttonY, "gui.flash.dimension.nether", 1));
            buttonY += 24;
        }
        if (currentDim != Level.END) {
            addRenderableWidget(option(cx, buttonY, "gui.flash.dimension.end", 2));
            buttonY += 24;
        }

        // Botón Cancelar alinear al final de la lista de opciones
        addRenderableWidget(Button.builder(Component.translatable("gui.flash.dimension.cancel"), b -> this.onClose())
                .bounds(cx - 50, buttonY + 6, 100, 20).build());
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
        if (!chosen) {
            ClientPlayNetworking.send(new ChooseDimension(-1)); // Notificar al servidor que se canceló
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}