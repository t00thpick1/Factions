package com.massivecraft.factions.zcore.persist.mysql;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.P;
import com.massivecraft.factions.zcore.persist.MemoryBoard;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;


public class SQLBoard extends MemoryBoard {
    private static final String SELECT = "SELECT * FROM factions_board";
    private static final String UPDATE = "INSERT INTO factions_board (`fLocation`, `factionsId`) VALUES(?, ?) ON DUPLICATE KEY UPDATE `factionsId` = VALUES(`factionsId`)";
    private static final String DELETE = "DELETE FROM factions_board WHERE `fLocation` = ?";
    private Map<FLocation, String> pendingUpdates = new HashMap<FLocation, String>();

    public void removeAt(FLocation flocation) {
        if (flocationIds.containsKey(flocation)) {
            pendingUpdates.put(flocation, "0");
        }
        super.removeAt(flocation);
    }

    public void setIdAt(String id, FLocation flocation) {
        if (!id.equalsIgnoreCase("0")) {
            pendingUpdates.put(flocation, id);
        }
        super.setIdAt(id, flocation);
    }

    public boolean forceSave() {
        //Factions.log("Saving board to disk");

        Connection connection = null;
        PreparedStatement update = null;
        PreparedStatement delete = null;
        try {
            connection = FactionsMySQL.getInstance().getConnection();
            update = connection.prepareStatement(UPDATE);
            int up = 0;
            delete = connection.prepareStatement(DELETE);
            int del = 0;
            for (Entry<FLocation, String> entry : pendingUpdates.entrySet()) {
                if (entry.getValue().equalsIgnoreCase("0")) {
                    delete.setString(1, entry.getKey().toString());
                    delete.addBatch();
                    if (del++ % 500 == 0) {
                        delete.executeBatch();
                    }
                } else {
                    update.setString(1, entry.getKey().toString());
                    update.setString(2, entry.getValue());
                    update.addBatch();
                    if (up++ % 500 == 0) {
                        update.executeBatch();
                    }
                }
            }
            update.executeBatch();
            delete.executeBatch();
            pendingUpdates.clear();
        } catch (SQLException e) {
            e.printStackTrace();
            P.p.log("Failed to save the board.");
            return false;
        } finally {
            try {
                update.close();
            } catch(Exception e) {}
            try {
                delete.close();
            } catch(Exception e) {}
            try {
                connection.close();
            } catch(Exception e) {}
        }

        return true;
    }

    public boolean load() {
        P.p.log("Loading board from disk");

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = FactionsMySQL.getInstance().getConnection();
            statement = connection.prepareStatement(SELECT);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                flocationIds.put(FLocation.fromString(resultSet.getString("fLocation")), resultSet.getString("factionId"));
            }
        } catch (SQLException e) {
            // TODO failed load, not good
            return false;
        } finally {
            try {
                resultSet.close();
            } catch (SQLException e) {
            }
            try {
                statement.close();
            } catch (SQLException e) {
            }
            try {
                connection.close();
            } catch (SQLException e) {
            }
        }

        return true;
    }

    public void convertFrom(MemoryBoard old) {
        Board.instance = this;
        this.flocationIds = old.flocationIds;
        pendingUpdates.putAll(old.flocationIds);
        forceSave();
    }
}
