package io.github.cjustinn.specialisedworkforce2;

import io.github.cjustinn.specialisedlib.Database.DatabaseCredentials;
import io.github.cjustinn.specialisedlib.Database.DatabaseService;
import io.github.cjustinn.specialisedlib.Economy.SpecialisedEconomy;
import io.github.cjustinn.specialisedlib.Logging.LoggingService;
import io.github.cjustinn.specialisedworkforce2.commands.handlers.WorkforceAdminCommandExecutor;
import io.github.cjustinn.specialisedworkforce2.commands.handlers.WorkforceCommandExecutor;
import io.github.cjustinn.specialisedworkforce2.commands.tabcompleters.WorkforceAdminTabCompleter;
import io.github.cjustinn.specialisedworkforce2.commands.tabcompleters.WorkforceCommandTabCompleter;
import io.github.cjustinn.specialisedworkforce2.enums.AttributeLogType;
import io.github.cjustinn.specialisedworkforce2.enums.DatabaseQuery;
import io.github.cjustinn.specialisedworkforce2.listeners.CustomInventoryListener;
import io.github.cjustinn.specialisedworkforce2.listeners.WorkforceJoinLeaveListener;
import io.github.cjustinn.specialisedworkforce2.listeners.attributes.*;
import io.github.cjustinn.specialisedworkforce2.models.WorkforceInteractionLogValue;
import io.github.cjustinn.specialisedworkforce2.models.WorkforceProfession;
import io.github.cjustinn.specialisedworkforce2.models.WorkforceRewardBacklogItem;
import io.github.cjustinn.specialisedworkforce2.models.WorkforceUserProfession;
import io.github.cjustinn.specialisedworkforce2.services.*;
import jdk.jshell.JShell;
import jdk.jshell.execution.LocalExecutionControlProvider;
import net.coreprotect.CoreProtect;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

public final class SpecialisedWorkforce2 extends JavaPlugin {
    public static SpecialisedWorkforce2 plugin;

    @Override
    public void onEnable() {
        SpecialisedWorkforce2.plugin = this;

        if (!this.initialisePlugin()) {
            LoggingService.writeLog(Level.SEVERE, "Plugin initialisation failed. SpecialisedWorkforce 2 is now disabled.");
            Bukkit.getPluginManager().disablePlugin(this);
        } else {
            EvaluationService.context = JShell.builder().executionEngine(new LocalExecutionControlProvider(), new HashMap<>()).build();

            LoggingService.writeLog(Level.INFO,"SpecialisedWorkforce 2 is running!");
        }
    }

    @Override
    public void onDisable() {
        if (!DatabaseService.CloseConnection()) {
            LoggingService.writeLog(Level.SEVERE, String.format("Failed to disconnect from the %s database.", DatabaseService.enableMySQL ? "MySQL" : "SQLite"));
        }
    }

    private boolean initialisePlugin() {
        List<Boolean> statuses = new ArrayList<Boolean>();

        statuses.add(this.initialiseCoreProtect());
        statuses.add(this.loadSettings());
        statuses.add(this.loadJobs());
        statuses.add(this.loadUserData());
        statuses.add(this.registerListeners());

        return statuses.stream().reduce(true, (acc, curr) -> acc && curr);
    }

    private boolean loadSettings() {
        LoggingService.writeLog(Level.INFO, "Fetching plugin settings...");
        saveDefaultConfig();

        // Fetch SQL-related settings.
        DatabaseService.enableMySQL = getConfig().getBoolean("mysql.enabled", false);
        if (DatabaseService.enableMySQL) {
            DatabaseService.credentials = new DatabaseCredentials(getConfig().getConfigurationSection("mysql"));
        }

        // Fetch economy-related settings.
        EconomyService.economyIntegrationEnabled = this.integrateEconomy();
        EconomyService.fallbackCurrencySymbol = getConfig().getString("general.fallbackCurrencySymbol", "$");

        // Fetch job-related settings.
        WorkforceService.maximumLevel = getConfig().getInt("limits.maxLevel", 50);

        WorkforceService.earnedExperienceEquation = getConfig().getString("equations.experienceEarning", "5 * (1 + ({level} * 0.5))");
        WorkforceService.requiredExperienceEquation = getConfig().getString("equations.experienceRequired", "100 * (1 + ({level} * 5))");

        WorkforceService.jobQuitLossRate = getConfig().getDouble("general.quitLossRate", 0.2);
        WorkforceService.maxDescriptionLength = getConfig().getInt("limits.maxProfessionDescriptionLength");

        ConfigurationSection maxJobSection = getConfig().getConfigurationSection("limits.professionGroupLimits");
        if (maxJobSection != null) {
            for (String key : maxJobSection.getKeys(false)) {
                WorkforceService.maximumJobs.put(key.toUpperCase(), getConfig().getInt("limits.professionGroupLimits." + key, 1));
            }
        } else {
            LoggingService.writeLog(Level.WARNING, "You have no maximum jobs set for any groups. The default is one per group.");
        }

        return true;
    }

