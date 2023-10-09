package io.github.cjustinn.specialisedworkforce2.services;

import io.github.cjustinn.specialisedworkforce2.models.SQL.MySQLCredentials;
import io.github.cjustinn.specialisedworkforce2.models.SQL.MySQLProperty;
import org.bukkit.Bukkit;

import javax.annotation.Nullable;
import java.sql.*;

public class SQLService {
    public static boolean useMySQL = false;
    public static MySQLCredentials mysqlCredentials = null;
    public static Connection connection = null;

    public static boolean CreateConnection(String sqlitePath) {
        try {
            Class.forName("org.sqlite.JDBC");

            if (SQLService.connection == null) {
                SQLService.connection = SQLService.useMySQL
                        ? DriverManager.getConnection(
                                SQLService.mysqlCredentials.getConnectionString(),
                                SQLService.mysqlCredentials.username,
                                SQLService.mysqlCredentials.password
                            )
                        : DriverManager.getConnection("jdbc:sqlite:" + sqlitePath);

                return true;
            }

            return false;
        } catch (ClassNotFoundException | SQLException err) {
            return false;
        }
    }

    public static @Nullable ResultSet RunQuery(String query, MySQLProperty[] properties) {
        try {
            PreparedStatement statement = SQLService.connection.prepareStatement(query);
            if (properties.length > 0) {
                for (MySQLProperty property : properties) {
                    switch (property.type) {
                        case "integer":
                            statement.setInt(property.index, (int) property.value);
                            break;
                        case "double":
                            statement.setDouble(property.index, (double) property.value);
                        default:
                            statement.setString(property.index, (String) property.value);
                            break;
                    }
                }
            }

            return statement.executeQuery();
        } catch (SQLException err) {
            return null;
        }
    }

    public static @Nullable ResultSet RunQuery(String query) {
        try {
            PreparedStatement statement = SQLService.connection.prepareStatement(query);
            return statement.executeQuery();
        } catch (SQLException err) {
            return null;
        }
    }

    public static boolean RunUpdate(String query, MySQLProperty[] properties) {
        try {
            PreparedStatement statement = connection.prepareStatement(query);
            if (properties.length > 0) {
                for (MySQLProperty property : properties) {
                    switch (property.type) {
                        case "integer":
                            statement.setInt(property.index, (int) property.value);
                            break;
                        case "double":
                            statement.setDouble(property.index, (double) property.value);
                            break;
                        default:
                            statement.setString(property.index, (String) property.value);
                            break;
                    }
                }
            }

            statement.executeUpdate();

            return true;
        } catch (SQLException err) {
            Bukkit.getConsoleSender().sendMessage(err.getMessage());
            return false;
        }
    }

    public static boolean CloseConnection() {
        if (SQLService.connection != null) {
            try {
                SQLService.connection.close();
                SQLService.connection = null;

                return true;
            } catch (SQLException err) {
                return false;
            }
        } else {
            return false;
        }
    }
}
