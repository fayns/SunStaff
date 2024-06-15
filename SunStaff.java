package com.sunstaff;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SunStaff extends JavaPlugin implements Listener {

    private FileConfiguration config;
    private Map<UUID, ExaminationTask> examinationTasks = new HashMap<>();
    private boolean pluginEnabled = false;

    @Override
    public void onEnable() {
        // Load configuration on plugin enable
        saveDefaultConfig();
        config = getConfig();

        // Register /staff and /staffreload commands
        getCommand("staff").setExecutor(this);

        // Register event handler
        getServer().getPluginManager().registerEvents(this, this);

        // Log a message to the console when the plugin is enabled
        pluginEnabled = true; // Указание того, что плагин включен
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        // Проверка, включен ли плагин
        if (pluginEnabled) {
            // Вывод сообщений в консоль после полного запуска сервера
            getLogger().info(ChatColor.translateAlternateColorCodes('&',"&x&0&8&7&B&F&B&lS&x&0&7&8&9&F&C&lu&x&0&6&9&8&F&C&ln&x&0&5&A&6&F&D&lS&x&0&3&B&5&F&D&lt&x&0&2&C&3&F&E&la&x&0&1&D&2&F&E&lf&x&0&0&E&0&F&F&lf &aplugin has been enabled!"));
            getLogger().info(ChatColor.translateAlternateColorCodes('&',"&6Author: &x&F&B&0&8&0&8&lF&x&D&E&0&A&0&A&la&x&C&2&0&D&0&D&ly&x&A&5&0&F&0&F&ln&x&8&8&1&1&1&1&ls"));
            getLogger().info(ChatColor.translateAlternateColorCodes('&',"Plugin &x&0&8&7&B&F&B&lS&x&0&7&8&9&F&C&lu&x&0&6&9&8&F&C&ln&x&0&5&A&6&F&D&lS&x&0&3&B&5&F&D&lt&x&0&2&C&3&F&E&la&x&0&1&D&2&F&E&lf&x&0&0&E&0&F&F&lf &6on SpigotMC"));
        }
    }



    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("staff")) {
            if (args.length == 0) {
                openStaffMenu(sender);
            } else {
                switch (args[0].toLowerCase()) {
                    case "help":
                        showHelp(sender);
                        break;
                    case "list":
                        sendStaffList(sender);
                        break;
                    case "off":
                        handleStaffStatus(sender, "off");
                        break;
                    case "on":
                        handleStaffStatus(sender, "on");
                        break;
                    case "reload":
                        reloadConfig(sender);
                        break;
                    case "examination":
                        if (args.length == 3) {
                            startExamination(sender, args[1], args[2]);
                        } else {
                            sender.sendMessage(ChatColor.RED + "Usage: /staff examination <player> <time>");
                        }
                        break;
                    case "offexamination":
                        if (args.length == 2) {
                            stopExamination(sender, args[1]);
                        } else {
                            sender.sendMessage(ChatColor.RED + "Usage: /staff offexamination <player>");
                        }
                        break;
                    case "addtime":
                        if (args.length == 4 && args[1].equalsIgnoreCase("examination")) {
                            addExaminationTime(sender, args[2], args[3]);
                        } else {
                            sender.sendMessage(ChatColor.RED + "Usage: /staff addtime examination <player> <time>");
                        }
                        break;
                    default:
                        sender.sendMessage(ChatColor.RED + "Unknown command. Use /staff help for a list of commands.");
                        break;
                }
            }
            return true;
        }
        return false;
    }

    private void openStaffMenu(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            // Load menu settings from config
            String menuTitle = config.getString("menu.title", "&0Staff Menu");
            int menuSize = config.getInt("menu.size", 9);

            // Create menu
            Inventory menu = Bukkit.createInventory(null, menuSize, ChatColor.translateAlternateColorCodes('&', menuTitle));

            // Add staff members to the menu
            List<String> staffList = config.getStringList("staff.list");
            for (String staffMember : staffList) {
                ItemStack item = createItem(staffMember);
                menu.addItem(item);
            }

            // Open the menu for the player
            player.openInventory(menu);
        } else {
            sender.sendMessage("This command can only be used in-game.");
        }
    }

    private void sendStaffList(CommandSender sender) {
        // Send staff list to chat
        List<String> staffList = config.getStringList("staff.list");
        for (String staffMember : staffList) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', staffMember));
        }
    }

    private ItemStack createItem(String staffMember) {
        // Convert & symbols to color codes and create an item for each staff member
        staffMember = ChatColor.translateAlternateColorCodes('&', staffMember);
        ItemStack item = new ItemStack(Material.PAPER, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(staffMember);
        item.setItemMeta(meta);
        return item;
    }

    private void handleStaffStatus(CommandSender sender, String status) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            String message = config.getString("messages.staff_" + status, "&cYou do not have permission to use this command.");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            Bukkit.getServer().broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&x&F&F&6&7&0&0▍&r &fModerator &6" + player.getName() + " &fis now &c" + (status.equals("on") ? "Active" : "Inactive")));
        } else {
            sender.sendMessage("This command can only be used in-game.");
        }
    }

    private void reloadConfig(CommandSender sender) {
        if (sender.hasPermission("staff.reload")) {
            reloadConfig();
            config = getConfig();
            String reloadMessage = config.getString("config-messages.reload-message", "Plugin configuration reloaded.");
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', reloadMessage));
        } else {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
        }
    }

    private void startExamination(CommandSender sender, String playerName, String timeStr) {
        if (sender.hasPermission("staff.examination")) {
            Player target = Bukkit.getPlayer(playerName);
            if (target != null) {
                int time = parseTime(timeStr);
                if (time == -1) {
                    sender.sendMessage(ChatColor.RED + "Invalid time format.");
                    return;
                }

                ExaminationTask task = new ExaminationTask(this, target, time);
                task.start();
                examinationTasks.put(target.getUniqueId(), task);

                String examinationMessage = config.getString("messages.examination_start", "&cYou are being checked for cheats.");
                target.sendMessage(ChatColor.translateAlternateColorCodes('&', examinationMessage));
                sender.sendMessage(ChatColor.GREEN + "Cheat examination started for " + playerName + " for " + time + " seconds.");

                // Display title message
                String titleMessage = config.getString("messages.examination_title", "&cCheat Examination");
                String subtitleMessage = config.getString("messages.examination_subtitle", "&fPlease wait...");
                target.sendTitle(ChatColor.translateAlternateColorCodes('&', titleMessage),
                        ChatColor.translateAlternateColorCodes('&', subtitleMessage),
                        10, 70, 20);

                target.teleport(Bukkit.getWorlds().get(0).getSpawnLocation()); // Teleport to spawn
                target.setGameMode(GameMode.SURVIVAL);
                target.setAllowFlight(false);
                target.setFlying(false);
            } else {
                sender.sendMessage(ChatColor.RED + "Player not found.");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
        }
    }

    private int parseTime(String timeStr) {
        Pattern pattern = Pattern.compile("(\\d+)([smh])");
        Matcher matcher = pattern.matcher(timeStr);
        if (matcher.matches()) {
            int time = Integer.parseInt(matcher.group(1));
            String suffix = matcher.group(2).toLowerCase();
            switch (suffix) {
                case "s":
                    return time;
                case "m":
                    return time * 60;
                case "h":
                    return time * 3600;
                default:
                    return -1;
            }
        }
        return -1;
    }


    private void stopExamination(CommandSender sender, String playerName) {
        if (sender.hasPermission("staff.offexamination")) {
            Player target = Bukkit.getPlayer(playerName);
            if (target != null && examinationTasks.containsKey(target.getUniqueId())) {
                ExaminationTask task = examinationTasks.remove(target.getUniqueId());
                if (task != null) {
                    task.cancel();
                }
                target.sendMessage(ChatColor.GREEN + "You have passed the examination.");
                sender.sendMessage(ChatColor.GREEN + "Cheat examination stopped for " + playerName + ".");
            } else {
                sender.sendMessage(ChatColor.RED + "Player is not currently under examination.");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
        }
    }

    private void addExaminationTime(CommandSender sender, String playerName, String timeStr) {
        if (sender.hasPermission("staff.addtime.examination")) {
            Player target = Bukkit.getPlayer(playerName);
            if (target != null && examinationTasks.containsKey(target.getUniqueId())) {
                int extraTime = parseTime(timeStr);
                if (extraTime == -1) {
                    sender.sendMessage(ChatColor.RED + "Invalid time format.");
                    return;
                }

                ExaminationTask task = examinationTasks.get(target.getUniqueId());
                task.addTime(extraTime);

                sender.sendMessage(ChatColor.GREEN + "Added " + extraTime + " seconds to examination for " + playerName + ".");
                target.sendMessage(ChatColor.YELLOW + "Your examination time has been increased by " + extraTime + " seconds.");
            } else {
                sender.sendMessage(ChatColor.RED + "Player is not currently under examination.");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
        }
    }


    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "==== Staff Commands ====");
        sender.sendMessage(ChatColor.YELLOW + "/staff" + ChatColor.WHITE + " - Open staff menu");
        sender.sendMessage(ChatColor.YELLOW + "/staff help" + ChatColor.WHITE + " - Show this help message");
        sender.sendMessage(ChatColor.YELLOW + "/staff list" + ChatColor.WHITE + " - List all staff members");
        sender.sendMessage(ChatColor.YELLOW + "/staff on" + ChatColor.WHITE + " - Set yourself as active staff");
        sender.sendMessage(ChatColor.YELLOW + "/staff off" + ChatColor.WHITE + " - Set yourself as inactive staff");
        sender.sendMessage(ChatColor.YELLOW + "/staff reload" + ChatColor.WHITE + " - Reload the plugin configuration");
        sender.sendMessage(ChatColor.YELLOW + "/staff examination <player> <time>" + ChatColor.WHITE + " - Start cheat examination for a player");
        sender.sendMessage(ChatColor.YELLOW + "/staff offexamination <player>" + ChatColor.WHITE + " - Stop cheat examination for a player");
        sender.sendMessage(ChatColor.YELLOW + "/staff addtime examination <player> <time>" + ChatColor.WHITE + " - Add time to a player's cheat examination");
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (examinationTasks.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (examinationTasks.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (examinationTasks.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private class ExaminationTask extends BukkitRunnable {

        private final JavaPlugin plugin;
        private final Player player;
        private int timeLeft;

        public ExaminationTask(JavaPlugin plugin, Player player, int time) {
            this.plugin = plugin;
            this.player = player;
            this.timeLeft = time;
        }

        public void start() {
            this.runTaskTimer(plugin, 0, 20); // Run task every second
        }

        public void addTime(int extraTime) {
            this.timeLeft += extraTime;
        }

        @Override
        public void run() {
            if (timeLeft <= 0) {
                // Ban the player and stop the examination
                String banMessage = config.getString("messages.examination_fail", "&cYou failed the cheat examination.");
                Bukkit.getBanList(BanList.Type.NAME).addBan(player.getName(), ChatColor.translateAlternateColorCodes('&', banMessage), null, null);
                player.kickPlayer(ChatColor.translateAlternateColorCodes('&', banMessage));
                stopExamination(plugin.getServer().getConsoleSender(), player.getName());
                this.cancel();
            } else {
                // Check if the player is banned during the examination
                if (Bukkit.getBanList(BanList.Type.NAME).isBanned(player.getName())) {
                    stopExamination(plugin.getServer().getConsoleSender(), player.getName());
                    this.cancel();
                }
                timeLeft--;
            }
        }
    }
}
