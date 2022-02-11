package fr.mathys;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Random;
import java.util.Stack;
import java.util.UUID;

public class DeathSwap extends JavaPlugin implements CommandExecutor, Listener {
    private final int TIME = 300;
    private BukkitTask task;
    private Stack<UUID> playingPlayers = new Stack<>();
    private boolean paused;

    public void onEnable() {
        for (String command : getDescription().getCommands().keySet())
            getServer().getPluginCommand(command).setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);
    }

    private void start() {
        start(TIME);
    }

    private void start(int time) {
        this.paused = false;
        this

                .task = (new BukkitRunnable() {
            int timer = time;

            public void run() {
                if (DeathSwap.this.paused)
                    return;
                if (this.timer <= 10 && this.timer != 0)
                    Bukkit.broadcastMessage(ChatColor.RED + "" + ChatColor.BOLD + "Swapping in " + this.timer + ((this.timer == 1) ? " second!" : " seconds!"));
                if (this.timer == 0) {
                    for (Pair pair : Pair.getPairs()) {
                        Player player1 = Bukkit.getPlayer(pair.getPlayer1());
                        Player player2 = Bukkit.getPlayer(pair.getPlayer2());
                        assert player1 != null;
                        Location location = player1.getLocation();
                        assert player2 != null;
                        player1.teleport(player2);
                        player2.teleport(location);
                    }

                    Stack<Player> filteredPlayers = new Stack<>();
                    Stack<Location> locations = new Stack<>();
                    for(Player player : Bukkit.getOnlinePlayers()) {
                        Pair pair = Pair.getTwin(player);
                        if(pair != null) continue;
                        if(!playingPlayers.contains(player.getUniqueId())) continue;
                        filteredPlayers.push(player);
                    }

                    if(filteredPlayers.size() >= 2 || locations.size() >= 2) {
                        Stack<Player> randomizedPlayers = new Stack<>();
                        Random random = new Random();
                        Player tempPlayer;
                        int randomNumber;
                        int i;
                        int loopSize = filteredPlayers.size();
                        for(i = 0; i < loopSize;) {
                            randomNumber = random.nextInt(filteredPlayers.size());
                            tempPlayer = filteredPlayers.get(randomNumber);
                            filteredPlayers.remove(tempPlayer);
                            randomizedPlayers.push(tempPlayer);
                            locations.push(tempPlayer.getLocation());
                            i++;
                        }

                        Location firstLocation = locations.peek();
                        locations.pop();
                        locations.add(0, firstLocation);

                        i = 0;
                        for(Player player : randomizedPlayers) {
                            Location location = locations.get(i);
                            player.teleport(location);
                            i++;
                        }
                    }

                    this.timer = TIME;
                } else {
                    this.timer--;
                }
            }
        }).runTaskTimer(this, 0L, 20L);
    }

    private void stop() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("deathswap")) {
            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("start")) {
                    if (this.task != null) {
                        sender.sendMessage(ChatColor.GREEN + "Death swap is already started.");
                        return false;
                    }
                    start();
                    sender.sendMessage(ChatColor.GREEN + "Death swap started.");
                    return true;
                }
                if (args[0].equalsIgnoreCase("stop")) {
                    stop();
                    sender.sendMessage(ChatColor.RED + "Death swap stopped.");
                    return true;
                }
                if (args[0].equalsIgnoreCase("play") || args[0].equalsIgnoreCase("join")) {
                    UUID id = ((Player) sender).getUniqueId();
                    if(!playingPlayers.contains(id)) {
                        playingPlayers.add(id);
                        sender.sendMessage(ChatColor.GREEN + "Successfully joined the game");
                    }
                    else {
                        playingPlayers.remove(id);
                        sender.sendMessage(ChatColor.GREEN + "Successfully left the game");
                    }
                    return true;
                }
                return false;
            }
            if (args.length == 2) {
                if (args[0].equalsIgnoreCase("remove")) {
                    Player player = Bukkit.getPlayer(args[1]);
                    if (player == null) {
                        sender.sendMessage(ChatColor.RED + "Player " + args[1] + " not found.");
                        return false;
                    }
                    Pair pair = Pair.getTwin(player);
                    if (pair == null) {
                        sender.sendMessage(ChatColor.RED + player.getName() + " is not paired.");
                        return false;
                    }
                    pair.remove();
                    sender.sendMessage(ChatColor.GREEN + player.getName() + " removed from pair.");
                    return true;
                }
            } else if (args.length == 3) {
                if (args[0].equalsIgnoreCase("pair")) {
                    Player player1 = Bukkit.getPlayer(args[1]);
                    Player player2 = Bukkit.getPlayer(args[2]);
                    if (player1 == null) {
                        sender.sendMessage(ChatColor.RED + "Player " + args[1] + " not found.");
                        return false;
                    }
                    if (player2 == null) {
                        sender.sendMessage(ChatColor.RED + "Player " + args[2] + " not found.");
                        return false;
                    }
                    if (player1.equals(player2)) {
                        sender.sendMessage(ChatColor.RED + "A player can not be paired to themselves!");
                        return false;
                    }
                    if (Pair.getTwin(player1) != null) {
                        sender.sendMessage(ChatColor.RED + player1.getName() + " is already paired.");
                        return false;
                    }
                    if (Pair.getTwin(player2) != null) {
                        sender.sendMessage(ChatColor.RED + player2.getName() + " is already paired.");
                        return false;
                    }
                    new Pair(player1.getUniqueId(), player2.getUniqueId());
                    sender.sendMessage(ChatColor.GREEN + player1.getName() + " and " + player2.getName() + " are now paired!");
                    return true;
                }
            }
            sender.sendMessage(ChatColor.RED + "Invalid usage. Please use:");
            sender.sendMessage(ChatColor.RED + "/deathswap join");
            sender.sendMessage(ChatColor.RED + "/deathswap pair <player 1> <player 2>");
            sender.sendMessage(ChatColor.RED + "/deathswap remove <player>");
            sender.sendMessage(ChatColor.RED + "/deathswap start");
            sender.sendMessage(ChatColor.RED + "/deathswap stop");
            return false;
        }
        return false;
    }

    @EventHandler
    public void onPlayerQuitEvent(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Pair pair = Pair.getTwin(player);
        if (pair != null) {
            pair.remove();
            Player player1 = Bukkit.getPlayer(pair.getPlayer2());
            if (player1 == null)
                return;
            if (player1.equals(player))
                player1 = Bukkit.getPlayer(pair.getPlayer1());
            if (player1 == null)
                return;
            player1.sendMessage(ChatColor.YELLOW + player.getName() + " left. You are no longer paired.");
        }
    }
}
