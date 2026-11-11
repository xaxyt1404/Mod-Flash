package com.flash.client;

import com.flash.FlashMod;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Barra principal (arriba de la barra de XP) y barra secundaria justo debajo. */
public final class FlashHud {
    private FlashHud() {}

    private static final int BAR_W = 182, BAR_H = 5;

    public static void register() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, FlashMod.id("hud"), FlashHud::render);
    }

    private static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!ClientState.unlocked || mc.player == null) return;

        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        int x = w / 2 - BAR_W / 2;
        int yEnergy = h - 66;           // por encima de la barra de XP / corazones
        int yDim = yEnergy + 8;         // debajo de la principal

        // --- Speed Force (0..100) ---
        int fill = (int) Math.round(BAR_W * (ClientState.energy / 100.0));
        int color = ClientState.fatigued ? 0xFFD83B3B : (ClientState.superSpeed ? 0xFFFFE066 : 0xFFE0A93A);
        g.fill(x - 1, yEnergy - 1, x + BAR_W + 1, yEnergy + BAR_H + 1, 0xCC000000);
        g.fill(x, yEnergy, x + fill, yEnergy + BAR_H, color);

        String label = Component.translatable("hud.flash.energy").getString()
                + " " + (int) ClientState.energy + "  [Nv " + ClientState.level + "]"
                + (ClientState.fatigued ? "  " + Component.translatable("hud.flash.fatigue").getString() : "");
        g.text(mc.font, label, x, yEnergy - 10, 0xFFFFFFFF, true);

        // --- Ruptura Dimensional (0..100 %) ---
        int fill2 = (int) Math.round(BAR_W * (ClientState.dim / 100.0));
        g.fill(x - 1, yDim - 1, x + BAR_W + 1, yDim + BAR_H + 1, 0xCC000000);
        g.fill(x, yDim, x + fill2, yDim + BAR_H, ClientState.breach ? 0xFFFFFFFF : 0xFF4FC3F7);
        if (ClientState.dim > 0 || ClientState.breach) {
            g.text(mc.font, Component.translatable("hud.flash.rift").getString() + " " + (int) ClientState.dim + "%",
                    x, yDim + 7, 0xFF9AD8F5, true);
        }
    }
}
