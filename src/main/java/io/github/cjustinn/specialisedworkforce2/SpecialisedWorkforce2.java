package io.github.cjustinn.specialisedworkforce2;

import io.github.cjustinn.specialisedworkforce2.commands.handlers.WorkforceAdminCommandExecutor;
import io.github.cjustinn.specialisedworkforce2.commands.handlers.WorkforceCommandExecutor;
import io.github.cjustinn.specialisedworkforce2.commands.tabcompleters.WorkforceAdminTabCompleter;
import io.github.cjustinn.specialisedworkforce2.commands.tabcompleters.WorkforceCommandTabCompleter;
import io.github.cjustinn.specialisedworkforce2.listeners.CustomInventoryListener;
import io.github.cjustinn.specialisedworkforce2.models.SQL.MySQLCredentials;
import io.github.cjustinn.specialisedworkforce2.models.WorkforceProfession;
import io.github.cjustinn.specialisedworkforce2.models.WorkforceUserProfession;
import io.github.cjustinn.specialisedworkforce2.services.EconomyService;
import io.github.cjustinn.specialisedworkforce2.services.LoggingService;
import io.github.cjustinn.specialisedworkforce2.services.SQLService;
import io.github.cjustinn.specialisedworkforce2.services.WorkforceService;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public final class SpecialisedWorkforce2 extends JavaPlugin {
    // Take a very Angular-esque approach - any functions used in multiple classes should be split out into separate "service" classes, which can be
    // invoked from anywhere they're needed. Services should be created around similar, related functions (economy, job, etc.).
    private FileConfiguration pluginConfig = null;
    private FileConfiguration jobConfig = null;

    @Override
    public void onEnable() {
        if (!this.initialisePlugin()) {
            LoggingService.WriteError("Plugin initialisation failed. SpecialisedWorkforce 2 is now disabled.");
            Bukkit.getPluginManager().disablePlugin(this);
        } else {
            LoggingService.WriteMessage("SpecialisedWorkforce 2 is running!");
        }
    }

    @Override
    public void onDisable() {
        if (!SQLService.CloseConnection()) {
            LoggingService.WriteError(String.format("Failed to disconnect from the %s database.", SQLService.useMySQL ? "MySQL" : "SQLite"));
        }
    }

    private boolean initialisePlugin() {
        List<Boolean> statuses = new ArrayList<Boolean>();

        statuses.add(this.loadConfiguration());
        statuses.add(this.loadUserData());
        statuses.add(this.registerListeners());

        return statuses.stream().reduce(true, (acc, curr) -> acc && curr);
    }

    private boolean loadConfiguration() {
        LoggingService.WriteMessage("Loading configuration files...");

        // Status Flag
        boolean success = true;

        // Fetch the plugin folder.
        File pluginDirectory = getDataFolder();
        if (!pluginDirectory.exists())
            pluginDirectory.mkdir();

        // (1) Load main config file ('plugins/SpecialisedWorkforce2/config.yml').
        File mainFile = new File(pluginDirectory, "config.yml");
        try {
            mainFile.createNewFile();
        } catch (IOException err) {
            LoggingService.WriteError("Could not create main plugin config.");
            success = false;
        }

        if (mainFile.exists()) {
            this.pluginConfig = YamlConfiguration.loadConfiguration(mainFile);

            Map<String, Object> defaults = new HashMap<String, Object>();
            defaults.put("mysql.enabled", false);
            defaults.put("mysql.host", "localhost");
            defaults.put("mysql.port", "3306");
            defaults.put("mysql.user", "");
            defaults.put("mysql.pass", "");
            defaults.put("mysql.database", "");
            defaults.put("maxJobs.primary", 1);
            defaults.put("maxLevel", 50);
            defaults.put("experienceEarningEquation", "5 * (1 + ({level} * 0.5))");
            defaults.put("experienceRequirementEquation", "100 * (1 + ({level} * 5))");
            defaults.put("quitLossRate", 0.2);
            defaults.put("useEconomy", true);

            this.pluginConfig.addDefaults(defaults);
            this.pluginConfig.options().copyDefaults(true);
            try {
                this.pluginConfig.save(mainFile);
            } catch (IOException err) {
                LoggingService.WriteError("Could not save main plugin config. Settings may not be saved.");
            }
        }

        // (2) Load job config file ('plugins/SpecialisedWorkforce2/jobs.yml').
        File jobFile = new File(pluginDirectory, "jobs.yml");
        try {
            jobFile.createNewFile();
        } catch (IOException err) {
            LoggingService.WriteError("Could not create jobs file.");
            success = false;
        }

        if (jobFile.exists()) {
            this.jobConfig = YamlConfiguration.loadConfiguration(jobFile);

            Map<String, Object> defaults = new HashMap<String, Object>();
            defaults.put("farmer.name", "Farmer");
            defaults.put("farmer.group", "primary");
            defaults.put("farmer.description", new String[]{
                    "Receives bonuses to crop yields",
                    "and a chance to negate damage to",
                    "their hoes."
            });
            defaults.put("farmer.icon.name", "WHEAT");
            defaults.put("farmer.icon.customModelData", 1000);
            defaults.put("farmer.payment.enabled", true);
            defaults.put("farmer.payment.equation", "0.25 * (1 + ({level} * 0.38))");
            defaults.put("farmer.attributes.cropbreaker.type", "BONUS_BLOCK_DROPS");
            defaults.put("farmer.attributes.cropbreaker.amount", "1 * (1 + ({level} * 0.06))");
            defaults.put("farmer.attributes.cropbreaker.chance", "0.1 + ({level} * 0.014)");
            defaults.put("farmer.attributes.cropbreaker.levelThreshold", 0);
            defaults.put("farmer.attributes.cropbreaker.targets", new String[]{
                    "CARROT",
                    "POTATO",
                    "BEETROOT",
                    "MELON",
                    "PUMPKIN",
                    "WHEAT"
            });
            defaults.put("farmer.attributes.toolsave.type", "DURABILITY_SAVE");
            defaults.put("farmer.attributes.toolsave.chance", "0.1 + ({level} * 0.013)");
            defaults.put("farmer.attributes.toolsave.levelThreshold", 10);
            defaults.put("farmer.attributes.toolsave.targets", new String[]{
                    "{*}_HOE"
            });


            this.jobConfig.addDefaults(defaults);
            this.jobConfig.options().copyDefaults(true);
            try {
                this.jobConfig.save(jobFile);
            } catch (IOException err) {
                LoggingService.WriteError("Could not save plugin jobs config. Jobs may not be saved.");
            }
        }

        boolean loadedSettings = false, loadedJobs = false;
        if (success) {
            loadedSettings = this.loadSettings();
            loadedJobs = this.loadJobs();
        }

        return success && loadedSettings && loadedJobs;
    }

    private boolean loadSettings() {
        LoggingService.WriteMessage("Fetching plugin settings...");
        if (this.pluginConfig == null) {
            LoggingService.WriteError("Main plugin config file does not exist!");
            return false;
        }

        // Fetch SQL-related settings.
        SQLService.useMySQL = this.pluginConfig.getBoolean("mysql.enabled", false);
        if (SQLService.useMySQL) {
            SQLService.mysqlCredentials = new MySQLCredentials(
                    this.pluginConfig.getString("mysql.host", "localhost"),
                    this.pluginConfig.getString("mysql.port", "3306"),
                    this.pluginConfig.getString("mysql.database", ""),
                    this.pluginConfig.getString("mysql.user", ""),
                    this.pluginConfig.getString("mysql.pass", "")
            );
        }

        // Fetch economy-related settings.
        final boolean vaultInstalled = this.getServer().getPluginManager().getPlugin("Vault") != null;
        final boolean economyEnabledInConfig = this.pluginConfig.getBoolean("useEconomy", true);
        EconomyService.economyIntegrationEnabled = economyEnabledInConfig && vaultInstalled;

        if (!vaultInstalled && economyEnabledInConfig) {
            LoggingService.WriteWarning("You have economy integration enabled but Vault is not installed! Economy integration has been disabled.");
        }

        // Fetch job-related settings.
        WorkforceService.maximumLevel = this.pluginConfig.getInt("maxLevel", 50);
        WorkforceService.earnedExperienceEquation = this.pluginConfig.getString("experienceEarningEquation");
        WorkforceService.requiredExperienceEquation = this.pluginConfig.getString("experienceRequirementEquation");
        WorkforceService.jobQuitLossRate = this.pluginConfig.getDouble("quitLossRate");

        ConfigurationSection maxJobSection = this.pluginConfig.getConfigurationSection("maxJobs");
        if (maxJobSection != null) {
            for (String key : maxJobSection.getKeys(false)) {
                WorkforceService.maximumJobs.put(key.toUpperCase(), this.pluginConfig.getInt("maxJobs." + key, 1));
            }
        } else {
            LoggingService.WriteWarning("You have no maximum jobs set for any groups. The default is one per group.");
        }

        return true;
    }

    private boolean loadJobs() {
        LoggingService.WriteMessage("Fetching jobs...");
        if (this.jobConfig == null) {
            LoggingService.WriteError("Plugin jobs config file does not exist!");
            return false;
        }

        for (final String jobId : this.jobConfig.getKeys(false)) {
            final ConfigurationSection jobSection = this.jobConfig.getConfigurationSection(jobId);
            if (jobSection != null) {
                final WorkforceProfession job = new WorkforceProfession(jobId, jobSection);
                WorkforceService.professions.add(job);

                LoggingService.WriteMessage(String.format("Loaded %s with %d attributes...", job.name, job.attributes.size()));
            }
        }

        LoggingService.WriteMessage(String.format("Loaded %d professions.", WorkforceService.professions.size()));

        return true;
    }

    private boolean loadUserData() {
        final boolean databaseInitialised = this.initialiseDatabase();
        boolean userDataFetched = false;

        if (databaseInitialised) {
            userDataFetched = this.fetchUserData();
        } else {
            LoggingService.WriteError(
                    String.format(
                            "Unable to connect to the %s database and create necessary tables.",
                            SQLService.useMySQL ? "MySQL" : "SQLite"
                    )
            );
        }

        return databaseInitialised && userDataFetched;
    }

    private boolean initialiseDatabase() {
        // Create the data folder directory.
        File pluginFolder = getDataFolder();
        File dataFolder = new File(pluginFolder, "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdir();
        }

        final boolean connectionEstablished = SQLService.CreateConnection(new File(dataFolder, "database.db").getPath());
        boolean tablesCreated = false;

        if (connectionEstablished) {
            try {
                final PreparedStatement userTableQuery = SQLService.connection.prepareStatement("CREATE TABLE IF NOT EXISTS workforce_employment(uuid VARCHAR(36) NOT NULL,jobId VARCHAR(40) NOT NULL,jobLevel INT DEFAULT 1,jobExperience INT DEFAULT 0,active TINYINT DEFAULT 1,PRIMARY KEY(uuid, jobId));");
                userTableQuery.executeUpdate();
                userTableQuery.close();

                tablesCreated = true;
            } catch (SQLException err) {
                tablesCreated = false;
            }
        }

        return connectionEstablished && tablesCreated;
    }

    private boolean fetchUserData() {
        ResultSet userDataResults = SQLService.RunQuery("SELECT uuid, jobId as id, jobLevel as level, jobExperience as experience, active FROM workforce_employment;");
        if (userDataResults != null) {
            try {
                while (userDataResults.next()) {
                    final String uuid = userDataResults.getString(1);
                    final String jobId = userDataResults.getString(2);
                    final int level = userDataResults.getInt(3);
                    final int experience = userDataResults.getInt(4);
                    final int active = userDataResults.getInt(5);

                    WorkforceService.userProfessions.add(new WorkforceUserProfession(uuid, level, experience, jobId, active));
                }

                LoggingService.WriteMessage(String.format("Loaded %d users.", WorkforceService.userProfessions.size()));

                return true;
            } catch (SQLException err) {
                return false;
            }
        } else {
            LoggingService.WriteError("Could not fetch user data!");
            return false;
        }
    }

    private boolean registerListeners() {
        // Command Executors
        getCommand("workforce").setExecutor(new WorkforceCommandExecutor());
        getCommand("workforceadmin").setExecutor(new WorkforceAdminCommandExecutor());

        // Command Tab Completers
        getCommand("workforce").setTabCompleter(new WorkforceCommandTabCompleter());
        getCommand("workforceadmin").setTabCompleter(new WorkforceAdminTabCompleter());

        // Listeners
        getServer().getPluginManager().registerEvents(new CustomInventoryListener(), this);

        return true;
    }
}
