package me.alexdev.cosmicjackpot.struct;

public class JackpotHistory implements Comparable<JackpotHistory> {
    private String name;
    private double jackpotWins;
    private long jackpotWinnings;
    private double ticketsPurchased;

    public JackpotHistory(String name) {
        this.name = name;
    }

    public JackpotHistory() {
    }

    @Override
    public int compareTo(JackpotHistory other) {
        int winnings = Long.compare(other.getJackpotWinnings(), this.getJackpotWinnings());
        if (winnings != 0) {
            return winnings;
        }
        return Double.compare(other.getJackpotWins(), this.getJackpotWins());
    }

    public String getName() {
        return this.name;
    }

    public double getJackpotWins() {
        return this.jackpotWins;
    }

    public long getJackpotWinnings() {
        return this.jackpotWinnings;
    }

    public double getTicketsPurchased() {
        return this.ticketsPurchased;
    }

    public void setJackpotWins(double jackpotWins) {
        this.jackpotWins = jackpotWins;
    }

    public void setJackpotWinnings(long jackpotWinnings) {
        this.jackpotWinnings = jackpotWinnings;
    }

    public void setTicketsPurchased(double ticketsPurchased) {
        this.ticketsPurchased = ticketsPurchased;
    }
}
