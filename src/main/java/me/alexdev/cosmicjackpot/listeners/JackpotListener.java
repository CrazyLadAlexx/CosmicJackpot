package me.alexdev.cosmicjackpot.listeners;

import me.alexdev.cosmicjackpot.CosmicJackpot;
import me.alexdev.cosmicjackpot.struct.Jackpot;
import me.alexdev.cosmicjackpot.struct.JackpotHistory;
import me.alexdev.cosmicjackpot.struct.JackpotManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
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

public class JackpotListener implements Listener {
    private static final String TICKET_METADATA_KEY = "jackpotTicket";

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        event.getPlayer().removeMetadata(TICKET_METADATA_KEY, CosmicJackpot.get());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equalsIgnoreCase(JackpotManager.CONFIRM_MENU_TITLE)) {
            return;
        }

        event.setCancelled(true);
        event.setResult(Event.Result.DENY);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !this.isConfirmButton(clicked.getType())) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        Integer ticketsToPurchase = this.consumePendingTickets(player);
        if (ticketsToPurchase == null) {
            player.closeInventory();
            return;
        }

        player.closeInventory();
        if (clicked.getType() == Material.LIME_STAINED_GLASS_PANE) {
            this.confirmPurchase(player, ticketsToPurchase);
        } else {
            this.sendCancelled(player);
        }
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onPlayerTalk(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Integer ticketsToPurchase = this.consumePendingTickets(player);
        if (ticketsToPurchase == null || ticketsToPurchase < 0) {
            return;
        }

        event.setCancelled(true);
        if (!ChatColor.stripColor(event.getMessage()).equalsIgnoreCase("confirm")) {
            this.sendCancelled(player);
            return;
        }

        event.setMessage("");
        this.confirmPurchase(player, ticketsToPurchase);
    }

    private void confirmPurchase(Player player, int ticketsToPurchase) {
        JackpotManager manager = CosmicJackpot.get().getJackpotManager();
        Jackpot current = manager.getCurrentJackpot();
        double price = ticketsToPurchase * current.getTicketPrice();
        if (!CosmicJackpot.economy.has(player, price)) {
            player.sendMessage(ChatColor.RED + "You no longer have $" + manager.getMoneyFormat().format(price) + " required to purchase " + ticketsToPurchase + " tickets!");
            return;
        }

        current.purchaseTicket(player, ticketsToPurchase);
        player.sendMessage(ChatColor.GREEN + "You have purchased " + ticketsToPurchase + " ticket" + (ticketsToPurchase > 1 ? "s" : "") + " for " + ChatColor.GREEN + ChatColor.BOLD.toString() + "$" + ChatColor.GREEN + manager.getMoneyFormat().format(price) + "!");
        player.sendMessage(ChatColor.GRAY + "Use " + ChatColor.GRAY + ChatColor.UNDERLINE.toString() + "/jackpot" + ChatColor.GRAY + " to view current Jackpot information!");
        player.sendMessage("");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.4f);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "contest add " + player.getUniqueId() + " lotteryTickets " + ticketsToPurchase);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "contest add " + player.getUniqueId() + " lotteryMoneySpent " + price);
        JackpotHistory history = manager.getJackpotHistory().computeIfAbsent(player.getUniqueId(), uuid -> new JackpotHistory(player.getName()));
        history.setTicketsPurchased(history.getTicketsPurchased() + (double)ticketsToPurchase);
    }

    private boolean isConfirmButton(Material material) {
        return material == Material.LIME_STAINED_GLASS_PANE || material == Material.RED_STAINED_GLASS_PANE;
    }

    private Integer consumePendingTickets(Player player) {
        if (!player.hasMetadata(TICKET_METADATA_KEY) || player.getMetadata(TICKET_METADATA_KEY).isEmpty()) {
            return null;
        }

        MetadataValue metadata = player.getMetadata(TICKET_METADATA_KEY).get(0);
        player.removeMetadata(TICKET_METADATA_KEY, CosmicJackpot.get());
        return metadata.asInt();
    }

    private void sendCancelled(Player player) {
        player.sendMessage(ChatColor.RED + ChatColor.BOLD.toString() + "(!) " + ChatColor.RED + "Jackpot Ticket Purchase cancelled.");
    }
}
