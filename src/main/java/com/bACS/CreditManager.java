package com.bACS;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CreditManager {
    private final Map<UUID, Integer> playerCredits = new HashMap<>();
    private final Map<UUID, Integer> loseStreak = new HashMap<>();
    private static final int DEFAULT_CREDITS = 1000;

    public int getCredits(UUID playerId) {
        return playerCredits.getOrDefault(playerId, DEFAULT_CREDITS);
    }

    public boolean hasPlayer(UUID playerId) {
        return playerCredits.containsKey(playerId);
    }

    public void setCredits(UUID playerId, int amount) {
        playerCredits.put(playerId, amount);
    }

    public void addCredits(UUID playerId, int amount) {
        playerCredits.put(playerId, getCredits(playerId) + amount);
    }

    public boolean spendCredits(UUID playerId, int amount) {
        int current = getCredits(playerId);
        if (current < amount) return false;
        playerCredits.put(playerId, current - amount);
        return true;
    }

    public void onRoundWin(UUID playerId) {
        addCredits(playerId, 2300);
        loseStreak.put(playerId, 0);
    }

    public void onRoundLose(UUID playerId) {
        int streak = loseStreak.getOrDefault(playerId, 0) + 1;
        loseStreak.put(playerId, streak);

        switch (streak) {
            case 1:
                addCredits(playerId, 1500);
                break;
            case 2:
                addCredits(playerId, 2000);
                break;
            default:
                addCredits(playerId, 2500);
                break;
        }
    }

    public void onKill(UUID playerId) {
        addCredits(playerId, 200);
    }

    public int getNextRoundMinCredits(UUID playerId) {
        int current = getCredits(playerId);
        int streak = loseStreak.getOrDefault(playerId, 0);
        int nextStreak = streak + 1;

        int minGain;
        if (nextStreak == 1) {
            minGain = 1500;
        } else if (nextStreak == 2) {
            minGain = 2000;
        } else {
            minGain = 2500;
        }

        return current + minGain;
    }

    public void resetPlayer(UUID playerId) {
        playerCredits.remove(playerId);
        loseStreak.remove(playerId);
    }

    public void resetAll() {
        playerCredits.clear();
        loseStreak.clear();
    }
}