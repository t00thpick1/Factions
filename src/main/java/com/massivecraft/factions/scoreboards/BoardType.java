package com.massivecraft.factions.scoreboards;

public enum BoardType {

    FACTION_INFO("finfo");

    private String path;

    /**
     * Represents a type of scoreboard defined in the default config.yml
     *
     * @param path - path to the board's ConfigurationSection.
     */
    BoardType(String path) {
        this.path = path;
    }

    public String getPath() {
        return "scoreboard" + this.path;
    }

}