    private boolean integrateEconomy() {
        final boolean vaultInstalled = this.getServer().getPluginManager().getPlugin("Vault") != null;
        final boolean economyEnabledInConfig = getConfig().getBoolean("general.useEconomy", true);

        if (!vaultInstalled && economyEnabledInConfig) {
            LoggingService.writeLog(Level.WARNING, "You have economy integration enabled but Vault is not installed! Economy integration has been disabled.");
        } else if (vaultInstalled && economyEnabledInConfig) {
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                EconomyService.enableSpecialisedEconomicsFeatures = rsp.getProvider() instanceof SpecialisedEconomy;
                EconomyService.SetEconomy(rsp.getProvider());

                return true;
            } else {
                LoggingService.writeLog(Level.WARNING, "Failed to get Vault economy provider; economy integration is disabled.");
            }
        }

        return false;
    }

    private boolean loadJobs() {
        LoggingService.writeLog(Level.INFO, "Fetching jobs...");
        ConfigurationSection professionsSection = getConfig().getConfigurationSection("professions");

        if (professionsSection == null) {
            LoggingService.writeLog(Level.SEVERE, "Professions config not found!");
            return false;
        }

        for (final String jobId : professionsSection.getKeys(false)) {
            final ConfigurationSection jobSection = professionsSection.getConfigurationSection(jobId);
            if (jobSection != null) {
                final WorkforceProfession job = new WorkforceProfession(jobId, jobSection);
                WorkforceService.professions.add(job);

                LoggingService.writeLog(Level.INFO, String.format("Loaded %s with %d attributes...", job.name, job.attributes.size()));
            }
        }

        LoggingService.writeLog(Level.INFO, String.format("Loaded %d professions.", WorkforceService.professions.size()));

        return true;
    }

    private boolean loadUserData() {
        final boolean databaseInitialised = this.initialiseDatabase();
        boolean userDataFetched = false,  loggingDataFetched = false, backlogDataFetched = false;

        if (databaseInitialised) {
            userDataFetched = this.fetchUserData();
            loggingDataFetched = this.fetchAttributeLogs();
            backlogDataFetched = this.fetchBackloggedRewards();
        } else {
            LoggingService.writeLog(Level.SEVERE,
                    String.format(
                            "Unable to connect to the %s database and create necessary tables.",
                            DatabaseService.enableMySQL ? "MySQL" : "SQLite"
                    )
            );
        }

        return databaseInitialised && userDataFetched && loggingDataFetched && backlogDataFetched;
    }

    private boolean initialiseDatabase() {
        // Create the data folder directory.
        File pluginFolder = getDataFolder();
        File dataFolder = new File(pluginFolder, "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdir();
        }

        final boolean connectionEstablished = DatabaseService.CreateConnection(new File(dataFolder, "database.db").getPath());
        boolean tablesCreated = false;

        if (connectionEstablished) {
            tablesCreated = DatabaseService.RunUpdate(DatabaseQuery.CreateUserTable) && DatabaseService.RunUpdate(DatabaseQuery.CreateLogsTable) && DatabaseService.RunUpdate(DatabaseQuery.CreateRewardBacklogTable);

            if (!tablesCreated) {
                LoggingService.writeLog(Level.SEVERE, "Failed to create the required database tables.");
            }
        }

        return connectionEstablished && tablesCreated;
    }

    private boolean fetchBackloggedRewards() {
        ResultSet backlogResults = DatabaseService.RunQuery(DatabaseQuery.SelectAllBackloggedRewards);
        if (backlogResults != null) {
            try {
                while (backlogResults.next()) {
                    final int rewardTypeInt = backlogResults.getInt(1);
                    final String uuid = backlogResults.getString(2);
                    final double amount = backlogResults.getDouble(3);
                    final String cause = backlogResults.getString(4);

                    AttributeLoggingService.rewardBacklog.add(
                            new WorkforceRewardBacklogItem(
                                    rewardTypeInt,
                                    uuid,
                                    amount,
                                    cause
                            )
                    );
                }

                LoggingService.writeLog(Level.INFO, String.format("Successfully loaded %s backlogged rewards.", AttributeLoggingService.rewardBacklog.size()));

                return true;
            } catch (SQLException err) {
                LoggingService.writeLog(Level.SEVERE, String.format("Failed to load backlogged rewards: %s", err.getMessage()));
                return false;
            }
        } else {
            LoggingService.writeLog(Level.SEVERE, "Could not fetch backlogged reward data!");
            return false;
        }
    }

    private boolean fetchAttributeLogs() {
        ResultSet logResults = DatabaseService.RunQuery(DatabaseQuery.SelectAllLogs);
        if (logResults != null) {
            try {
                while (logResults.next()) {
                    final int typeInt = logResults.getInt(1);
                    final Optional<AttributeLogType> optionalType = Arrays.stream(AttributeLogType.values()).filter((t) -> t.value == typeInt).findFirst();

                    final AttributeLogType type = optionalType.isPresent() ? optionalType.get() : null;
                    final String worldName = logResults.getString(2);
                    final double worldX = logResults.getDouble(3);
                    final double worldY = logResults.getDouble(4);
                    final double worldZ = logResults.getDouble(5);
                    final String uuid = logResults.getString(6);

                    if (type != null) {
                        AttributeLoggingService.logs.put(new Location(Bukkit.getWorld(worldName), worldX, worldY, worldZ), new WorkforceInteractionLogValue(uuid, type));
                    }
                }

                LoggingService.writeLog(Level.INFO, "Successfully loaded all block interaction logs.");

                return true;
            } catch (SQLException err) {
                LoggingService.writeLog(Level.SEVERE, String.format("Failed to load interaction logs: %s", err.getMessage()));
                return false;
            }
        } else {
            LoggingService.writeLog(Level.SEVERE, "Could not fetch logging data!");
            return false;
        }
    }

    private boolean fetchUserData() {
        ResultSet userDataResults = DatabaseService.RunQuery(DatabaseQuery.SelectAllUsers);
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

                LoggingService.writeLog(Level.INFO, String.format("Loaded %d users.", WorkforceService.userProfessions.size()));

                return true;
            } catch (SQLException err) {
                return false;
            }
        } else {
            LoggingService.writeLog(Level.SEVERE, "Could not fetch user data!");
            return false;
        }
    }

    private boolean initialiseCoreProtect() {
        Plugin plugin = getServer().getPluginManager().getPlugin("CoreProtect");
        final boolean onServer = plugin != null;
        final boolean apiEnabled = ((CoreProtect) plugin).getAPI().isEnabled();
        final boolean validApiVersion = ((CoreProtect) plugin).getAPI().APIVersion() >= 9;

        if (apiEnabled && validApiVersion) {
            CoreProtectService.SetAPI(((CoreProtect) plugin).getAPI());
        } else if (!onServer || !apiEnabled || !validApiVersion) {
            LoggingService.writeLog(Level.SEVERE, "Unable to find & initialise CoreProtect. SpecialisedWorkforce 2 is now disabling.");
        }

        return onServer && apiEnabled && validApiVersion;
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
        getServer().getPluginManager().registerEvents(new WorkforceBlockListener(), this);
        getServer().getPluginManager().registerEvents(new WorkforcePlayerListener(), this);
        getServer().getPluginManager().registerEvents(new WorkforceEntityListener(), this);
        getServer().getPluginManager().registerEvents(new WorkforceInventoryListener(), this);
        getServer().getPluginManager().registerEvents(new WorkforceLoggingListener(), this);
        getServer().getPluginManager().registerEvents(new WorkforceJoinLeaveListener(), this);

        return true;
    }
}
