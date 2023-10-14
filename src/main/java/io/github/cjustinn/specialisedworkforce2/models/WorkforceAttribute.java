package io.github.cjustinn.specialisedworkforce2.models;

import io.github.cjustinn.specialisedworkforce2.enums.WorkforceAttributeType;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkforceAttribute {
    public final WorkforceAttributeType type;
    public final int levelThreshold;
    public final double paymentModifier;
    public final double experienceModifier;
    private final Map<String, String> equations;
    private final List<String> targets;

    public WorkforceAttribute(ConfigurationSection section) {
        this.type = WorkforceAttributeType.valueOf(section.getString("type"));
        this.levelThreshold = section.getInt("levelThreshold");
        this.paymentModifier = section.contains("paymentModifier") ? section.getDouble("paymentModifier") : 1.0;
        this.experienceModifier = section.contains("experienceModifier") ? section.getDouble("experienceModifier") : 1.0;
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
