package com.flash.data;

import com.flash.FlashMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

public final class FlashAttachments {
    private FlashAttachments() {}

    public static final AttachmentType<FlashData> FLASH_DATA = AttachmentRegistry.<FlashData>create(
            FlashMod.id("player_data"),
            builder -> builder
                    .initializer(() -> FlashData.DEFAULT)
                    .persistent(FlashData.CODEC)
                    .copyOnDeath());

    public static void init() { /* fuerza la carga de la clase */ }
}
