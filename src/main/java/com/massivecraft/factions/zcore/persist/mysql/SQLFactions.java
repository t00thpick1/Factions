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
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.zcore.persist.MemoryFaction;
import com.massivecraft.factions.zcore.persist.MemoryFactions;

public class SQLFactions extends MemoryFactions {
    private static final String LOAD = "SELECT * FROM factions_factions";
    private static final String UPDATES = "INSERT INTO factions_factions (`id`,tag`,`description`,`open`,`peaceful`,`permanent`,`permanentPower`,`peacefulExplosionsEnabled`," +
            "`home`,`powerBoost`,`relationWish`,`claimOwnership`,`invites`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE SET `tag` = VALUES(`tag`), " +
            "`description` = VALUES(`description`), `open` = VALUES(`open`), `peaceful` = VALUES(`peaceful`), `permanent` = VALUES(`permanent`), `permanentPower` = VALUES(`permanentPower`), `peacefulExplosionsEnabled` = VALUES(`peacefulExplosionsEnabled`), " +
            "`home` = VALUES(`home`), `powerBoost` = VALUES(`powerBoost`), `relationWish` = VALUES(`relationWish`), `claimOwnership` = VALUES(`claimOwnership`), `invites` = VALUES(`invites`)";
    private static final String DELETIONS = "DELETE FROM factions_factions WHERE `id` IN (%s)";
    private List<String> pendingDeletions = new ArrayList<String>();
    private Map<String, SQLFaction> pendingUpdates = new HashMap<String, SQLFaction>();

    @Override
    public void load() {
        Connection connection = null;
        ResultSet resultSet = null;
        try {
            connection = FactionsMySQL.getInstance().getConnection();
            PreparedStatement load = connection.prepareStatement(LOAD);
            resultSet = load.executeQuery();
            while (resultSet.next()) {
                SQLFaction faction = new SQLFaction(resultSet);
                factions.put(faction.getId(), faction);
                int id = Integer.valueOf(faction.getId());
                nextId = nextId > id ? nextId : id;
            }
            resultSet.close();
            load.close();
        } catch (SQLException e) {
            // Failed save.... not good.
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
    public Faction generateFactionObject(String string) {
        SQLFaction faction = new SQLFaction(string);
        pendingUpdates.put(string, faction);
        return faction;
    }

    @Override
    public Faction generateFactionObject() {
        return generateFactionObject(Integer.valueOf(nextId++).toString());
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
            for (SQLFaction faction : pendingUpdates.values()) {
                faction.write(updates);
                updates.addBatch();
                if (batchSize++ % 500 == 0) {
                    updates.executeBatch();
                }
            }
            updates.executeBatch();
            pendingUpdates.clear();
            deletion = connection.prepareStatement(String.format(DELETIONS, Joiner.on(", ").join(pendingDeletions))); // Ids should only be internally generated numbers so no need to sanitize.
            deletion.execute();
            pendingDeletions.clear();
        } catch (SQLException e) {
            // Failed database sync.... not good.
            e.printStackTrace();
        } finally {
            try {
                updates.close();
            } catch (Exception e) {
            }
            try {
                deletion.close();
            } catch (Exception e) {
            }
            try {
                connection.close();
            } catch (Exception e) {
            }
        }
    }

    public void markForUpdate(SQLFaction sqlFaction) {
        pendingUpdates.put(sqlFaction.getId(), sqlFaction);
    }

    public void remove(SQLFaction sqlFaction) {
        pendingUpdates.remove(sqlFaction.getId());
        pendingDeletions.add(sqlFaction.getId());
    }

    @Override
    public void convertFrom(MemoryFactions old) {
        Factions.instance = this;
        this.pendingUpdates.putAll(Maps.transformValues(old.factions, new Function<Faction, SQLFaction>() {
            @Override
            public SQLFaction apply(Faction arg0) {
                return new SQLFaction((MemoryFaction) arg0);
            }
        }));
        this.factions.putAll(pendingUpdates);
        this.nextId = old.nextId;
        forceSave();
    }

}
