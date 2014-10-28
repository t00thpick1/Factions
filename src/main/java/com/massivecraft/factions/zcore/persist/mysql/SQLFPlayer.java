package com.massivecraft.factions.zcore.persist.mysql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.struct.ChatMode;
import com.massivecraft.factions.struct.Role;
import com.massivecraft.factions.zcore.persist.MemoryFPlayer;

public class SQLFPlayer extends MemoryFPlayer {
    public SQLFPlayer(ResultSet resultSet) throws SQLException {
        power = resultSet.getDouble("power");
        lastPowerUpdateTime = resultSet.getLong("lastPowerUpdateTime");
        lastLoginTime = resultSet.getLong("lastLoginTime");
        powerBoost = resultSet.getDouble("powerBoost");
        factionId = resultSet.getString("factionId");
        title = resultSet.getString("title");
        role = Role.valueOf(resultSet.getString("role"));
        chatMode = ChatMode.valueOf(resultSet.getString("chatMode"));
        id = resultSet.getString("id");
    }

    public SQLFPlayer(String id) {
        super(id);
    }

    public SQLFPlayer(MemoryFPlayer arg0) {
        super(arg0);
    }

    @Override
    public void setFaction(Faction faction) {
        super.setFaction(faction);
        ((SQLFPlayers) FPlayers.getInstance()).markForUpdate(this);
    }

    @Override
    public void setRole(Role role) {
        super.setRole(role);
        ((SQLFPlayers) FPlayers.getInstance()).markForUpdate(this);
    }

    @Override
    public void setPowerBoost(double powerBoost) {
        super.setPowerBoost(powerBoost);
        ((SQLFPlayers) FPlayers.getInstance()).markForUpdate(this);
    }

    @Override
    public void setChatMode(ChatMode chatMode) {
        super.setChatMode(chatMode);
        ((SQLFPlayers) FPlayers.getInstance()).markForUpdate(this);
    }

    @Override
    public void resetFactionData(boolean doSpoutUpdate) {
        super.resetFactionData(doSpoutUpdate);
        ((SQLFPlayers) FPlayers.getInstance()).markForUpdate(this);
    }

    @Override
    public void setLastLoginTime(long lastLoginTime) {
        super.setLastLoginTime(lastLoginTime);
        ((SQLFPlayers) FPlayers.getInstance()).markForUpdate(this);
    }

    @Override
    public void setTitle(String title) {
        super.setTitle(title);
        ((SQLFPlayers) FPlayers.getInstance()).markForUpdate(this);
    }

    @Override
    public void alterPower(double delta) {
        super.alterPower(delta);
        ((SQLFPlayers) FPlayers.getInstance()).markForUpdate(this);
    }

    @Override
    public void updatePower() {
        super.updatePower();
        ((SQLFPlayers) FPlayers.getInstance()).markForUpdate(this);
    }

    @Override
    public void onDeath() {
        super.onDeath();
        ((SQLFPlayers) FPlayers.getInstance()).markForUpdate(this);
    }

    @Override
    public void remove() {
        ((SQLFPlayers) FPlayers.getInstance()).markForRemoval(this);
    }

    public void write(PreparedStatement updates) throws SQLException {
        updates.setDouble(2, this.power);
        updates.setLong(3, lastPowerUpdateTime);
        updates.setLong(4, lastLoginTime);
        updates.setDouble(5, powerBoost);
        updates.setString(6, factionId);
        updates.setString(7, title);
        updates.setString(8, role.name());
        updates.setString(9, chatMode.name());
        updates.setString(1, id);
    }
}
