package com.massivecraft.factions.zcore.persist;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.P;
import com.massivecraft.factions.struct.Relation;
import com.massivecraft.factions.util.AsciiCompass;

import org.bukkit.ChatColor;

import java.util.*;
import java.util.Map.Entry;


public abstract class MemoryBoard extends Board {
    public HashMap<FLocation, String> flocationIds = new HashMap<FLocation, String>();

    //----------------------------------------------//
    // Get and Set
    //----------------------------------------------//
    public String getIdAt(FLocation flocation) {
        if (!flocationIds.containsKey(flocation)) {
            return "0";
        }

        return flocationIds.get(flocation);
    }

    public Faction getFactionAt(FLocation flocation) {
        return Factions.getInstance().getFactionById(getIdAt(flocation));
    }

    public void setIdAt(String id, FLocation flocation) {
        clearOwnershipAt(flocation);

        if (id.equals("0")) {
            removeAt(flocation);
        }

        flocationIds.put(flocation, id);
    }

    public void setFactionAt(Faction faction, FLocation flocation) {
        setIdAt(faction.getId(), flocation);
    }

    public void removeAt(FLocation flocation) {
        clearOwnershipAt(flocation);
        flocationIds.remove(flocation);
    }

    // not to be confused with claims, ownership referring to further member-specific ownership of a claim
    public void clearOwnershipAt(FLocation flocation) {
        Faction faction = getFactionAt(flocation);
        if (faction != null && faction.isNormal()) {
            faction.clearClaimOwnership(flocation);
        }
    }

    public void unclaimAll(String factionId) {
        Faction faction = Factions.getInstance().getFactionById(factionId);
        if (faction != null && faction.isNormal()) {
            faction.clearAllClaimOwnership();
        }
        clean(factionId);
    }

