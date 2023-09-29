package io.github.cjustinn.specialisedworkforce2.models;

import io.github.cjustinn.specialisedworkforce2.enums.WorkforceAttributeType;
import io.github.cjustinn.specialisedworkforce2.services.EconomyService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WorkforceProfession {
    private final boolean paymentEnabled;

    public final String id;
    public final String name;
    public final String group;
    public final String icon;
    public final int customModelData;
    public final String paymentEquation;
    public final List<TextComponent> description;

    public final List<WorkforceAttribute> attributes;

    public WorkforceProfession(String id, ConfigurationSection section) {
        this.id = id;
        this.paymentEnabled = section.getBoolean("payment.enabled");
        this.name = section.getString("name");
        this.group = section.getString("group").toUpperCase();
        this.icon = section.getString("icon.name");
        this.customModelData = section.getInt("icon.customModelData");
        this.paymentEquation = section.getString("payment.equation");
        this.description = section.getStringList("description").stream().map((line) -> Component.text(line)).collect(Collectors.toList());
        this.attributes = new ArrayList<WorkforceAttribute>();

        this.description.add(0, Component.text(this.group).color(NamedTextColor.GRAY));
        this.description.add(1, Component.text(""));

        final ConfigurationSection attributesSection = section.getConfigurationSection("attributes");
        if (attributesSection != null) {
            for (final String key : attributesSection.getKeys(false)) {
                final ConfigurationSection attrKeySection = attributesSection.getConfigurationSection(key);
                if (attrKeySection != null) {
                    this.attributes.add(new WorkforceAttribute(attrKeySection));
                }
            }
        }
    }

    public String getGroup() { return this.group; }

    public ItemStack getIconItem(final List<TextComponent> description) {
        Material iconMaterial = Material.getMaterial(this.icon);
        if (iconMaterial == null) {
            iconMaterial = Material.PLAYER_HEAD;
        }

        ItemStack icon = new ItemStack(iconMaterial, 1);
        ItemMeta meta = icon.getItemMeta();

        meta.setCustomModelData(this.customModelData);
        meta.lore(description);
        meta.displayName(Component.text(this.name, NamedTextColor.GOLD));
        meta.getPersistentDataContainer().set(
                new NamespacedKey(Bukkit.getPluginManager().getPlugin("SpecialisedWorkforce2"), "JobIdKey"),
                PersistentDataType.STRING,
                this.id
        );

        icon.setItemMeta(meta);
        return icon;
    }

    public boolean isPaymentEnabled() {
        return this.paymentEnabled && EconomyService.economyIntegrationEnabled;
    }

    public boolean hasAttributeOfType(WorkforceAttributeType type) {
        return this.attributes.stream().anyMatch((attribute) -> attribute.type == type);
    }

    public @Nullable List<WorkforceAttribute> getAttributesByType(WorkforceAttributeType type) {
        if (this.hasAttributeOfType(type)) {
            return this.attributes.stream().filter((attribute) -> attribute.type == type).collect(Collectors.toList());
        }

        return null;
    }

    public @Nullable List<WorkforceAttribute> getAttributesByType(WorkforceAttributeType type, int playerLevel) {
        if (this.hasAttributeOfType(type) && this.attributes.stream().anyMatch((attribute) -> playerLevel >= attribute.levelThreshold)) {
            return this.attributes.stream().filter((attribute) -> attribute.type == type && playerLevel >= attribute.levelThreshold).collect(Collectors.toList());
        }

        return null;
    }


}
