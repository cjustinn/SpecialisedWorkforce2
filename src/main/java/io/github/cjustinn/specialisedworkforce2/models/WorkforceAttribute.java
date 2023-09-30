package io.github.cjustinn.specialisedworkforce2.models;

import io.github.cjustinn.specialisedworkforce2.enums.WorkforceAttributeType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkforceAttribute {
    public final WorkforceAttributeType type;
    public final int levelThreshold;
    private final Map<String, String> equations;
    private final List<String> targets;

    public WorkforceAttribute(ConfigurationSection section) {
        this.type = WorkforceAttributeType.valueOf(section.getString("type"));
        this.levelThreshold = section.getInt("levelThreshold");
        this.equations = new HashMap<String, String>() {{
            if (section.getKeys(false).contains("chance")) {
                put("chance", section.getString("chance"));
            }
            if (section.getKeys(false).contains("amount")) {
                put("amount", section.getString("amount"));
            }
        }};
        this.targets = section.getStringList("targets");
    }

    public String getEquation(String name) {
        return this.equations.containsKey(name) ? this.equations.get(name) : "0";
    }

    public boolean targets(String item) {
        return this.targets.stream().anyMatch((target) -> {
            final String itemKey = target.toLowerCase().replace("{*}", "");
            return item.toLowerCase().contains(itemKey);
        });
    }
}
