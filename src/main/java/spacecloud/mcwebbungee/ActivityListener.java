package spacecloud.mcwebbungee;

import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ActivityListener implements Listener {

    private final Mcweb_bungee plugin;

    public ActivityListener(Mcweb_bungee plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        String uuid = player.getUniqueId().toString();
        String username = player.getName();
        String userIp = player.getAddress().getHostString();

        if (!isPlayerInDatabase(username)) {
            insertPlayerData(uuid, username, userIp);
        }
        logPlayerActivity(uuid, username, userIp, "Join server");
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        ProxiedPlayer player = event.getPlayer();
        logPlayerActivity(player.getUniqueId().toString(), player.getName(), player.getAddress().getHostString(), "Leave server");
    }

    @EventHandler
    public void onServerSwitch(ServerSwitchEvent event) {
        ProxiedPlayer player = event.getPlayer();
        String activity = "Change server to " + player.getServer().getInfo().getName();
        logPlayerActivity(player.getUniqueId().toString(), player.getName(), player.getAddress().getHostString(), activity);
    }

    private boolean isPlayerInDatabase(String username) {
        String query = "SELECT * FROM PlayerData WHERE username = ?";
        try (Connection connection = plugin.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check player data: " + e.getMessage());
            return false;
        }
    }

    private void insertPlayerData(String uuid, String username, String userIp) {
        String query = "INSERT INTO PlayerData (uuid, username, user_ip) VALUES (?, ?, ?)";
        try (Connection connection = plugin.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, uuid);
            statement.setString(2, username);
            statement.setString(3, userIp);
            statement.executeUpdate();
            plugin.getLogger().info("Inserted new player data for " + username);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to insert player data: " + e.getMessage());
        }
    }

    private void logPlayerActivity(String uuid, String username, String userIp, String activity) {
        String query = "INSERT INTO Activity (uuid, username, user_ip, activity, date) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)";
        try (Connection connection = plugin.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, uuid);
            statement.setString(2, username);
            statement.setString(3, userIp);
            statement.setString(4, activity);
            statement.executeUpdate();
            plugin.getLogger().info("Logged activity for player " + username + ": " + activity);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to log player activity: " + e.getMessage());
        }
    }

}
