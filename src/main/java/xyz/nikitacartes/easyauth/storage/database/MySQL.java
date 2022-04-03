package xyz.nikitacartes.easyauth.storage.database;

import xyz.nikitacartes.easyauth.storage.PlayerCache;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.HashMap;

import static xyz.nikitacartes.easyauth.EasyAuth.config;
import static xyz.nikitacartes.easyauth.utils.EasyLogger.logError;


public class MySQL {
    private static Connection MySQLConnection;

    /**
     * Connects to the MySQL.
     */
    public static void initialize() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            MySQLConnection = DriverManager.getConnection(
                    URLEncoder.encode(config.main.MySQLConnectionString, StandardCharsets.UTF_8)
            );
            PreparedStatement preparedStatement = MySQLConnection.prepareStatement("SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?;");
            preparedStatement.setString(1, config.main.MySQLTableName);
            if (!preparedStatement.executeQuery().next()) {
                MySQLConnection.createStatement().executeUpdate("CREATE TABLE `" + config.main.MySQLDatabase + "`.`" + config.main.MySQLTableName + "` ( `id` INT NOT NULL AUTO_INCREMENT , `uuid` VARCHAR(36) NOT NULL , `data` JSON NOT NULL , PRIMARY KEY (`id`), UNIQUE (`uuid`)) ENGINE = InnoDB;");
            }
        } catch (SQLException | ClassNotFoundException e) {
            logError(e.getMessage());
        }
    }

    /**
     * Closes database connection.
     */
    public static boolean close() {
        try {
            if (MySQLConnection != null) {
                MySQLConnection.close();
                return true;
            }
        } catch (SQLException e) {
            logError(e.getMessage());
        }
        return false;
    }

    /**
     * Tells whether DB connection is closed.
     *
     * @return false if connection is open, otherwise false
     */
    public static boolean isClosed() {
        return MySQLConnection == null;
    }


    /**
     * Inserts the data for the player.
     *
     * @param uuid uuid of the player to insert data for
     * @param data data to put inside database
     * @return true if operation was successful, otherwise false
     */
    @Deprecated
    public static boolean registerUser(String uuid, String data) {
        try {
            if (!isUserRegistered(uuid)) {
                PreparedStatement preparedStatement = MySQLConnection.prepareStatement("INSERT INTO " + config.main.MySQLTableName + " (uuid, data) VALUES (?, ?);");
                preparedStatement.setString(1, uuid);
                preparedStatement.setString(2, data);
                preparedStatement.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            logError("Register error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Checks if player is registered.
     *
     * @param uuid player's uuid
     * @return true if registered, otherwise false
     */
    public static boolean isUserRegistered(String uuid) {
        try {
            PreparedStatement preparedStatement = MySQLConnection.prepareStatement("SELECT * FROM " + config.main.MySQLTableName + " WHERE uuid = ?;");
            preparedStatement.setString(1, uuid);
            return preparedStatement.executeQuery().next();
        } catch (SQLException e) {
            logError(e.getMessage());
        }
        return false;
    }

    /**
     * Deletes data for the provided uuid.
     *
     * @param uuid uuid of player to delete data for
     */
    public static void deleteUserData(String uuid) {
        try {
            PreparedStatement preparedStatement = MySQLConnection.prepareStatement("DELETE FROM " + config.main.MySQLTableName + " WHERE uuid = ?;");
            preparedStatement.setString(1, uuid);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logError(e.getMessage());
        }
    }

    /**
     * Updates player's data.
     *
     * @param uuid uuid of the player to update data for
     * @param data data to put inside database
     */
    @Deprecated
    public static void updateUserData(String uuid, String data) {
        try {
            PreparedStatement preparedStatement = MySQLConnection.prepareStatement("UPDATE " + config.main.MySQLTableName + " SET data = ? WHERE uuid = ?;");
            preparedStatement.setString(1, data);
            preparedStatement.setString(1, uuid);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logError(e.getMessage());
        }
    }

    /**
     * Gets the hashed password from DB.
     *
     * @param uuid uuid of the player to get data for.
     * @return data as string if player has it, otherwise empty string.
     */
    public static String getUserData(String uuid) {
        try {
            if (isUserRegistered(uuid)) {
                PreparedStatement preparedStatement = MySQLConnection.prepareStatement("SELECT data FROM " + config.main.MySQLTableName + " WHERE uuid = ?;");
                preparedStatement.setString(1, uuid);
                ResultSet query = preparedStatement.executeQuery();
                query.next();
                return query.getString(1);
            }
        } catch (SQLException e) {
            logError("Error getting data: " + e.getMessage());
        }
        return "";
    }

    public static void saveFromCache(HashMap<String, PlayerCache> playerCacheMap) {
        try {
            PreparedStatement preparedStatement = MySQLConnection.prepareStatement("INSERT INTO " + config.main.MySQLTableName + " (uuid, data) VALUES (?, ?) ON DUPLICATE KEY UPDATE data = ?;");
            // Updating player data.
            playerCacheMap.forEach((uuid, playerCache) -> {
                String data = playerCache.toJson();
                try {
                    preparedStatement.setString(1, uuid);
                    preparedStatement.setString(2, data);
                    preparedStatement.setString(3, data);

                    preparedStatement.addBatch();
                } catch (SQLException e) {
                    logError("Error saving player data! (" + uuid + ") " + e.getMessage());
                }
            });
            preparedStatement.executeBatch();
        } catch (SQLException e) {
            logError("Error saving players data! " + e.getMessage());
        }
    }
}