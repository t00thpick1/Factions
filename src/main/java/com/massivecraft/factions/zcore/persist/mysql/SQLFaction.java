package com.massivecraft.factions.zcore.persist.mysql;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.SerializationUtils;
import org.bukkit.Location;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.struct.Relation;
import com.massivecraft.factions.util.LazyLocation;
import com.massivecraft.factions.zcore.persist.MemoryFaction;

public class SQLFaction extends MemoryFaction {
    @SuppressWarnings("unchecked")
    public SQLFaction(ResultSet resultSet) throws SQLException {
        this.id = resultSet.getString("id");
        this.tag = resultSet.getString("tag");
        this.description = resultSet.getString("description");
        this.invites = (Set<String>) SerializationUtils.deserialize(resultSet.getBytes("invites"));
        this.money = resultSet.getDouble("money");
        this.permanent = resultSet.getBoolean("permanent");
        this.open = resultSet.getBoolean("open");
        this.peaceful = resultSet.getBoolean("peaceful");
        this.peacefulExplosionsEnabled = resultSet.getBoolean("peacefulExplosionsEnabled");
        this.home = (LazyLocation) SerializationUtils.deserialize(resultSet.getBytes("home"));
        String string = resultSet.getString("permanentPower");
        this.permanentPower = string != null ? Integer.valueOf(string) : null;
        this.powerBoost = resultSet.getDouble("powerBoost");
        this.relationWish = (Map<String, Relation>) SerializationUtils.deserialize(resultSet.getBytes("relationWish"));
        this.claimOwnership = (Map<FLocation, Set<String>>) SerializationUtils.deserialize(resultSet.getBytes("claimOwnership"));
        this.fplayers = new HashSet<FPlayer>();
    }

    public SQLFaction(String id) {
        super(id);
    }

    public SQLFaction(MemoryFaction arg0) {
        super(arg0);
    }

    @Override
    public void invite(FPlayer fplayer) {
        ((SQLFactions) Factions.getInstance()).markForUpdate(this);
        super.invite(fplayer);
    }

    @Override
    public void deinvite(FPlayer fplayer) {
        ((SQLFactions) Factions.getInstance()).markForUpdate(this);
        super.deinvite(fplayer);
    }

    @Override
    public void setOpen(boolean isOpen) {
        if (getOpen() != isOpen) {
            ((SQLFactions) Factions.getInstance()).markForUpdate(this);
        }
        super.setOpen(isOpen);
    }

    @Override
    public void setPeaceful(boolean isPeaceful) {
        if (isPeaceful() != isPeaceful) {
            ((SQLFactions) Factions.getInstance()).markForUpdate(this);
        }
        super.setPeaceful(isPeaceful);
    }

    @Override
    public void setPeacefulExplosionsEnabled(boolean val) {
        if (getPeacefulExplosionsEnabled() != val) {
            ((SQLFactions) Factions.getInstance()).markForUpdate(this);
        }
        super.setPeacefulExplosionsEnabled(val);
    }

    @Override
    public void setPermanent(boolean isPermanent) {
        if (isPermanent() != isPermanent) {
            ((SQLFactions) Factions.getInstance()).markForUpdate(this);
        }
        super.setPermanent(isPermanent);
    }

    @Override
    public void setTag(String str) {
        if (!getTag().equals(str)) {
            ((SQLFactions) Factions.getInstance()).markForUpdate(this);
        }
        super.setTag(str);
    }

    @Override
    public void setDescription(String value) {
        if (!getDescription().equals(value)) {
            ((SQLFactions) Factions.getInstance()).markForUpdate(this);
        }
        super.setDescription(value);
    }

    @Override
    public void setHome(Location home) {
        ((SQLFactions) Factions.getInstance()).markForUpdate(this);
        super.setHome(home);
    }

    @Override
    public void confirmValidHome() {
        if (!Conf.homesMustBeInClaimedTerritory || this.home == null || (this.home.getLocation() != null && Board.getInstance().getFactionAt(new FLocation(this.home.getLocation())) == this)) {
            return;
        }

        msg("<b>Your faction home has been un-set since it is no longer in your territory.");
        this.home = null;
        ((SQLFactions) Factions.getInstance()).markForUpdate(this);
    }

    @Override
    public void setPermanentPower(Integer permanentPower) {
        if (getPermanentPower() != permanentPower) {
            ((SQLFactions) Factions.getInstance()).markForUpdate(this);
        }
        super.setPermanentPower(permanentPower);
    }

    @Override
    public void setPowerBoost(double powerBoost) {
        if (getPowerBoost() != powerBoost) {
            ((SQLFactions) Factions.getInstance()).markForUpdate(this);
        }
        super.setPowerBoost(powerBoost);
    }

    @Override
    public void setRelationWish(Faction otherFaction, Relation relation) {
        ((SQLFactions) Factions.getInstance()).markForUpdate(this);
        super.setRelationWish(otherFaction, relation);
    }

    @Override
    public void clearAllClaimOwnership() {
        ((SQLFactions) Factions.getInstance()).markForUpdate(this);
        super.clearAllClaimOwnership();
    }

    @Override
    public void clearClaimOwnership(FLocation loc) {
        ((SQLFactions) Factions.getInstance()).markForUpdate(this);
        super.clearClaimOwnership(loc);
    }

    @Override
    public void clearClaimOwnership(FPlayer player) {
        ((SQLFactions) Factions.getInstance()).markForUpdate(this);
        super.clearClaimOwnership(player);
    }

    @Override
    public void setPlayerAsOwner(FPlayer player, FLocation loc) {
        ((SQLFactions) Factions.getInstance()).markForUpdate(this);
        super.setPlayerAsOwner(player, loc);
    }

    @Override
    public void removePlayerAsOwner(FPlayer player, FLocation loc) {
        ((SQLFactions) Factions.getInstance()).markForUpdate(this);
        super.removePlayerAsOwner(player, loc);
    }

    @Override
    public void remove() {
        super.remove();
        ((SQLFactions) Factions.getInstance()).remove(this);
    }

    public void write(PreparedStatement updates) throws SQLException {
        updates.setString(1, this.id);
        updates.setString(2, this.tag);
        updates.setString(3, this.description);
        updates.setBoolean(4, this.open);
        updates.setBoolean(5, this.peaceful);
        updates.setBoolean(6, this.permanent);
        updates.setString(7, this.permanentPower != null ? this.permanentPower.toString() : null);
        updates.setBoolean(8, this.peacefulExplosionsEnabled);
        updates.setBytes(9, SerializationUtils.serialize(this.home));
        updates.setDouble(10, this.powerBoost);
        updates.setBytes(11, SerializationUtils.serialize((Serializable) this.relationWish));
        updates.setBytes(12, SerializationUtils.serialize((Serializable) this.claimOwnership));
        updates.setBytes(13, SerializationUtils.serialize((Serializable) this.invites));
    }
}
