package io.github.cjustinn.specialisedworkforce2.listeners;

import io.github.cjustinn.specialisedworkforce2.enums.CustomInventoryType;
import io.github.cjustinn.specialisedworkforce2.models.WorkforceProfession;
import io.github.cjustinn.specialisedworkforce2.services.CustomInventoryService;
import io.github.cjustinn.specialisedworkforce2.services.WorkforceService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.UUID;

public class CustomInventoryListener implements Listener {
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        final String userId = ((Player) event.getWhoClicked()).getUniqueId().toString();
        InventoryView inventoryView = event.getView();

        if (inventoryView.title() == CustomInventoryType.JOIN.title && event.getCurrentItem() != null) {
            ItemStack item = event.getCurrentItem();
            ItemMeta meta = item.getItemMeta();

            if (meta.getPersistentDataContainer().has(new NamespacedKey(Bukkit.getPluginManager().getPlugin("SpecialisedWorkforce2"), "JobIdKey"))) {
                final String targetJob = meta.getPersistentDataContainer().get(
                        new NamespacedKey(Bukkit.getPluginManager().getPlugin("SpecialisedWorkforce2"), "JobIdKey"),
                        PersistentDataType.STRING
                );

                if (!WorkforceService.UserHasActiveProfession(userId, targetJob)) {
                    WorkforceProfession targetProfession = WorkforceService.GetProfessionById(targetJob);
                    if (targetProfession != null) {
                        final int userProfessionsInGroup = WorkforceService.GetActiveProfessionsInGroup(userId, targetProfession.group);

                        if (userProfessionsInGroup < WorkforceService.GetMaxJobsForGroup(targetProfession.group)) {
                            WorkforceService.AddProfessionToUser(userId, targetJob);

                            inventoryView.close();
                            CustomInventoryService.activeInventories.remove(userId);
                        }
                    }
                }
            }
        } else if (inventoryView.title() == CustomInventoryType.STATUS.title && event.getCurrentItem() != null && event.getClick() == ClickType.RIGHT) {
            ItemStack item = event.getCurrentItem();
            ItemMeta meta = item.getItemMeta();

            if (meta.getPersistentDataContainer().has(new NamespacedKey(Bukkit.getPluginManager().getPlugin("SpecialisedWorkforce2"), "JobIdKey"))) {
                final String targetJob = meta.getPersistentDataContainer().get(
                        new NamespacedKey(Bukkit.getPluginManager().getPlugin("SpecialisedWorkforce2"), "JobIdKey"),
                        PersistentDataType.STRING
                );

                if (WorkforceService.UserHasActiveProfession(userId, targetJob)) {
                    final int index = WorkforceService.GetIndexOfUserProfession(userId, targetJob);
                    WorkforceService.userProfessions.get(index).quitProfession();

                    inventoryView.close();
                    CustomInventoryService.activeInventories.remove(userId);

                    Bukkit.getPlayer(UUID.fromString(userId)).sendMessage(Component.text(String.format("You are no longer a %s.", WorkforceService.userProfessions.get(index).getProfession().name), NamedTextColor.GREEN));
                }
            }
        }

        if (Arrays.stream(CustomInventoryType.values()).anyMatch((type) -> inventoryView.title() == type.title)) {
            event.setCancelled(true);
        }
    }
}
