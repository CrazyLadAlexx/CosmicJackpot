package me.alexdev.cosmicjackpot.struct;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import me.alexdev.cosmicjackpot.CosmicJackpot;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class Jackpot {
    private static final int TICKET_PRICE = 10000;

    public static transient double TAX = 0.1;

    private Map<PlayerInfo, Integer> entries = new ConcurrentHashMap<>();
    private long totalWinnings;
    private int secondsLeft;

    @Override
    public String toString() {
        return "Jackpot{totalWinnings=" + this.totalWinnings
                + ", secondsLeft=" + this.secondsLeft
                + ", entries=" + this.entries.size() + "}";
    }

    public Jackpot() {
    }

    public Jackpot(int timer) {
        this.secondsLeft = timer;
    }

    public void purchaseTicket(Player player, int tickets) {
        double cost = tickets * this.getTicketPrice();
        EconomyResponse removed = CosmicJackpot.economy.withdrawPlayer(player, cost);
        if (!removed.transactionSuccess()) {
            Bukkit.getLogger().info("Transaction not successful for " + player.getName() + " for $" + cost + " with " + tickets + " tickets purchased!");
            return;
        }

        PlayerInfo info = new PlayerInfo(player);
        Integer current = this.entries.get(info);
        Bukkit.getLogger().info("(CosmicJackpot) Purchasing " + tickets + " ticket(s) for " + player.getName()
                + (current != null ? " with " + current + " current total." : " with 0 current."));
        this.entries.put(info, (current == null ? 0 : current) + tickets);
        this.totalWinnings += (long) cost;
    }

    public double getPercentHolder(Player player) {
        return this.getPercentHolder(player.getUniqueId());
    }

    public double getPercentHolder(UUID uuid) {
        int ticketsOwned = this.getTicketsPurchased(uuid);
        if (ticketsOwned <= 0) {
            return 0.0;
        }
        int totalTickets = this.getTicketsSold();
        if (totalTickets <= 0) {
            return 0.0;
        }
        return (double)ticketsOwned / (double)totalTickets * 100.0;
    }

    public int getTicketsPurchased(Player player) {
        return this.getTicketsPurchased(player.getUniqueId());
    }

    public int getTicketsPurchased(UUID uuid) {
        int found = 0;
        for (Map.Entry<PlayerInfo, Integer> entry : this.entries.entrySet()) {
            if (entry.getKey().getUuid().equals(uuid)) {
                found += entry.getValue();
            }
        }
        return found;
    }

    public int getTicketPrice() {
        return TICKET_PRICE;
    }

    public int getTicketsSold() {
        int total = 0;
        for (Integer tickets : this.entries.values()) {
            total += tickets;
        }
        return total;
    }

    public long getPlayerWinnings() {
        long taxes = (long) (this.totalWinnings * TAX);
        return this.totalWinnings - taxes;
    }

    public PlayerInfo getRandomWinner() {
        if (this.entries.isEmpty()) {
            return null;
        }

        int winningTicket = ThreadLocalRandom.current().nextInt(this.getTicketsSold());
        int cursor = 0;
        for (Map.Entry<PlayerInfo, Integer> entry : this.entries.entrySet()) {
            cursor += entry.getValue();
            if (winningTicket < cursor) {
                return entry.getKey();
            }
        }
        return null;
    }

    public Map<PlayerInfo, Integer> getEntries() {
        return this.entries;
    }

    public long getTotalWinnings() {
        return this.totalWinnings;
    }

    public void setTotalWinnings(long totalWinnings) {
        this.totalWinnings = totalWinnings;
    }

    public int getSecondsLeft() {
        return this.secondsLeft;
    }

    public void setSecondsLeft(int secondsLeft) {
        this.secondsLeft = secondsLeft;
    }
}
