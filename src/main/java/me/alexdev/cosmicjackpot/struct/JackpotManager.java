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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import me.alexdev.cosmicjackpot.CosmicJackpot;
import me.alexdev.cosmicjackpot.struct.Jackpot;
import me.alexdev.cosmicjackpot.struct.JackpotHistory;
import me.alexdev.cosmicjackpot.struct.PlayerInfo;
import me.alexdev.cosmicjackpot.utils.ItemUtils;
import me.alexdev.cosmicjackpot.utils.TimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

public class JackpotManager {
    private File topFile;
    private File currentJackpotFile;
    private File notificationToggleFile;
    private Gson gson = new GsonBuilder().setPrettyPrinting().enableComplexMapKeySerialization().create();
    private Type jackpotHistoryToken = new TypeToken<Map<UUID, JackpotHistory>>(){}.getType();
    private Type jackpotToken = new TypeToken<Jackpot>(){}.getType();
    private Type togglesToken = new TypeToken<HashSet<UUID>>(){}.getType();
    private Set<UUID> toggledNofications = new HashSet<UUID>();
    private Map<UUID, JackpotHistory> jackpotHistory;
    private DecimalFormat moneyFormat = new DecimalFormat("#,###.##");
    private Jackpot currentJackpot;
    private long taxesCollected;
    private int defaultDrawTime = (int)TimeUnit.HOURS.toSeconds(2L);
    private LinkedList<JackpotHistory> sortedHistory = new LinkedList();
    private long lastListUpdate;

    public void onJackpotTick() {
        int secondsLeft = this.currentJackpot.getSecondsLeft();
        if (secondsLeft > 0) {
            if (secondsLeft == 300 || secondsLeft == 60 || secondsLeft == 30 || secondsLeft == 10 || secondsLeft <= 5 && secondsLeft > 0) {
                for (Player pl : Bukkit.getOnlinePlayers()) {
                    if (this.toggledNofications.contains(pl.getUniqueId())) continue;
                    if (secondsLeft >= 60) {
                        pl.sendMessage("");
                    }
                    pl.sendMessage(ChatColor.AQUA + ChatColor.BOLD.toString() + "(!) " + ChatColor.AQUA + "Astro Jackpot (" + ChatColor.AQUA + ChatColor.BOLD + "$" + ChatColor.AQUA + this.getMoneyFormat().format(this.currentJackpot.getPlayerWinnings()) + ") drawing in " + ChatColor.AQUA + ChatColor.BOLD.toString() + (secondsLeft == 60 ? "60s" : TimeUtils.formatDifference(secondsLeft).trim()) + ChatColor.AQUA + "!");
                    if (secondsLeft > 5) {
                        pl.sendMessage(ChatColor.GRAY + "Use " + ChatColor.LIGHT_PURPLE + "/jackpot buy" + ChatColor.GRAY + " to purchase a ticket!");
                    }
                    if (secondsLeft < 60) continue;
                    pl.sendMessage("");
                }
            }
            this.currentJackpot.setSecondsLeft(secondsLeft - 1);
        } else {
            this.drawWinner();
        }
    }

