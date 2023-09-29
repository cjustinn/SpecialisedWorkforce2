package io.github.cjustinn.specialisedworkforce2.models;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WorkforceLeaderboardUser {
    public final String uuid;
    public final List<WorkforceUserProfession> professions;

    public WorkforceLeaderboardUser(String uuid, List<WorkforceUserProfession> professions) {
        this.uuid = uuid;
        this.professions = professions;
    }

    public String getUuid() { return this.uuid; }

    public ItemStack getLeaderboardItem() {
        OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(this.uuid));

        ItemStack item = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        meta.setOwningPlayer(player);
        meta.displayName(Component.text(player.getName(), NamedTextColor.GOLD));

        List<TextComponent> description = new ArrayList<TextComponent>() {{ add(Component.text("")); }};
        for (int i = 0; i < this.professions.size(); i++) {
            WorkforceProfession prof = this.professions.get(i).getProfession();
            description.add(
                    Component.text("Level ", NamedTextColor.GRAY)
                            .append(Component.text(this.professions.get(i).getLevel(), NamedTextColor.GOLD))
                            .append(Component.text(String.format(" %s", prof.name), NamedTextColor.GRAY))
            );

            if (i != this.professions.size() - 1) {
                description.add(Component.text(""));
            }
        }

        meta.lore(description);
        item.setItemMeta(meta);

        return item;
    }
}
