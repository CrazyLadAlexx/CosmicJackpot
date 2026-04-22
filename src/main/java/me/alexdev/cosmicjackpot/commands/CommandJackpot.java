package me.alexdev.cosmicjackpot.commands;

import java.util.LinkedList;
import me.alexdev.cosmicjackpot.CosmicJackpot;
import me.alexdev.cosmicjackpot.struct.Jackpot;
import me.alexdev.cosmicjackpot.struct.JackpotHistory;
import me.alexdev.cosmicjackpot.struct.JackpotManager;
import me.alexdev.cosmicjackpot.utils.TimeUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

public class CommandJackpot implements CommandExecutor {
    private static final int MAX_TICKETS_PER_PURCHASE = 100000;
    private static final int TOP_ENTRIES_PER_PAGE = 15;
    private static final int MAX_TOP_PAGES = 5;
    private static final String TICKET_METADATA_KEY = "jackpotTicket";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        JackpotManager manager = CosmicJackpot.get().getJackpotManager();

        if (args.length == 0) {
            return this.showCurrentJackpot(sender, manager);
        }

        String subCommand = args[0].toLowerCase();
        if (subCommand.equals("buy")) {
            return this.handleBuy(sender, args, manager);
        }
        if (subCommand.equals("top")) {
            return this.showTopWinners(sender, args, manager);
        }
        if (subCommand.equals("stats")) {
            return this.showStats(sender, manager);
        }
        if (subCommand.equals("toggle") || subCommand.equals("notifications")) {
            return this.toggleNotifications(sender, manager);
        }
        if (subCommand.equals("draw") && sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Forcing Jackpot winner!");
            manager.drawWinner();
            return true;
        }
        if (subCommand.equals("count") && sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "Forcing countdown.");
            manager.getCurrentJackpot().setSecondsLeft(61);
            return true;
        }
        if (subCommand.equals("settime") && sender.isOp()) {
            return this.setDrawTime(sender, args, manager);
        }

        this.showHelp(sender);
        return true;
    }

    private boolean handleBuy(CommandSender sender, String[] args, JackpotManager manager) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can buy jackpot tickets.");
            return true;
        }
        if (args.length > 2) {
            this.showHelp(sender);
            return true;
        }

        Player player = (Player) sender;
        Jackpot current = manager.getCurrentJackpot();
        String amount = args.length == 2 ? args[1] : "1";
        if (!isPositiveInteger(amount)) {
            sender.sendMessage(ChatColor.RED + "Please enter a valid amount of tickets to purchase!");
            return true;
        }

        int tickets = Integer.parseInt(amount);
        if (tickets < 1 || tickets > MAX_TICKETS_PER_PURCHASE) {
            sender.sendMessage(ChatColor.RED + "Please enter a ticket amount between 1 and 100,000!");
            return true;
        }

        double totalPrice = tickets * current.getTicketPrice();
        if (!CosmicJackpot.economy.has(player, totalPrice)) {
            sender.sendMessage(ChatColor.RED + "You cannot afford the total price of $" + manager.getMoneyFormat().format(totalPrice) + " for " + tickets + " tickets!");
            return true;
        }

        sender.sendMessage("");
        sender.sendMessage(ChatColor.GREEN + "Confirm purchase of " + ChatColor.AQUA + ChatColor.BOLD + ChatColor.UNDERLINE + tickets + ChatColor.GREEN + " Jackpot Ticket(s)");
        sender.sendMessage(ChatColor.GREEN + "for " + ChatColor.AQUA + ChatColor.BOLD.toString() + "$" + ChatColor.AQUA + manager.getMoneyFormat().format(totalPrice) + (tickets > 1 ? ChatColor.GREEN + " at " + ChatColor.AQUA + ChatColor.BOLD + "$" + ChatColor.AQUA + manager.getMoneyFormat().format(current.getTicketPrice()) + "/ea" : "!"));
        sender.sendMessage("");
        player.setMetadata(TICKET_METADATA_KEY, new FixedMetadataValue(CosmicJackpot.get(), tickets));
        player.openInventory(manager.getJackpotConfirmMenu(tickets, totalPrice));
        return true;
    }

    private boolean showTopWinners(CommandSender sender, String[] args, JackpotManager manager) {
        LinkedList<JackpotHistory> topWinners = manager.getSortedJackpotHistory();
        if (topWinners.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "There are no recorded Jackpot winners!");
            return true;
        }

        int totalPages = (int) Math.min(MAX_TOP_PAGES, Math.ceil((double) topWinners.size() / TOP_ENTRIES_PER_PAGE));
        int page = args.length == 2 && isPositiveInteger(args[1]) ? Integer.parseInt(args[1]) : 1;
        page = Math.max(1, Math.min(page, totalPages));

        sender.sendMessage("");
        sender.sendMessage(ChatColor.LIGHT_PURPLE + ChatColor.BOLD.toString() + "Top Jackpot Winners (" + ChatColor.AQUA + ChatColor.BOLD + page + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + "/" + ChatColor.AQUA + ChatColor.BOLD + totalPages + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + ")");
        int startIndex = (page - 1) * TOP_ENTRIES_PER_PAGE;
        int endIndex = Math.min(startIndex + TOP_ENTRIES_PER_PAGE, topWinners.size());
        for (int i = startIndex; i < endIndex; ++i) {
            JackpotHistory history = topWinners.get(i);
            int position = i + 1;
            sender.sendMessage(" " + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + position + ". " + ChatColor.WHITE + history.getName()
                    + ChatColor.LIGHT_PURPLE + " - " + ChatColor.AQUA + ChatColor.BOLD + (int) history.getJackpotWins()
                    + ChatColor.AQUA + " Wins" + ChatColor.AQUA + ChatColor.BOLD + " (" + ChatColor.LIGHT_PURPLE
                    + ChatColor.BOLD + "$" + ChatColor.LIGHT_PURPLE + manager.getMoneyFormat().format(history.getJackpotWinnings())
                    + ChatColor.AQUA + ChatColor.BOLD + ")");
        }
        return true;
    }

    private boolean showStats(CommandSender sender, JackpotManager manager) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players have jackpot stats.");
            return true;
        }

        Player player = (Player) sender;
        JackpotHistory history = manager.getJackpotHistory().getOrDefault(player.getUniqueId(), new JackpotHistory(player.getName()));
        sender.sendMessage("");
        sender.sendMessage(ChatColor.LIGHT_PURPLE + ChatColor.BOLD.toString() + "Cosmic Jackpot Stats");
        sender.sendMessage(ChatColor.AQUA + "Total Winnings: " + this.money(manager, history.getJackpotWinnings()));
        sender.sendMessage(ChatColor.AQUA + "Total Tickets Purchased: " + ChatColor.LIGHT_PURPLE + manager.getMoneyFormat().format(history.getTicketsPurchased()));
        sender.sendMessage(ChatColor.AQUA + "Total Jackpot Wins: " + ChatColor.LIGHT_PURPLE + manager.getMoneyFormat().format(history.getJackpotWins()));
        sender.sendMessage("");
        return true;
    }

    private boolean toggleNotifications(CommandSender sender, JackpotManager manager) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can toggle jackpot notifications.");
            return true;
        }

        Player player = (Player) sender;
        if (manager.getToggledNotifications().remove(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "Jackpot Notifications " + ChatColor.GREEN + ChatColor.BOLD + "ENABLED");
        } else {
            manager.getToggledNotifications().add(player.getUniqueId());
            player.sendMessage(ChatColor.YELLOW + "Jackpot Notifications " + ChatColor.RED + ChatColor.BOLD + "DISABLED");
            player.sendMessage(ChatColor.GRAY + "You will no longer see Jackpot countdown notifications!");
        }
        return true;
    }

    private boolean showCurrentJackpot(CommandSender sender, JackpotManager manager) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can view personal jackpot odds.");
            return true;
        }

        Player player = (Player) sender;
        Jackpot current = manager.getCurrentJackpot();
        if (current == null) {
            sender.sendMessage(ChatColor.RED + "It seems there is no jackpot currently running!");
            return true;
        }

        sender.sendMessage("");
        sender.sendMessage(ChatColor.LIGHT_PURPLE + ChatColor.BOLD.toString() + "Cosmic Jackpot");
        sender.sendMessage(ChatColor.AQUA + ChatColor.BOLD.toString() + "  Jackpot Value: " + this.money(manager, current.getPlayerWinnings()) + ChatColor.GRAY + " (-" + (int) (Jackpot.TAX * 100.0) + "% tax)");
        sender.sendMessage(ChatColor.AQUA + ChatColor.BOLD.toString() + "  Tickets Sold: " + ChatColor.YELLOW + manager.getMoneyFormat().format(current.getTicketsSold()));
        sender.sendMessage(ChatColor.AQUA + ChatColor.BOLD.toString() + "  Your Tickets: " + ChatColor.GREEN + current.getTicketsPurchased(player) + ChatColor.GRAY + " (" + manager.getMoneyFormat().format(current.getPercentHolder(player)) + "%)");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.AQUA + ChatColor.BOLD.toString() + "(!) " + ChatColor.AQUA + "Next Winner in " + ChatColor.AQUA + ChatColor.UNDERLINE + TimeUtils.formatDifference(current.getSecondsLeft()));
        sender.sendMessage("");
        return true;
    }

    private boolean setDrawTime(CommandSender sender, String[] args, JackpotManager manager) {
        if (args.length != 2 || !isPositiveInteger(args[1])) {
            sender.sendMessage(ChatColor.RED + "Usage: /jackpot settime <seconds>");
            return true;
        }

        int time = Integer.parseInt(args[1]);
        manager.setDefaultDrawTime(time);
        CosmicJackpot.get().getConfig().set("drawTime", time);
        CosmicJackpot.get().saveConfig();
        sender.sendMessage(ChatColor.RED + "Draw time set to " + time + " seconds.");
        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(ChatColor.AQUA + ChatColor.BOLD.toString() + "Jackpot Commands");
        sender.sendMessage(ChatColor.WHITE + "/jackpot");
        sender.sendMessage(ChatColor.GRAY + "View current Jackpot information.");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.WHITE + "/jackpot buy <x>");
        sender.sendMessage(ChatColor.GRAY + "Buy X tickets for the current Jackpot.");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.WHITE + "/jackpot top");
        sender.sendMessage(ChatColor.GRAY + "View top Jackpot Winners!");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.WHITE + "/jackpot stats");
        sender.sendMessage(ChatColor.GRAY + "View your Jackpot stats!");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.WHITE + "/jackpot notifications");
        sender.sendMessage(ChatColor.GRAY + "Toggle Jackpot countdown notifications!");
        sender.sendMessage("");
    }

    private String money(JackpotManager manager, double value) {
        return ChatColor.LIGHT_PURPLE.toString() + ChatColor.BOLD + "$" + ChatColor.LIGHT_PURPLE + manager.getMoneyFormat().format(value);
    }

    private static boolean isPositiveInteger(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); ++i) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
