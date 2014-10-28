package com.massivecraft.factions.zcore.persist.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.bukkit.scheduler.BukkitRunnable;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.P;
import com.massivecraft.factions.zcore.persist.MemoryBoard;
import com.massivecraft.factions.zcore.persist.MemoryFPlayers;
import com.massivecraft.factions.zcore.persist.MemoryFactions;

public class FactionsMySQL {
    private static FactionsMySQL instance;
    private DataSource dataSource;
    private String password = Conf.mySQLPassword;
    private String userName = Conf.mySQLUsername;
    private String connectionString = Conf.mySQLConnectionString;

    public FactionsMySQL() {
        PoolProperties properties = new PoolProperties();
        properties.setDriverClassName("com.mysql.jdbc.Driver");
        properties.setUrl(connectionString);
        properties.setUsername(userName);
        properties.setPassword(password);
        properties.setMaxIdle(1);
        properties.setMaxActive(1);
        properties.setInitialSize(0);
        properties.setMaxWait(-1);
        properties.setRemoveAbandoned(true);
        properties.setRemoveAbandonedTimeout(60);
        properties.setTestOnBorrow(true);
        properties.setValidationQuery("SELECT 1");
        properties.setValidationInterval(30000);
        dataSource = new DataSource(properties);
        createTables();
    }

    private void createTables() {
        Connection connection = null;
        PreparedStatement statement = null;
        Statement create = null;
        ResultSet resultSet = null;
        try {
            connection = getConnection();
            statement = connection.prepareStatement("SELECT table_name FROM INFORMATION_SCHEMA.TABLES"
                    + " WHERE table_schema = ?"
                    + " AND table_name = ?");
            statement.setString(1, connectionString.substring(connectionString.lastIndexOf("/") + 1));
            statement.setString(2, "factions_factions");
            resultSet = statement.executeQuery();
            if (!resultSet.next()) {
                create = connection.createStatement();
                create.executeUpdate("CREATE TABLE IF NOT EXISTS `factions_factions` ("
                        + "`id` varchar(64),"
                        + "`tag` varchar(100) NOT NULL,"
                        + "`description` varchar(200) NOT NULL,"
                        + "`open` boolean NOT NULL,"
                        + "`peaceful` boolean NOT NULL,"
                        + "`permanent` boolean NOT NULL,"
                        + "`peacefulExplosionsEnabled` boolean NOT NULL,"
                        + "`home` varchar(15) NOT NULL,"
                        + "`powerBoost` INT NOT NULL,"
                        + "`permanentPower` VARCHAR(20),"
                        + "`relationWish` LONG VARBINARY NOT NULL,"
                        + "`claimOwnership` LONG VARBINARY NOT NULL,"
                        + "`invites` LONG VARBINARY NOT NULL,"
                        + "PRIMARY KEY (`id`),"
                        + "UNIQUE KEY `id` (`id`),"
                        + "UNIQUE KEY `tag` (`tag`))"
                        + " DEFAULT CHARSET=latin1;");
                create.close();
            }
            resultSet.close();
            statement.setString(2, "factions_players");
            resultSet = statement.executeQuery();
            if (!resultSet.next()) {
                create = connection.createStatement();
                create.executeUpdate("CREATE TABLE IF NOT EXISTS `factions_players` ("
                        + "`id` varchar(64),"
                        + "`lastPowerUpdateTime` BIGINT,"
                        + "`lastLoginTime` BIGINT,"
                        + "`factionId` varchar(20) NOT NULL,"
                        + "`title` varchar(200) NOT NULL,"
                        + "`role` varchar(20) NOT NULL,"
                        + "`chatMode` varchar(20) NOT NULL,"
                        + "`power` INT NOT NULL,"
                        + "`powerBoost` INT NOT NULL,"
                        + "PRIMARY KEY (`id`),"
                        + "UNIQUE KEY `id` (`id`))"
                        + " DEFAULT CHARSET=latin1;");
                create.close();
            }
            resultSet.close();
            statement.setString(2, "factions_board");
            resultSet = statement.executeQuery();
            if (!resultSet.next()) {
                create = connection.createStatement();
                create.executeUpdate("CREATE TABLE IF NOT EXISTS `factions_board` ("
                        + "`fLocation` varchar(100),"
                        + "`factionsId` varchar(20) NOT NULL,"
                        + "PRIMARY KEY (`fLocation`),"
                        + "UNIQUE KEY `fLocation` (`fLocation`))"
                        + " DEFAULT CHARSET=latin1;");
                create.close();
            }
            resultSet.close();
        } catch (SQLException e) {
            // Didn't create tables, bad.
        } finally {
            try {
                resultSet.close();
            } catch (Exception e) {}
            try {
                create.close();
            } catch (Exception e) {}
            try {
                statement.close();
            } catch (Exception e) {}
            try {
                connection.close();
            } catch (Exception e) {}
        }
    }

    public static FactionsMySQL getInstance() {
        if (instance == null) {
            instance = new FactionsMySQL();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void convertTo() {
        if (!(Factions.getInstance() instanceof MemoryFactions)) {
            return;
        }
        if (!(FPlayers.getInstance() instanceof MemoryFPlayers)) {
            return;
        }
        if (!(Board.getInstance() instanceof MemoryBoard)) {
            return;
        }
        getInstance();
        new BukkitRunnable() {
            @Override
            public void run() {
                Logger logger = P.p.getLogger();
                logger.info("Beginning Board conversion to MySQL");
                new SQLBoard().convertFrom((MemoryBoard) Board.getInstance());
                logger.info("Board Converted");
                logger.info("Beginning FPlayers conversion to MySQL");
                new SQLFPlayers().convertFrom((MemoryFPlayers) FPlayers.getInstance());
                logger.info("FPlayers Converted");
                logger.info("Beginning Factions conversion to MySQL");
                new SQLFactions().convertFrom((MemoryFactions) Factions.getInstance());
                logger.info("Factions Converted");
                logger.info("Refreshing object caches");
                for (FPlayer fPlayer : FPlayers.getInstance().getAllFPlayers()) {
                    Faction faction = Factions.getInstance().getFactionById(fPlayer.getFactionId());
                    faction.addFPlayer(fPlayer);
                }
                logger.info("Conversion Complete");
            }
        }.runTaskAsynchronously(P.p);
    }
}
