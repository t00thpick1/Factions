package com.massivecraft.factions.zcore.persist.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.zcore.persist.MemoryFPlayer;
import com.massivecraft.factions.zcore.persist.MemoryFPlayers;

public class SQLFPlayers extends MemoryFPlayers {
    private static final String CLEAN = "DELETE FROM factions_players WHERE `factionId` = 0";
    private static final String LOAD = "SELECT * FROM factions_players WHERE NOT factionId = '0'";
    private static final String UPDATES = "INSERT INTO factions_players (`id`, `power`, `lastPowerUpdateTime`, `lastLoginTime`, `powerBoost`, `factionId`, `title`, `role`, `chatMode`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)" +
            " ON DUPLICATE KEY UPDATE `power` = VALUES(`power`), `lastPowerUpdateTime` = VALUES(`lastPowerUpdateTime`), `lastLoginTime` = VALUES(`lastLoginTime`), `powerBoost` = VALUES(`powerBoost`), `factionId` = VALUES(`factionId`)" +
            ", `title` = VALUES(`title`), `role` = VALUES(`role`), `chatMode` = VALUES(`chatMode`)";
    private static final String DELETIONS = "DELETE FROM factions_players WHERE `id` IN (%s)";
    private List<String> pendingDeletions = new ArrayList<String>();
    private Map<String, SQLFPlayer> pendingUpdates = new HashMap<String, SQLFPlayer>();

    @Override
    public void load() {
        Connection connection = null;
        ResultSet resultSet = null;
        try {
            connection = FactionsMySQL.getInstance().getConnection();
            PreparedStatement clean = connection.prepareStatement(CLEAN);
            clean.executeUpdate();
            clean.close();
            PreparedStatement load = connection.prepareStatement(LOAD);
            resultSet = load.executeQuery();
            while (resultSet.next()) {
                SQLFPlayer fPlayer = new SQLFPlayer(resultSet);
                this.fPlayers.put(fPlayer.getId(), fPlayer);
            }
            resultSet.close();
            load.close();
        } catch (SQLException e) {
            // Failed load.... not good.
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    @Override
    public FPlayer generateFPlayer(String string) {
        SQLFPlayer fPlayer = new SQLFPlayer(string);
        pendingUpdates.put(string, fPlayer);
        return fPlayer;
    }

    @Override
    public void forceSave() {
        Connection connection = null;
        PreparedStatement updates = null;
        PreparedStatement deletion = null;
        try {
            connection = FactionsMySQL.getInstance().getConnection();
            int batchSize = 0;
            updates = connection.prepareStatement(UPDATES);
            for (SQLFPlayer fPlayer : pendingUpdates.values()) {
                if (!fPlayer.shouldBeSaved()) {
                    pendingDeletions.add(fPlayer.getId());
                }
                fPlayer.write(updates);
                updates.addBatch();
                if (batchSize++ % 500 == 0) {
                    updates.executeBatch();
                }
            }
            updates.executeBatch();
            pendingUpdates.clear();
            deletion = connection.prepareStatement(String.format(DELETIONS, Joiner.on(", ").join(pendingDeletions)));
            // Ids should only be internally generated numbers so no need to sanitize.
            deletion.execute();
            pendingDeletions.clear();
        } catch (SQLException e) {
            // Failed database sync.... not good.
            e.printStackTrace();
        } finally {
            try {
                connection.close();
            } catch (Exception e) {
            }
            try {
                updates.close();
            } catch (Exception e) {
            }
            try {
                deletion.close();
            } catch (Exception e) {
            }
        }
    }

    public void markForUpdate(SQLFPlayer fPlayer) {
        pendingUpdates.put(fPlayer.getId(), fPlayer);
    }

    public void markForRemoval(SQLFPlayer fPlayer) {
        pendingUpdates.remove(fPlayer.getId());
        pendingDeletions.add(fPlayer.getId());
    }

    public void convertFrom(MemoryFPlayers old) {
        FPlayers.instance = this;
        this.pendingUpdates.putAll(Maps.transformValues(old.fPlayers, new Function<FPlayer, SQLFPlayer>() {
            @Override
            public SQLFPlayer apply(FPlayer arg0) {
                return new SQLFPlayer((MemoryFPlayer) arg0);
            }
        }));
        this.fPlayers.putAll(pendingUpdates);
        forceSave();
    }
}
