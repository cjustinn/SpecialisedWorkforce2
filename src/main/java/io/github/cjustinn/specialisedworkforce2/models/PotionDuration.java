package io.github.cjustinn.specialisedworkforce2.models;

import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.HashMap;
import java.util.Map;

public class PotionDuration {
    public static Map<PotionEffectType, PotionDuration> standards = new HashMap<PotionEffectType, PotionDuration>() {{
        put(PotionEffectType.REGENERATION, new PotionDuration(900, 1800, 450));
        put(PotionEffectType.SPEED, new PotionDuration(3600, 9600, 1800));
        put(PotionEffectType.FIRE_RESISTANCE, new PotionDuration(3600, 9600, 0));
        put(PotionEffectType.HEAL, new PotionDuration(0, 0, 0));
        put(PotionEffectType.NIGHT_VISION, new PotionDuration(3600, 9600, 0));
        put(PotionEffectType.INCREASE_DAMAGE, new PotionDuration(3600, 9600, 1800));
        put(PotionEffectType.JUMP, new PotionDuration(3600, 9600, 1800));
        put(PotionEffectType.WATER_BREATHING, new PotionDuration(3600, 9600, 0));
        put(PotionEffectType.INVISIBILITY, new PotionDuration(3600, 9600, 0));
        put(PotionEffectType.SLOW_FALLING, new PotionDuration(1800, 4800, 0));
        put(PotionEffectType.LUCK, new PotionDuration(6000, 0, 0));
        put(PotionEffectType.POISON, new PotionDuration(900, 1800, 440));
        put(PotionEffectType.WEAKNESS, new PotionDuration(1800, 4800, 0));
        put(PotionEffectType.SLOW, new PotionDuration(1800, 4800, 400));
        put(PotionEffectType.HARM, new PotionDuration(0, 0, 0));
    }};

    private int base;
    private int upgraded;
    private int extended;

    public PotionDuration(int b, int e, int u) {
        this.base = b;
        this.extended = e;
        this.upgraded = u;
    }

    public int getBaseDuration() { return this.base; }
    public int getExtendedDuration() { return this.extended; }
    public int getUpgradedDuration() { return this.upgraded; }
}
