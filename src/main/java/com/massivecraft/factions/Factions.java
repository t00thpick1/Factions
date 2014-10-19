package com.massivecraft.factions;

import java.util.ArrayList;
import java.util.Set;

public abstract class Factions {
    private final static Factions instance = getFactionsImpl();

    public abstract Faction getFactionById(String id);

    public abstract Faction getByTag(String str);

    public abstract Faction getBestTagMatch(String start);

    public abstract boolean isTagTaken(String str);

    public abstract boolean isValidFactionId(String id);

    public abstract Faction createFaction();

    public abstract void removeFaction(String id);

    public abstract Set<String> getFactionTags();

    public abstract ArrayList<Faction> getAllFactions();

    public abstract Faction getNone();

    public abstract Faction getSafeZone();

    public abstract Faction getWarZone();

    public abstract void forceSave();

    public static Factions getInstance() {
        return instance;
    }

    private static Factions getFactionsImpl() {
        // TODO Auto-generated method stub
        return null;
    }
}
