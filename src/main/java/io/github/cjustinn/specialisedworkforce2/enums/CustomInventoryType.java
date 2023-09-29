package io.github.cjustinn.specialisedworkforce2.enums;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

public enum CustomInventoryType {
    STATUS("Your Professions"),
    JOIN("Join Profession"),
    LEADERBOARD("Global Professions");

    public final TextComponent title;
    CustomInventoryType(String title) {
        this.title = Component.text(title);
    }
}