    public void clean(String factionId) {
        Iterator<Entry<FLocation, String>> iter = flocationIds.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<FLocation, String> entry = iter.next();
            if (entry.getValue().equals(factionId)) {
                iter.remove();
            }
        }
    }

    // Is this coord NOT completely surrounded by coords claimed by the same faction?
    // Simpler: Is there any nearby coord with a faction other than the faction here?
    public boolean isBorderLocation(FLocation flocation) {
        Faction faction = getFactionAt(flocation);
        FLocation a = flocation.getRelative(1, 0);
        FLocation b = flocation.getRelative(-1, 0);
        FLocation c = flocation.getRelative(0, 1);
        FLocation d = flocation.getRelative(0, -1);
        return faction != getFactionAt(a) || faction != getFactionAt(b) || faction != getFactionAt(c) || faction != getFactionAt(d);
    }

    // Is this coord connected to any coord claimed by the specified faction?
    public boolean isConnectedLocation(FLocation flocation, Faction faction) {
        FLocation a = flocation.getRelative(1, 0);
        FLocation b = flocation.getRelative(-1, 0);
        FLocation c = flocation.getRelative(0, 1);
        FLocation d = flocation.getRelative(0, -1);
        return faction == getFactionAt(a) || faction == getFactionAt(b) || faction == getFactionAt(c) || faction == getFactionAt(d);
    }


    //----------------------------------------------//
    // Cleaner. Remove orphaned foreign keys
    //----------------------------------------------//

    public void clean() {
        Iterator<Entry<FLocation, String>> iter = flocationIds.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<FLocation, String> entry = iter.next();
            if (!Factions.getInstance().isValidFactionId(entry.getValue())) {
                P.p.log("Board cleaner removed " + entry.getValue() + " from " + entry.getKey());
                iter.remove();
            }
        }
    }

    //----------------------------------------------//
    // Coord count
    //----------------------------------------------//

    public int getFactionCoordCount(String factionId) {
        int ret = 0;
        for (String thatFactionId : flocationIds.values()) {
            if (thatFactionId.equals(factionId)) {
                ret += 1;
            }
        }
        return ret;
    }

    public int getFactionCoordCount(Faction faction) {
        return getFactionCoordCount(faction.getId());
    }

    public int getFactionCoordCountInWorld(Faction faction, String worldName) {
        String factionId = faction.getId();
        int ret = 0;
        Iterator<Entry<FLocation, String>> iter = flocationIds.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<FLocation, String> entry = iter.next();
            if (entry.getValue().equals(factionId) && entry.getKey().getWorldName().equals(worldName)) {
                ret += 1;
            }
        }
        return ret;
    }

    //----------------------------------------------//
    // Map generation
    //----------------------------------------------//

    /**
     * The map is relative to a coord and a faction north is in the direction of decreasing x east is in the direction
     * of decreasing z
     */
    public ArrayList<String> getMap(Faction faction, FLocation flocation, double inDegrees) {
        ArrayList<String> ret = new ArrayList<String>();
        Faction factionLoc = getFactionAt(flocation);
        ret.add(P.p.txt.titleize("(" + flocation.getCoordString() + ") " + factionLoc.getTag(faction)));

        int halfWidth = Conf.mapWidth / 2;
        int halfHeight = Conf.mapHeight / 2;
        FLocation topLeft = flocation.getRelative(-halfWidth, -halfHeight);
        int width = halfWidth * 2 + 1;
        int height = halfHeight * 2 + 1;

        if (Conf.showMapFactionKey) {
            height--;
        }

        Map<String, Character> fList = new HashMap<String, Character>();
        int chrIdx = 0;

        // For each row
        for (int dz = 0; dz < height; dz++) {
            // Draw and add that row
            String row = "";
            for (int dx = 0; dx < width; dx++) {
                if (dx == halfWidth && dz == halfHeight) {
                    row += ChatColor.AQUA + "+";
                } else {
                    FLocation flocationHere = topLeft.getRelative(dx, dz);
                    Faction factionHere = getFactionAt(flocationHere);
                    Relation relation = faction.getRelationTo(factionHere);
                    if (factionHere.isNone()) {
                        row += ChatColor.GRAY + "-";
                    } else if (factionHere.isSafeZone()) {
                        row += Conf.colorPeaceful + "+";
                    } else if (factionHere.isWarZone()) {
                        row += ChatColor.DARK_RED + "+";
                    } else if (factionHere == faction ||
                                       factionHere == factionLoc ||
                                       relation.isAtLeast(Relation.ALLY) ||
                                       (Conf.showNeutralFactionsOnMap && relation.equals(Relation.NEUTRAL)) ||
                                       (Conf.showEnemyFactionsOnMap && relation.equals(Relation.ENEMY))) {
                        if (!fList.containsKey(factionHere.getTag())) {
                            fList.put(factionHere.getTag(), Conf.mapKeyChrs[Math.min(chrIdx++, Conf.mapKeyChrs.length - 1)]);
                        }
                        char tag = fList.get(factionHere.getTag());
                        row += factionHere.getColorTo(faction) + "" + tag;
                    } else {
                        row += ChatColor.GRAY + "-";
                    }
                }
            }
            ret.add(row);
        }

        // Get the compass
        ArrayList<String> asciiCompass = AsciiCompass.getAsciiCompass(inDegrees, ChatColor.RED, P.p.txt.parse("<a>"));

        // Add the compass
        ret.set(1, asciiCompass.get(0) + ret.get(1).substring(3 * 3));
        ret.set(2, asciiCompass.get(1) + ret.get(2).substring(3 * 3));
        ret.set(3, asciiCompass.get(2) + ret.get(3).substring(3 * 3));

        // Add the faction key
        if (Conf.showMapFactionKey) {
            String fRow = "";
            for (String key : fList.keySet()) {
                fRow += String.format("%s%s: %s ", ChatColor.GRAY, fList.get(key), key);
            }
            ret.add(fRow);
        }

        return ret;
    }

    public abstract void convertFrom(MemoryBoard old);
}
