package com.flash.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Datos PERSISTENTES del jugador (sobreviven a reinicios y a la muerte). Inmutable. */
public record FlashData(boolean unlocked, int speedLevel) {
    public static final FlashData DEFAULT = new FlashData(false, 1);

    public static final Codec<FlashData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("unlocked", false).forGetter(FlashData::unlocked),
            Codec.intRange(1, 5).optionalFieldOf("speed_level", 1).forGetter(FlashData::speedLevel)
    ).apply(i, FlashData::new));

    public FlashData withUnlocked(boolean v) { return new FlashData(v, speedLevel); }
    public FlashData withLevel(int lvl) { return new FlashData(unlocked, Math.max(1, Math.min(5, lvl))); }
}