    public void drawWinner() {
        PlayerInfo winner = this.currentJackpot.getRandomWinner();
        if (winner == null) {
            Bukkit.broadcastMessage((String)(ChatColor.RED.toString() + ChatColor.BOLD + "(!) " + ChatColor.RED + "No Jackpot winner available due to no ticket sales!"));
            this.startNewJackpot();
            return;
        }
        Bukkit.broadcastMessage((String)"");
        Bukkit.broadcastMessage((String)(ChatColor.GREEN + ChatColor.BOLD.toString() + "(!) " + winner.getUsername() + ChatColor.GREEN + " has won the " + ChatColor.GREEN + ChatColor.UNDERLINE + "/jackpot" + ChatColor.GREEN + " and received "));
        Bukkit.broadcastMessage((String)(ChatColor.DARK_GREEN + ChatColor.BOLD.toString() + "$" + ChatColor.DARK_GREEN + this.moneyFormat.format(this.currentJackpot.getPlayerWinnings()) + ChatColor.GREEN + "! They purchased " + ChatColor.GREEN + this.moneyFormat.format(this.currentJackpot.getTicketsPurchased(winner.getUuid())) + ChatColor.GREEN + " (" + this.moneyFormat.format(this.currentJackpot.getPercentHolder(winner.getUuid())) + "%) ticket(s) out of the "));
        Bukkit.broadcastMessage((String)(ChatColor.GREEN.toString() + this.moneyFormat.format(this.currentJackpot.getTicketsSold()) + ChatColor.GREEN + " ticket(s) sold!"));
        Bukkit.broadcastMessage((String)"");
        if (this.currentJackpot != null) {
            this.currentJackpot.getEntries().forEach((info, num) -> {
                if (winner.getUuid().equals(info.getUuid())) {
                    return;
                }
                double percent = this.currentJackpot.getPercentHolder(info.getUuid());
            });
        }
        double winnings = this.currentJackpot.getPlayerWinnings();
        double taxesCollected = this.currentJackpot.getTotalWinnings() - this.currentJackpot.getPlayerWinnings();
        this.startNewJackpot();
        JackpotHistory history = this.jackpotHistory.computeIfAbsent(winner.getUuid(), e -> new JackpotHistory(winner.getUsername()));
        history.setJackpotWinnings(history.getJackpotWinnings() + (long)winnings);
        history.setJackpotWins(history.getJackpotWins() + 1.0);
        OfflinePlayer pl = Bukkit.getPlayer((UUID)winner.getUuid());
        if (pl == null) {
            pl = Bukkit.getOfflinePlayer((UUID)winner.getUuid());
        } else {
            ((Player)pl).sendMessage(ChatColor.GREEN.toString() + ChatColor.BOLD + "+ $" + ChatColor.GREEN + this.moneyFormat.format(winnings));
        }
        CosmicJackpot.economy.depositPlayer((OfflinePlayer)pl, winnings);
        Bukkit.getLogger().info("Depositing $" + winnings + " into " + pl.getName() + "'s player (" + (pl instanceof Player ? "Online" : "Offline") + ").");
        this.taxesCollected = (long)((double)this.taxesCollected + taxesCollected);
        this.saveWinnings();
        Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), (String)("contest add " + winner.getUuid() + " lotteryMoneyWin " + winnings));
    }

    public Inventory getJackpotConfirmMenu(int ticketsToBuy, double totalCost) {
        Inventory inv = Bukkit.createInventory(null, (int)9, (String)"Confirm Ticket Purchase");
        for (int i = 5; i < 9; ++i) {
            inv.setItem(i, ItemUtils.createItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + ChatColor.BOLD.toString() + "Cancel Purchase", Arrays.asList(ChatColor.GRAY + "Click to " + ChatColor.RED + "cancel" + ChatColor.GRAY + " ticket purchase.")));
        }
        ArrayList lore = new ArrayList(Arrays.asList(ChatColor.GRAY + "Click to " + ChatColor.GREEN + "confirm" + ChatColor.GRAY + " purchase of", ChatColor.GREEN + this.moneyFormat.format(ticketsToBuy) + ChatColor.GRAY + " ticket(s) for " + ChatColor.GREEN + ChatColor.BOLD + "$" + ChatColor.GREEN + this.moneyFormat.format(totalCost)));
        if (ticketsToBuy > 1) {
            lore.add(ChatColor.GRAY + "at " + ChatColor.GREEN + ChatColor.BOLD.toString() + "$" + ChatColor.GREEN + this.moneyFormat.format(this.currentJackpot.getTicketPrice()) + "/ea");
        }
        for (int i = 0; i < 4; ++i) {
            inv.setItem(i, ItemUtils.createItem(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + ChatColor.BOLD.toString() + "Confirm Purchase", lore));
        }
        return inv;
    }

    public void startNewJackpot() {
        Bukkit.getLogger().info("(CosmicJackpot) Starting new JackPot" + (this.currentJackpot == null ? "!" : ", previous Jackpot: $" + this.currentJackpot.getTotalWinnings()));
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
        this.topFile = this.getOrCreate("jackpotHistory.json");
        this.jackpotHistory = (Map)this.readJson(this.topFile, this.jackpotHistoryToken);
        if (this.jackpotHistory == null) {
            this.jackpotHistory = new HashMap<UUID, JackpotHistory>();
        }
        this.currentJackpotFile = this.getOrCreate("currentJackpot.json");
        this.currentJackpot = (Jackpot)this.readJson(this.currentJackpotFile, this.jackpotToken);
        if (this.currentJackpot == null) {
            this.startNewJackpot();
        } else {
            Bukkit.getLogger().info("Loaded previously running Jackpot drawing: " + this.currentJackpot);
        }
        this.notificationToggleFile = this.getOrCreate("notificationToggles.json");
        this.toggledNofications = (Set)this.readJson(this.notificationToggleFile, this.togglesToken);
        if (this.toggledNofications == null) {
            this.toggledNofications = new HashSet<UUID>();
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
        this.writeJson(this.topFile, this.jackpotHistory);
        this.writeJson(this.currentJackpotFile, this.currentJackpot);
        this.writeJson(this.notificationToggleFile, this.toggledNofications);
    }

    private Object readJson(File file, Type type) {
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
        CosmicJackpot.get().getConfig().set("taxesCollected", (Object)this.taxesCollected);
        CosmicJackpot.get().saveConfig();
    }

    public LinkedList<JackpotHistory> getSortedJackpotHistory() {
        if (this.lastListUpdate == 0L || System.currentTimeMillis() - this.lastListUpdate >= 10000L) {
            this.lastListUpdate = System.currentTimeMillis();
            this.sortedHistory = new LinkedList();
            for (JackpotHistory hist : this.jackpotHistory.values()) {
                if (hist.getJackpotWins() <= 0.0 || hist.getJackpotWinnings() <= 0L) continue;
                this.sortedHistory.add(hist);
            }
            Collections.sort(this.sortedHistory);
        }
        return this.sortedHistory;
    }

    public Set<UUID> getToggledNofications() {
        return this.toggledNofications;
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
}
