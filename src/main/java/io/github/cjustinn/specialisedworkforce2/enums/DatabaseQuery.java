package io.github.cjustinn.specialisedworkforce2.enums;

import io.github.cjustinn.specialisedlib.Database.DatabaseMultiQuery;

public enum DatabaseQuery implements DatabaseMultiQuery {
    // Table Creators
    CreateUserTable(
            "create_user_table",
            "CREATE TABLE IF NOT EXISTS workforce_employment(uuid VARCHAR(36) NOT NULL,jobId VARCHAR(40) NOT NULL,jobLevel INT DEFAULT 1,jobExperience INT DEFAULT 0,active TINYINT DEFAULT 1,PRIMARY KEY(uuid, jobId));",
            "CREATE TABLE IF NOT EXISTS workforce_employment(uuid VARCHAR(36) NOT NULL,jobId VARCHAR(40) NOT NULL,jobLevel INT DEFAULT 1,jobExperience INT DEFAULT 0,active TINYINT DEFAULT 1,PRIMARY KEY(uuid, jobId));"
    ),
    CreateLogsTable(
            "create_logs_table",
            "CREATE TABLE IF NOT EXISTS workforce_interaction_log(interactionType INT NOT NULL,world VARCHAR(100) NOT NULL,x DOUBLE(9,2) NOT NULL,y DOUBLE(9,2) NOT NULL,z DOUBLE(9,2) NOT NULL,uuid VARCHAR(36) NOT NULL,PRIMARY KEY(world, x, y, z));",
            "CREATE TABLE IF NOT EXISTS workforce_interaction_log(interactionType INT NOT NULL,world VARCHAR(100) NOT NULL,x DOUBLE(9,2) NOT NULL,y DOUBLE(9,2) NOT NULL,z DOUBLE(9,2) NOT NULL,uuid VARCHAR(36) NOT NULL,PRIMARY KEY(world, x, y, z));"
    ),
    CreateRewardBacklogTable(
            "create_rwbl_table",
            "CREATE TABLE IF NOT EXISTS workforce_reward_backlog(rewardType INT NOT NULL,uuid VARCHAR(36) NOT NULL,amount DOUBLE(9, 2) NOT NULL, rewardFrom VARCHAR(250) NOT NULL,PRIMARY KEY(rewardType, uuid, rewardFrom));",
            "CREATE TABLE IF NOT EXISTS workforce_reward_backlog(rewardType INT NOT NULL,uuid VARCHAR(36) NOT NULL,amount DOUBLE(9, 2) NOT NULL, rewardFrom VARCHAR(250) NOT NULL,PRIMARY KEY(rewardType, uuid, rewardFrom));"
    ),
    // User Table Queries
    InsertUser(
            "insert_user",
            "INSERT INTO workforce_employment (uuid, jobId) VALUES (?, ?);",
            "INSERT INTO workforce_employment (uuid, jobId) VALUES (?, ?);"
    ),
    SelectAllUsers(
            "select_all_users",
            "SELECT uuid, jobId as id, jobLevel as level, jobExperience as experience, active FROM workforce_employment;",
            "SELECT uuid, jobId as id, jobLevel as level, jobExperience as experience, active FROM workforce_employment;"
    ),
    UpdateUser(
            "update_user",
            "UPDATE workforce_employment SET jobLevel = ?, jobExperience = ?, active = ? WHERE uuid = ? AND jobId = ?;",
            "UPDATE workforce_employment SET jobLevel = ?, jobExperience = ?, active = ? WHERE uuid = ? AND jobId = ?;"
    ),
    // Log Table Queries
    InsertLog(
            "insert_log",
            "INSERT INTO workforce_interaction_log (interactionType, world, x, y, z, uuid) VALUES (?, ?, ?, ?, ?, ?);",
            "INSERT INTO workforce_interaction_log (interactionType, world, x, y, z, uuid) VALUES (?, ?, ?, ?, ?, ?);"
    ),
    SelectAllLogs(
            "select_all_logs",
            "SELECT interactionType, world, x, y, z, uuid FROM workforce_interaction_log;",
            "SELECT interactionType, world, x, y, z, uuid FROM workforce_interaction_log;"
    ),
    UpdateLog(
            "update_log",
            "UPDATE workforce_interaction_log SET uuid = ? WHERE world = ? AND x = ? AND y = ? AND z = ?;",
            "UPDATE workforce_interaction_log SET uuid = ? WHERE world = ? AND x = ? AND y = ? AND z = ?;"
    ),
    DeleteLog(
            "delete_log",
            "DELETE FROM workforce_interaction_log WHERE world = ? AND x = ? AND y = ? AND z = ?",
            "DELETE FROM workforce_interaction_log WHERE world = ? AND x = ? AND y = ? AND z = ?"
    ),
    // Reward Backlog Table Queries
    InsertBacklogReward(
            "insert_backlog_reward",
            "INSERT INTO workforce_reward_backlog (rewardType, uuid, amount, rewardFrom) VALUES (?, ?, ?, ?);",
            "INSERT INTO workforce_reward_backlog (rewardType, uuid, amount, rewardFrom) VALUES (?, ?, ?, ?);"
    ),
    SelectAllBackloggedRewards(
            "select_all_rewards",
            "SELECT rewardType, uuid, amount, rewardFrom FROM workforce_reward_backlog;",
            "SELECT rewardType, uuid, amount, rewardFrom FROM workforce_reward_backlog;"
    ),
    UpdateBackloggedReward(
            "update_backlog_reward",
            "UPDATE workforce_reward_backlog SET amount = ? WHERE rewardType = ? AND uuid = ? AND rewardFrom = ?;",
            "UPDATE workforce_reward_backlog SET amount = ? WHERE rewardType = ? AND uuid = ? AND rewardFrom = ?;"
    ),
    DeleteBackloggedReward(
            "delete_backlog_reward",
            "DELETE FROM workforce_reward_backlog WHERE rewardType = ? AND uuid = ? AND rewardFrom = ?;",
            "DELETE FROM workforce_reward_backlog WHERE rewardType = ? AND uuid = ? AND rewardFrom = ?;"
    )
    ;

    private final String id;
    private final String mysql;
    private final String sqlite;

    DatabaseQuery(final String id, final String mysql, final String sqlite) {
        this.id = id;
        this.mysql = mysql;
        this.sqlite = sqlite;
    }

    @Override
    public String getQueryId() {
        return this.id;
    }

    @Override
    public String getMySQL() {
        return this.mysql;
    }

    @Override
    public String getSQLite() {
        return this.sqlite;
    }
}
