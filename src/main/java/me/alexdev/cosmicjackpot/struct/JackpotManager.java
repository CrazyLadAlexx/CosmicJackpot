package me.alexdev.cosmicjackpot.struct;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import me.alexdev.cosmicjackpot.CosmicJackpot;
import me.alexdev.cosmicjackpot.utils.ItemUtils;
import me.alexdev.cosmicjackpot.utils.TimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

public class JackpotManager {
    public static final String CONFIRM_MENU_TITLE = "Confirm Ticket Purchase";

    private static final int CACHE_REFRESH_MILLIS = 10000;
    private static final int[] ANNOUNCEMENT_SECONDS = {300, 60, 30, 10, 5, 4, 3, 2, 1};
    private static final String HISTORY_FILE = "jackpotHistory.json";
    private static final String CURRENT_JACKPOT_FILE = "currentJackpot.json";
    private static final String NOTIFICATION_TOGGLES_FILE = "notificationToggles.json";

    private File historyFile;
    private File currentJackpotFile;
    private File notificationToggleFile;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().enableComplexMapKeySerialization().create();
    private final Type jackpotHistoryToken = new TypeToken<Map<UUID, JackpotHistory>>(){}.getType();
    private final Type jackpotToken = new TypeToken<Jackpot>(){}.getType();
    private final Type togglesToken = new TypeToken<HashSet<UUID>>(){}.getType();
    private final DecimalFormat moneyFormat = new DecimalFormat("#,###.##");

    private Set<UUID> toggledNotifications = new HashSet<>();
    private Map<UUID, JackpotHistory> jackpotHistory;
    private Jackpot currentJackpot;
    private long taxesCollected;
    private int defaultDrawTime = (int) TimeUnit.HOURS.toSeconds(2L);
    private List<JackpotHistory> sortedHistory = List.of();
    private long lastListUpdate;

    public void onJackpotTick() {
        int secondsLeft = this.currentJackpot.getSecondsLeft();
        if (secondsLeft <= 0) {
            this.drawWinner();
            return;
        }

        if (this.shouldAnnounce(secondsLeft)) {
            this.broadcastCountdown(secondsLeft);
        }
        this.currentJackpot.setSecondsLeft(secondsLeft - 1);
    }

