package io.github.cjustinn.specialisedworkforce2.models;

import io.github.cjustinn.specialisedworkforce2.models.SQL.MySQLProperty;
import io.github.cjustinn.specialisedworkforce2.services.EvaluationService;
import io.github.cjustinn.specialisedworkforce2.services.LoggingService;
import io.github.cjustinn.specialisedworkforce2.services.SQLService;
import io.github.cjustinn.specialisedworkforce2.services.WorkforceService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class WorkforceUserProfession {
    public final String uuid;
    private int level;
    private int experience;
    private boolean active;

    public final String jobId;

    public WorkforceUserProfession(String uuid, int level, int experience, String id, int active) {
        this.uuid = uuid;
        this.level = level;
        this.experience = experience;
        this.jobId = id;
        this.active = active == 1;
    }

    public String getGroup() { return this.getProfession().group; }
    public boolean isActive() { return this.active; }
    public boolean isMaximumLevel() { return this.level >= WorkforceService.maximumLevel; }
    public boolean employsPlayer(String uuid) {
        return this.uuid.equalsIgnoreCase(uuid);
    }

    public @Nullable WorkforceProfession getProfession() {
        return WorkforceService.GetProfessionById(this.jobId);
    }

    public int getLevel() { return this.level; }
    public int getExperience() { return this.experience; }
    public int getRequiredExperience() {
        return (int) Math.ceil(EvaluationService.evaluate(WorkforceService.requiredExperienceEquation.replace("{level}", String.valueOf(this.level))));
    }

    public void quitProfession() {
        this.level = (int) Math.floor(this.level * (1.0 - WorkforceService.jobQuitLossRate));
        this.experience = 0;
        this.active = false;

        if (this.level <= 0) {
            this.level = 1;
        }

        this.update();
    }


    public void addExperience(int amount) {
        if (!this.isMaximumLevel()) {
            this.experience += amount;

            while (this.experience >= this.getRequiredExperience() && !this.isMaximumLevel()) {
                this.experience -= this.getRequiredExperience();
                this.addLevel(1);
            }

            if (this.isMaximumLevel()) {
                this.experience = 0;
            }

            this.update();
        }
    }

    public void setExperience(int amount) {
        this.experience = amount;
        this.update();
    }

    public void addLevel(int amount) {
        this.level += amount;

        if (this.level > WorkforceService.maximumLevel) {
            this.level = WorkforceService.maximumLevel;
        }

        Player user = this.getUser();
        if (user != null) {
            user.sendActionBar(
                    Component.text(
                            String.format("You are now a level %d %s.", this.level, this.getProfession().name),
                            NamedTextColor.GREEN
                    )
            );

            user.sendMessage(
                    Component.text("You are now a level ")
                            .append(Component.text(String.format("%d %s", this.level, this.getProfession().name), NamedTextColor.GOLD))
                            .append(Component.text("."))
            );

            // Check for new attribute unlocks for this level.
            List<WorkforceAttribute> unlockedAttributes = this.getProfession().attributes.stream().filter((attr) -> attr.levelThreshold == this.level).collect(Collectors.toList());
            if (unlockedAttributes.size() > 0) {
                user.sendMessage(
                        Component.text("New profession attributes have been unlocked!", NamedTextColor.GREEN)
                );

                for (final WorkforceAttribute unlock : unlockedAttributes) {
                    if (unlock.title != null) {
                        user.sendMessage(
                                Component.text(unlock.title).color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true)
                        );
                    }

                    if (unlock.unlockMessage != null) {
                        user.sendMessage(Component.text(unlock.unlockMessage));
                    }
                }
            }
        }

        if (level == WorkforceService.maximumLevel) {
            Bukkit.broadcast(this.getMaxLevelMessage());
        }

        this.update();
    }

    public void setLevel(int amount) {
        this.level = amount > WorkforceService.maximumLevel ? WorkforceService.maximumLevel : amount;

        if (this.level == WorkforceService.maximumLevel) {
            Bukkit.broadcast(this.getMaxLevelMessage());
        }

        this.update();
    }

    public void setActive(boolean state) {
        this.active = state;
    }

    private @Nullable Player getUser() {
        final Player user = Bukkit.getPlayer(UUID.fromString(this.uuid));
        if (user != null && user.isOnline()) {
            return user;
        } else {
            return null;
        }
    }

    private TextComponent getMaxLevelMessage() {
        final Player user = this.getUser();
        if (user != null) {
            return Component.text(user.getName(), NamedTextColor.GOLD)
                    .append(Component.text(" is now a level ", NamedTextColor.WHITE))
                    .append(Component.text(String.format("%d %s", this.level, this.getProfession().name), NamedTextColor.GOLD))
                    .append(Component.text("!", NamedTextColor.WHITE));
        } else {
            return Component.text("Somebody is now a level ", NamedTextColor.WHITE)
                    .append(Component.text(String.format("%d", this.level), NamedTextColor.GOLD))
                    .append(Component.text(String.format("%s!", this.getProfession().name), NamedTextColor.WHITE));
        }
    }

    public void update() {
        final String query = "UPDATE workforce_employment SET jobLevel = ?, jobExperience = ?, active = ? WHERE uuid = ? AND jobId = ?";
        final boolean success = SQLService.RunUpdate(query, new MySQLProperty[]{
                new MySQLProperty("integer", this.level, 1),
                new MySQLProperty("integer", this.experience, 2),
                new MySQLProperty("integer", this.active ? 1 : 0, 3),
                new MySQLProperty("string", this.uuid, 4),
                new MySQLProperty("string", this.jobId, 5),
        });

        if (!success) {
            LoggingService.WriteError("Unable to save changes to user record in database!");
        }
    }
}
