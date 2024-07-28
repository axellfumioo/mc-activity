package spacecloud.mcwebbungee;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Mcweb_bungee extends Plugin {

    private Configuration config;
    private Connection connection;

    @Override
    public void onEnable() {
        loadConfig();
        if (!initializeDatabaseWithRetries(3)) {
            getLogger().severe("Database failed after 3 attempts (ERROR!!)");
            getProxy().getPluginManager().unregisterListeners(this);
            getProxy().getScheduler().cancel(this);
        } else {
            getLogger().info("Database success");
            setupDatabaseTables();
            getLogger().info("MC-WEB has been enabled.");

            getProxy().getPluginManager().registerListener(this, new ActivityListener(this));
        }
    }

    @Override
    public void onDisable() {
        closeDatabase();
        getLogger().info("MC-WEB has been disabled.");
    }

    private void loadConfig() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        File file = new File(getDataFolder(), "config.yml");

        if (!file.exists()) {
            try {
                file.createNewFile();
                Configuration configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
                configuration.set("database.host", "localhost");
                configuration.set("database.port", 3306);
                configuration.set("database.name", "mydatabase");
                configuration.set("database.user", "root");
                configuration.set("database.password", "password");
                ConfigurationProvider.getProvider(YamlConfiguration.class).save(configuration, file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean initializeDatabaseWithRetries(int maxRetries) {
        String host = config.getString("database.host");
        int port = config.getInt("database.port");
        String database = config.getString("database.name");
        String user = config.getString("database.user");
        String password = config.getString("database.password");

        String url = "jdbc:mysql://" + host + ":" + port + "/" + database;

        int attempts = 0;
        while (attempts < maxRetries) {
            try {
                connection = DriverManager.getConnection(url, user, password);
                getLogger().info("Connected to the database successfully.");
                return true;
            } catch (SQLException e) {
                attempts++;
                getLogger().severe("Failed to connect to the database (attempt " + attempts + "): " + e.getMessage());
                if (attempts >= maxRetries) {
                    return false;
                }
                try {
                    Thread.sleep(2000); // Wait 2 seconds before retrying
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return false;
    }


    private void setupDatabaseTables() {
        try (Statement statement = connection.createStatement()) {
            String createInitTable = "CREATE TABLE IF NOT EXISTS INIT (id INT AUTO_INCREMENT PRIMARY KEY)";
            statement.executeUpdate(createInitTable);

            String createPlayerDataTable = "CREATE TABLE IF NOT EXISTS PlayerData (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "username VARCHAR(255) NOT NULL, " +
                    "user_ip VARCHAR(255) NOT NULL" +
                    ")";
            statement.executeUpdate(createPlayerDataTable);

            String createActivityDataTable = "CREATE TABLE IF NOT EXISTS Activity (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "username VARCHAR(255) NOT NULL, " +
                    "user_ip VARCHAR(255) NOT NULL, " +
                    "activity VARCHAR(255) NOT NULL, " +
                    "date TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")";
            statement.executeUpdate(createActivityDataTable);
        } catch (SQLException e) {
            getLogger().severe("Failed to create tables: " + e.getMessage());
        }
    }


    private void closeDatabase() {
        if (connection != null) {
            try {
                connection.close();
                getLogger().info("Database connection closed.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                initializeDatabaseWithRetries(3);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }
}