    public void drawWinner() {
        PlayerInfo winner = this.currentJackpot.getRandomWinner();
        if (winner == null) {
            Bukkit.broadcastMessage(ChatColor.RED.toString() + ChatColor.BOLD + "(!) " + ChatColor.RED + "No Jackpot winner available due to no ticket sales!");
            this.startNewJackpot();
            return;
        }

        this.broadcastWinner(winner);

        double winnings = this.currentJackpot.getPlayerWinnings();
        double taxesCollected = this.currentJackpot.getTotalWinnings() - this.currentJackpot.getPlayerWinnings();
        this.startNewJackpot();
        JackpotHistory history = this.jackpotHistory.computeIfAbsent(winner.getUuid(), uuid -> new JackpotHistory(winner.getUsername()));
        history.setJackpotWinnings(history.getJackpotWinnings() + (long)winnings);
        history.setJackpotWins(history.getJackpotWins() + 1.0);
        OfflinePlayer player = Bukkit.getPlayer(winner.getUuid());
        if (player == null) {
            player = Bukkit.getOfflinePlayer(winner.getUuid());
        } else {
            ((Player) player).sendMessage(ChatColor.GREEN.toString() + ChatColor.BOLD + "+ $" + ChatColor.GREEN + this.moneyFormat.format(winnings));
        }
        CosmicJackpot.economy.depositPlayer(player, winnings);
        Bukkit.getLogger().info("Depositing $" + winnings + " into " + player.getName() + "'s player (" + (player instanceof Player ? "Online" : "Offline") + ").");
        this.taxesCollected = (long) (this.taxesCollected + taxesCollected);
        this.saveWinnings();
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "contest add " + winner.getUuid() + " lotteryMoneyWin " + winnings);
    }

    public Inventory getJackpotConfirmMenu(int ticketsToBuy, double totalCost) {
        Inventory inventory = Bukkit.createInventory(null, 9, CONFIRM_MENU_TITLE);
        for (int i = 5; i < 9; ++i) {
            inventory.setItem(i, ItemUtils.createItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + ChatColor.BOLD.toString() + "Cancel Purchase", List.of(ChatColor.GRAY + "Click to " + ChatColor.RED + "cancel" + ChatColor.GRAY + " ticket purchase.")));
        }
        List<String> lore = new ArrayList<>(List.of(
                ChatColor.GRAY + "Click to " + ChatColor.GREEN + "confirm" + ChatColor.GRAY + " purchase of",
                ChatColor.GREEN + this.moneyFormat.format(ticketsToBuy) + ChatColor.GRAY + " ticket(s) for " + ChatColor.GREEN + ChatColor.BOLD + "$" + ChatColor.GREEN + this.moneyFormat.format(totalCost)
        ));
        if (ticketsToBuy > 1) {
            lore.add(ChatColor.GRAY + "at " + ChatColor.GREEN + ChatColor.BOLD.toString() + "$" + ChatColor.GREEN + this.moneyFormat.format(this.currentJackpot.getTicketPrice()) + "/ea");
        }
        for (int i = 0; i < 4; ++i) {
            inventory.setItem(i, ItemUtils.createItem(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + ChatColor.BOLD.toString() + "Confirm Purchase", lore));
        }
        return inventory;
    }

    public void startNewJackpot() {
        Bukkit.getLogger().info("(CosmicJackpot) Starting new jackpot" + (this.currentJackpot == null ? "!" : ", previous pot: $" + this.currentJackpot.getTotalWinnings()));
        this.currentJackpot = new Jackpot(this.defaultDrawTime);
    }

    public void loadFileData(JavaPlugin plugin) {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }
        plugin.saveDefaultConfig();
        Jackpot.TAX = plugin.getConfig().getDouble("tax", 0.1);
        this.taxesCollected = plugin.getConfig().getLong("taxesCollected");
        this.defaultDrawTime = plugin.getConfig().getInt("drawTime", 14400);
        this.historyFile = this.getOrCreate(HISTORY_FILE);
        this.jackpotHistory = this.readJson(this.historyFile, this.jackpotHistoryToken);
        if (this.jackpotHistory == null) {
            this.jackpotHistory = new HashMap<>();
        }
        this.currentJackpotFile = this.getOrCreate(CURRENT_JACKPOT_FILE);
        this.currentJackpot = this.readJson(this.currentJackpotFile, this.jackpotToken);
        if (this.currentJackpot == null) {
            this.startNewJackpot();
        } else {
            Bukkit.getLogger().info("Loaded previously running Jackpot drawing: " + this.currentJackpot);
        }
        this.notificationToggleFile = this.getOrCreate(NOTIFICATION_TOGGLES_FILE);
        this.toggledNotifications = this.readJson(this.notificationToggleFile, this.togglesToken);
        if (this.toggledNotifications == null) {
            this.toggledNotifications = new HashSet<>();
        }
        Bukkit.getLogger().info("Loaded " + this.jackpotHistory.size() + " entries into the Jackpot History list.");
    }

    private File getOrCreate(String name) {
        File file = new File(CosmicJackpot.get().getDataFolder(), name);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
        }
        catch (IOException ex) {
            throw new IllegalStateException("Could not create " + name, ex);
        }
        return file;
    }

    public void saveFileData() {
        this.writeJson(this.historyFile, this.jackpotHistory);
        this.writeJson(this.currentJackpotFile, this.currentJackpot);
        this.writeJson(this.notificationToggleFile, this.toggledNotifications);
    }

    private <T> T readJson(File file, Type type) {
        try (Reader reader = new FileReader(file)) {
            return this.gson.fromJson(reader, type);
        }
        catch (IOException ex) {
            throw new IllegalStateException("Could not read " + file.getName(), ex);
        }
    }

    private void writeJson(File file, Object object) {
        try (Writer writer = new FileWriter(file)) {
            this.gson.toJson(object, writer);
        }
        catch (IOException ex) {
            throw new IllegalStateException("Could not save " + file.getName(), ex);
        }
    }

    public void saveWinnings() {
        CosmicJackpot.get().getConfig().set("taxesCollected", this.taxesCollected);
        CosmicJackpot.get().saveConfig();
    }

    public List<JackpotHistory> getSortedJackpotHistory() {
        if (this.lastListUpdate == 0L || System.currentTimeMillis() - this.lastListUpdate >= CACHE_REFRESH_MILLIS) {
            this.lastListUpdate = System.currentTimeMillis();
            this.sortedHistory = this.jackpotHistory.values().stream()
                    .filter(hist -> hist.getJackpotWins() > 0.0 && hist.getJackpotWinnings() > 0L)
                    .sorted()
                    .toList();
        }
        return this.sortedHistory;
    }

    public Set<UUID> getToggledNotifications() {
        return this.toggledNotifications;
    }

    public Map<UUID, JackpotHistory> getJackpotHistory() {
        return this.jackpotHistory;
    }

    public DecimalFormat getMoneyFormat() {
        return this.moneyFormat;
    }

    public Jackpot getCurrentJackpot() {
        return this.currentJackpot;
    }

    public void setDefaultDrawTime(int defaultDrawTime) {
        this.defaultDrawTime = defaultDrawTime;
    }

    private boolean shouldAnnounce(int secondsLeft) {
        for (int announcementSecond : ANNOUNCEMENT_SECONDS) {
            if (announcementSecond == secondsLeft) {
                return true;
            }
        }
        return false;
    }

    private void broadcastCountdown(int secondsLeft) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (this.toggledNotifications.contains(player.getUniqueId())) {
                continue;
            }

            if (secondsLeft >= 60) {
                player.sendMessage("");
            }
            player.sendMessage(ChatColor.AQUA + ChatColor.BOLD.toString() + "(!) " + ChatColor.AQUA
                    + "Cosmic Jackpot (" + ChatColor.AQUA + ChatColor.BOLD + "$" + ChatColor.AQUA
                    + this.moneyFormat.format(this.currentJackpot.getPlayerWinnings()) + ") drawing in "
                    + ChatColor.AQUA + ChatColor.BOLD + this.formatDrawTime(secondsLeft) + ChatColor.AQUA + "!");
            if (secondsLeft > 5) {
                player.sendMessage(ChatColor.GRAY + "Use " + ChatColor.LIGHT_PURPLE + "/jackpot buy" + ChatColor.GRAY + " to purchase a ticket!");
            }
            if (secondsLeft >= 60) {
                player.sendMessage("");
            }
        }
    }

    private String formatDrawTime(int secondsLeft) {
        return secondsLeft == 60 ? "60s" : TimeUtils.formatDifference(secondsLeft).trim();
    }

    private void broadcastWinner(PlayerInfo winner) {
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(ChatColor.GREEN + ChatColor.BOLD.toString() + "(!) " + winner.getUsername()
                + ChatColor.GREEN + " has won the " + ChatColor.GREEN + ChatColor.UNDERLINE + "/jackpot"
                + ChatColor.GREEN + " and received ");
        Bukkit.broadcastMessage(ChatColor.DARK_GREEN + ChatColor.BOLD.toString() + "$" + ChatColor.DARK_GREEN
                + this.moneyFormat.format(this.currentJackpot.getPlayerWinnings()) + ChatColor.GREEN
                + "! They purchased " + ChatColor.GREEN
                + this.moneyFormat.format(this.currentJackpot.getTicketsPurchased(winner.getUuid()))
                + ChatColor.GREEN + " (" + this.moneyFormat.format(this.currentJackpot.getPercentHolder(winner.getUuid()))
                + "%) ticket(s) out of the ");
        Bukkit.broadcastMessage(ChatColor.GREEN.toString() + this.moneyFormat.format(this.currentJackpot.getTicketsSold())
                + ChatColor.GREEN + " ticket(s) sold!");
        Bukkit.broadcastMessage("");
    }
}
