package io.github.cjustinn.specialisedworkforce2.services;

import io.github.cjustinn.specialisedworkforce2.enums.CustomInventoryType;
import io.github.cjustinn.specialisedworkforce2.interfaces.WorkforceInventoryGUI;
import io.github.cjustinn.specialisedworkforce2.models.*;
import io.github.cjustinn.specialisedworkforce2.models.GUI.WorkforceJoinInventoryGUI;
import io.github.cjustinn.specialisedworkforce2.models.GUI.WorkforceLeaderboardInventoryGUI;
import io.github.cjustinn.specialisedworkforce2.models.GUI.WorkforceStatusInventoryGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class CustomInventoryService {
    public static Map<String, WorkforceInventoryGUI> activeInventories = new HashMap<String, WorkforceInventoryGUI>();

    public static @Nullable Inventory BuildCustomInventory(CustomInventoryType type, String user, Map<String, Object> params) {
         Inventory customInventory = null;
         int requiredRows = 0;
         boolean multiPage = false;

         if (type == CustomInventoryType.JOIN) {
             // JOIN PROFESSION INVENTORY
             final @Nullable String filter = params.containsKey("group") ? (String) params.get("group") : null;
             List<WorkforceProfession> professions = WorkforceService.professions
                     .stream().filter((prof) -> (filter != null && prof.group.equalsIgnoreCase(filter)) || filter == null)
                     .collect(Collectors.toList());

             requiredRows = Math.max(1, (int) Math.ceil(professions.size() / 9.0));
             multiPage = requiredRows > 5;

             customInventory = Bukkit.createInventory(
                     (InventoryHolder) null,
                     Math.min(requiredRows, 6) * 9,
                     filter == null ? type.title : type.title.append(Component.text(String.format(" - %s", filter)))
             );
             PopulateJoinInventoryGUI(1, (int) Math.ceil(requiredRows / 5), multiPage, filter, user, customInventory);
         } else if (type == CustomInventoryType.STATUS) {
             // CURRENT PROFESSION STATUS INVENTORY
            requiredRows = Math.max(1, (int) Math.ceil(WorkforceService.GetActiveUserProfessions(user).size() / 9.0));
            multiPage = requiredRows > 5;

            customInventory = Bukkit.createInventory((InventoryHolder) null, Math.min(requiredRows, 6) * 9, type.title);
            PopulateStatusInventoryGUI(1, (int) Math.ceil(requiredRows / 5), multiPage, user, customInventory);
         } else if (type == CustomInventoryType.LEADERBOARD) {
             // GLOBAL LEADERBOARD INVENTORY
             final @Nullable String filter = params.containsKey("job") ? (String) params.get("job") : null;
             @Nullable WorkforceProfession filterProfession = null;
             if (filter != null) {
                 filterProfession = WorkforceService.GetProfessionById(filter);
             }

             requiredRows = Math.max(1, (int) Math.ceil(WorkforceService.GetActiveUserProfessions().stream().filter(
                     (prof) -> (filter != null && prof.jobId.equalsIgnoreCase(filter)) || filter == null
             ).collect(Collectors.toList()).size() / 9.0));
             multiPage = requiredRows > 5;

             customInventory = Bukkit.createInventory((InventoryHolder) null, Math.min(requiredRows, 6) * 9, filterProfession != null ? Component.text(String.format("%s - %s", type.title.content(), filterProfession.name)) : type.title);
             PopulateLeaderboardInventoryGUI(1, (int) Math.ceil(requiredRows / 5), multiPage, filter, customInventory);
         }

         if (customInventory != null) {
             if (activeInventories.containsKey(user)) activeInventories.remove(user);
             final int maxPages = (int) Math.ceil(requiredRows / 5);

             switch (type) {
                 case JOIN:
                     activeInventories.put(user, new WorkforceJoinInventoryGUI(customInventory, 1, maxPages));
                     break;
                 case STATUS:
                     activeInventories.put(user, new WorkforceStatusInventoryGUI(customInventory, 1, maxPages));
                     break;
                 case LEADERBOARD:
                     activeInventories.put(user, new WorkforceLeaderboardInventoryGUI(customInventory, 1, maxPages));
                     break;
             }
         }

         return customInventory;
    }

    public static @Nullable Inventory BuildCustomInventory(CustomInventoryType type, String user) {
        return BuildCustomInventory(type, user, new HashMap<String, Object>());
    }

    public static void PopulateLeaderboardInventoryGUI(final int page, final int maxPages, final boolean multiplePages, final @Nullable String filter, Inventory inventory) {
        inventory.clear();

        if (multiplePages) {
            // Populate the lowest row with navigation controls.
            AddPaginationControls(inventory, page, maxPages);
        }

        // Get the user objects for the provided page number.
        List<WorkforceLeaderboardUser> users = GetLeaderboardUsersForPage(page, filter, multiplePages);

        // Add the user item-icons into the inventory.
        for (int i = 0; i < users.size(); i++) {
            inventory.setItem(i, users.get(i).getLeaderboardItem());
        }
    }

    public static void PopulateStatusInventoryGUI(final int page, final int maxPages, final boolean multiplePages, final String user, Inventory inventory) {
        inventory.clear();

        if (multiplePages) {
            // Populate the lowest row with navigation controls.
            AddPaginationControls(inventory, page, maxPages);
        }

        // Get the professions for the provided page number.
        List<WorkforceUserProfession> professions = GetUserProfessionsForPage(user, page, multiplePages);

        // Add the profession item-icons into the inventory.
        for (int i = 0; i < professions.size(); i++) {
            final WorkforceProfession profession = professions.get(i).getProfession();

            List<TextComponent> description = new ArrayList<TextComponent>() {{
                addAll(profession.description);
            }};

            description.add(Component.text(""));
            description.add(
                    Component.text("Level ", NamedTextColor.GRAY).append(
                            Component.text(String.format("%d", professions.get(i).getLevel())).color(NamedTextColor.GOLD)
                    )
            );

            if (professions.get(i).isMaximumLevel()) {
                description.add(Component.text("Maximum Level", NamedTextColor.GOLD));
            } else {
                description.add(
                        Component.text(String.format("%d", professions.get(i).getExperience()), NamedTextColor.GOLD).append(
                                Component.text(String.format(" / %d", professions.get(i).getRequiredExperience()), NamedTextColor.GRAY)
                        )
                );
            }

            description.add(Component.text(""));
            description.add(Component.text("Right-click to quit this profession.", NamedTextColor.RED));

            inventory.setItem(i, profession.getIconItem(description));
        }
    }

    public static void PopulateJoinInventoryGUI(final int page, final int maxPages, final boolean multiplePages, final @Nullable String filter, final String user, Inventory inventory) {
        inventory.clear();

        if (multiplePages) {
            // Populate the lowest row with navigation controls.
            AddPaginationControls(inventory, page, maxPages);
        }

        // Get the professions for the provided page number.
        List<WorkforceProfession> professions = GetProfessionsForPage(page, filter, multiplePages);

        // Add the profession item-icons into the inventory.
        for (int i = 0; i < professions.size(); i++) {
            final WorkforceProfession profession = professions.get(i);
            final boolean playerHasProfession = WorkforceService.UserHasActiveProfession(user, profession.id);

            TextComponent prompt;
            if (playerHasProfession)
                prompt = Component.text("You already have this profession.").color(NamedTextColor.RED);
            else if (!playerHasProfession && WorkforceService.GetActiveProfessionsInGroup(user, profession.group) >= WorkforceService.GetMaxJobsForGroup(profession.group))
                prompt = Component.text(String.format("You cannot join another %s profession.", profession.group)).color(NamedTextColor.RED);
            else
                prompt = Component.text("Click to join this profession.").color(NamedTextColor.GREEN);

            List<TextComponent> description = new ArrayList<TextComponent>() {{
                addAll(profession.description);
            }};

            description.add(Component.text(""));
            description.add(prompt);

            inventory.setItem(i, profession.getIconItem(description));
        }
    }

    public static void AddPaginationControls(Inventory inventory, final int page, final int maxPages) {
        for (int i = 1; i <= 9; i++) {
            if (i == 9 && page < maxPages) {
                // Show "next page" item.
                inventory.setItem(GetSlotIndex(6, i), GetNextPageButton(page, maxPages));
            } else if (i == 1 && page > 1) {
                // Show "prev page" item.
                inventory.setItem(GetSlotIndex(6, i), GetPrevPageButton(page, maxPages));
            } else {
                // Show slot filler.
                inventory.setItem(GetSlotIndex(6, i), GetSlotFiller());
            }
        }
    }

    public static int GetSlotIndex(final int row, final int slot) {
        return (row - 1) * 9 + (slot - 1);
    }

    public static List<WorkforceLeaderboardUser> GetLeaderboardUsersForPage(final int page, final @Nullable String jobId, boolean multiPage) {
        List<WorkforceLeaderboardUser> users = WorkforceService.GetActiveUserProfessions().stream()
                .filter((userProfession) -> {
                    if ((jobId != null && userProfession.jobId.equalsIgnoreCase(jobId)) || jobId == null) {
                        return true;
                    } else {
                        return false;
                    }
                })
                .map((userProfession) -> userProfession.uuid)
                .distinct()
                .map((uuid) -> {
                    return new WorkforceLeaderboardUser(uuid, WorkforceService.GetActiveUserProfessions(uuid));
                }).collect(Collectors.toList());

        Collections.sort(users, Comparator.comparing(WorkforceLeaderboardUser::getUuid));

        if (multiPage) {
            final int perPage = GetSlotIndex(5, 9) + 1;
            final int startIndex = (page - 1) * perPage;
            final int endIndex = Math.min(page * perPage, users.size());

            if (startIndex >= WorkforceService.professions.size()) {
                return Arrays.stream(new WorkforceLeaderboardUser[] {}).collect(Collectors.toList());
            }

            return users.subList(startIndex, endIndex);
        } else {
            return users;
        }

    }

    public static List<WorkforceProfession> GetProfessionsForPage(final int page, final @Nullable String group, boolean multiPage) {
        List<WorkforceProfession> professions = WorkforceService.professions.stream()
                .filter((prof) -> (group != null && prof.group.equalsIgnoreCase(group)) || group == null)
                .collect(Collectors.toList());

        Collections.sort(professions, Comparator.comparing(WorkforceProfession::getGroup));

        if (!multiPage)
            return professions;
        else {
            final int perPage = GetSlotIndex(5, 9) + 1;
            final int startIndex = (page - 1) * perPage;
            final int endIndex = Math.min(page * perPage, professions.size());

            if (startIndex >= professions.size()) {
                return Arrays.stream(new WorkforceProfession[] {}).collect(Collectors.toList());
            }

            return professions.subList(startIndex, endIndex);
        }
    }

    public static List<WorkforceUserProfession> GetUserProfessionsForPage(final String user, final int page, boolean multiPage) {
        List<WorkforceUserProfession> userProfessions = WorkforceService.GetActiveUserProfessions(user);
        Collections.sort(userProfessions, Comparator.comparing(WorkforceUserProfession::getGroup));

        if (!multiPage) {
            return userProfessions;
        } else {
            final int perPage = GetSlotIndex(5, 9) + 1;
            final int startIndex = (page - 1) * perPage;
            final int endIndex = Math.min(page * perPage, userProfessions.size());

            if (startIndex >= userProfessions.size()) {
                return Arrays.stream(new WorkforceUserProfession[] {}).collect(Collectors.toList());
            }

            return userProfessions.subList(startIndex, endIndex);
        }
    }

    public static ItemStack GetNextPageButton(final int page, final int maxPages) {
        ItemStack item = new ItemStack(Material.LIME_STAINED_GLASS_PANE, 1);

        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Next Page", NamedTextColor.GOLD));
        meta.lore(Arrays.stream(new TextComponent[]{
                Component.text(
                        String.format("%d / %d", page, maxPages),
                        NamedTextColor.GRAY
                )
        }).collect(Collectors.toList()));

        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack GetPrevPageButton(final int page, final int maxPages) {

        ItemStack item = new ItemStack(Material.RED_STAINED_GLASS_PANE, 1);

        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Previous Page", NamedTextColor.GOLD));
        meta.lore(Arrays.stream(new TextComponent[]{
                Component.text(
                        String.format("%d / %d", page, maxPages),
                        NamedTextColor.GRAY
                )
        }).collect(Collectors.toList()));

        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack GetSlotFiller() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1);

        ItemMeta meta = filler.getItemMeta();
        meta.displayName(Component.text("Unused Slot", NamedTextColor.DARK_GRAY));

        filler.setItemMeta(meta);
        return filler;
    }
}
