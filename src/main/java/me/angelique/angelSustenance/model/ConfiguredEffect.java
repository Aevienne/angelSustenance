package me.angelique.angelSustenance.model;

import org.bukkit.potion.PotionEffectType;

public final class ConfiguredEffect {

    private final PotionEffectType type;
    private final int durationSeconds;
    private final int amplifier;

    public ConfiguredEffect(PotionEffectType type, int durationSeconds, int amplifier) {
        this.type = type;
        this.durationSeconds = durationSeconds;
        this.amplifier = amplifier;
    }

    public PotionEffectType getType() {
        return type;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public int getAmplifier() {
        return amplifier;
    }
}
