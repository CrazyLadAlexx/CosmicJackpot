package me.alexdev.cosmicjackpot.listeners;

import me.alexdev.cosmicjackpot.CosmicJackpot;
import me.alexdev.cosmicjackpot.struct.Jackpot;
import me.alexdev.cosmicjackpot.struct.JackpotHistory;
import me.alexdev.cosmicjackpot.struct.JackpotManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

public class JackpotListener
implements Listener {
    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        event.getPlayer().removeMetadata("jackpotTicket", (Plugin)CosmicJackpot.get());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getName().equalsIgnoreCase("Confirm Ticket Purchase")) {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() != Material.STAINED_GLASS_PANE) {
                return;
            }
            Player player = (Player)event.getWhoClicked();
            if (!player.hasMetadata("jackpotTicket") || player.getMetadata("jackpotTicket").size() <= 0) {
                player.closeInventory();
                return;
            }
            int ticketsToPurchase = ((MetadataValue)player.getMetadata("jackpotTicket").get(0)).asInt();
            player.removeMetadata("jackpotTicket", (Plugin)CosmicJackpot.get());
            short data = clicked.getData().getData();
            if (data == DyeColor.LIME.getWoolData()) {
                player.closeInventory();
                this.confirmPurchase(player, ticketsToPurchase);
            } else if (data == DyeColor.RED.getWoolData()) {
                player.closeInventory();
                player.sendMessage(ChatColor.RED + ChatColor.BOLD.toString() + "(!) " + ChatColor.RED + "Jackpot Ticket Purchase cancelled.");
            }
        }
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onPlayerTalk(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (player.hasMetadata("jackpotTicket") && player.getMetadata("jackpotTicket").size() > 0) {
            int ticketsToPurchase = ((MetadataValue)player.getMetadata("jackpotTicket").get(0)).asInt();
            player.removeMetadata("jackpotTicket", (Plugin)CosmicJackpot.get());
            if (ticketsToPurchase < 0) {
                return;
            }
            event.setCancelled(true);
            if (!ChatColor.stripColor((String)event.getMessage()).equalsIgnoreCase("confirm")) {
                player.sendMessage(ChatColor.RED + ChatColor.BOLD.toString() + "(!) " + ChatColor.RED + "Jackpot Ticket Purchase cancelled.");
                return;
            }
            event.setMessage("");
            this.confirmPurchase(player, ticketsToPurchase);
        }
    }

    private void confirmPurchase(Player player, int ticketsToPurchase) {
        JackpotManager manager = CosmicJackpot.get().getJackpotManager();
        Jackpot current = manager.getCurrentJackpot();
        double price = ticketsToPurchase * current.getTicketPrice();
        if (!CosmicJackpot.economy.has((OfflinePlayer)player, price)) {
            player.sendMessage(ChatColor.RED + "You no longer have $" + manager.getMoneyFormat().format(price) + " required to purchase " + ticketsToPurchase + " tickets!");
            return;
        }
        int adjustedTickets = ticketsToPurchase;
        current.purchaseTicket(player, ticketsToPurchase, adjustedTickets);
        player.sendMessage(ChatColor.GREEN + "You have purchased " + ticketsToPurchase + " ticket" + (ticketsToPurchase > 1 ? "s" : "") + " for " + ChatColor.GREEN + ChatColor.BOLD.toString() + "$" + ChatColor.GREEN + manager.getMoneyFormat().format(price) + "!");
        player.sendMessage(ChatColor.GRAY + "Use " + ChatColor.GRAY + ChatColor.UNDERLINE.toString() + "/jackpot" + ChatColor.GRAY + " to view current Jackpot information!");
        player.sendMessage("");
        player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.4f);
        Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), (String)("contest add " + player.getUniqueId() + " lotteryTickets " + ticketsToPurchase));
        Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), (String)("contest add " + player.getUniqueId() + " lotteryMoneySpent " + price));
        JackpotHistory history = manager.getJackpotHistory().computeIfAbsent(player.getUniqueId(), e -> new JackpotHistory(player.getName()));
        history.setTicketsPurchased(history.getTicketsPurchased() + (double)ticketsToPurchase);
    }
}
