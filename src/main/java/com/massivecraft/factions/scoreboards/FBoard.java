package com.massivecraft.factions.scoreboards;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.P;
import com.massivecraft.factions.scoreboards.tasks.ExpirationTimer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.util.List;

public class FBoard {

    private Scoreboard scoreboard;
    private Objective obj;
    private Faction faction;

    public FBoard(Player player, Faction faction, boolean timed) {
        this.faction = faction;
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        setup(player);
        apply(player);

        if (timed) {
            new ExpirationTimer(player.getName(), scoreboard).runTaskLater(P.p, P.p.getConfig().getInt("scoreboard.expiration", 7) * 20L); // remove after 10 seconds.
        }
    }

    public void apply(Player player) {
        player.setScoreboard(scoreboard);
    }

    public void remove(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
    }

    private void setup(Player player) {
        FPlayer fPlayer = FPlayers.i.get(player);
        obj = scoreboard.registerNewObjective("FBoard", "dummy");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.setDisplayName(faction.getRelationTo(fPlayer).getColor() + faction.getTag());

        List<String> list = P.p.getConfig().getStringList("scoreboard.finfo");
        int place = 16; // list.size();
        for (String s : list) {

            String replaced = replace(s);
            String awesome = replaced.length() > 16 ? replaced.substring(0, 15) : replaced;
            Score score = obj.getScore(awesome);
            score.setScore(place);

            place--;
            if (place < 0) {
                break; // Let's not let the scoreboard get too big.
            }
        }
    }

    private String replace(String s) {
        FPlayer fLeader = faction.getFPlayerAdmin();
        String leader = fLeader == null ? "Server" : fLeader.getName().substring(0, fLeader.getName().length() > 14 ? 13 : fLeader.getName().length());
        return ChatColor.translateAlternateColorCodes('&', s.replace("{power}", String.valueOf(faction.getPowerRounded())).replace("{online}", String.valueOf(faction.getOnlinePlayers().size())).replace("{members}", String.valueOf(faction.getFPlayers().size())).replace("{leader}", leader).replace("{chunks}", String.valueOf(faction.getLandRounded())));
    }

}
