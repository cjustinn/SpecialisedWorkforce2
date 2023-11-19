package io.github.cjustinn.specialisedworkforce2.models.GUI;

import io.github.cjustinn.specialisedworkforce2.enums.CustomInventoryType;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CustomInventoryMenuHolder implements InventoryHolder {
    private Inventory inventory;
    private CustomInventoryType type;

    public CustomInventoryMenuHolder(final int inventorySize, final CustomInventoryType inventoryType, final @Nullable String subtitle) {
        this.inventory = Bukkit.createInventory(this, inventorySize, subtitle != null ? inventoryType.title.append(Component.text(String.format(" - %s", subtitle))) : inventoryType.title);
        this.type = inventoryType;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return this.inventory;
    }

    public CustomInventoryType getType() {
        return this.type;
    }
}
