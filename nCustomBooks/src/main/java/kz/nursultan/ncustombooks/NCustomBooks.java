package kz.nursultan.ncustombooks;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class NCustomBooks extends JavaPlugin implements Listener, TabExecutor {

    private static final String CUSTOM_BOOK_NAME_BUR = ChatColor.BLUE + "Бур";
    private static final String CUSTOM_BOOK_DESCRIPTION_BUR = ChatColor.GRAY + "Позволяет кирке ломать блоки 3x3";

    private static final String CUSTOM_BOOK_NAME_MAGNIT = ChatColor.GREEN + "Магнит";
    private static final String CUSTOM_BOOK_DESCRIPTION_MAGNIT = ChatColor.GRAY + "Притягивает блоки в радиусе 10 блоков";

    private static final String PERMISSION_GIVE = "ncustombooks.give";

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("nCB").setExecutor(this);
        getCommand("nCB").setTabCompleter(this);
        saveDefaultConfig();
    }

    @Override
    public void onDisable() {
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || !item.getType().toString().contains("PICKAXE")) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.getLore() == null || !meta.getLore().contains(ChatColor.BLUE + "Бур")) return;

        Block centerBlock = event.getBlock();
        Location center = centerBlock.getLocation();

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;

                    Block block = center.getWorld().getBlockAt(center.getBlockX() + x, center.getBlockY() + y, center.getBlockZ() + z);
                    if (!block.getType().isAir() && block.getType().isSolid()) {
                        block.breakNaturally();
                        center.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION_LARGE, block.getLocation(), 1);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        ItemStack offHandItem = player.getInventory().getItemInOffHand();

        if (mainHandItem == null || mainHandItem.getType() != Material.ENCHANTED_BOOK) return;
        if (offHandItem == null || (!offHandItem.getType().toString().contains("PICKAXE") && !offHandItem.getType().toString().contains("SHOVEL") && !offHandItem.getType().toString().contains("HOE") && !offHandItem.getType().toString().contains("AXE"))) return;

        ItemMeta bookMeta = mainHandItem.getItemMeta();
        if (bookMeta == null || !bookMeta.hasDisplayName()) return;

        if (bookMeta.getDisplayName().equals(CUSTOM_BOOK_NAME_BUR)) {
            addCustomEnchantment(offHandItem, CUSTOM_BOOK_NAME_BUR);
            mainHandItem.setAmount(0);
        } else if (bookMeta.getDisplayName().equals(CUSTOM_BOOK_NAME_MAGNIT)) {
            addCustomEnchantment(offHandItem, CUSTOM_BOOK_NAME_MAGNIT);
            mainHandItem.setAmount(0);
        }
    }

    @EventHandler
    public void onPlayerHoldItem(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());

        if (item == null || item.getItemMeta() == null || item.getItemMeta().getLore() == null) return;

        if (item.getItemMeta().getLore().contains(ChatColor.GREEN + "Магнит")) {
            Location playerLocation = player.getLocation();

            player.getWorld().getEntitiesByClass(Item.class).stream()
                    .filter(entity -> entity.getLocation().distance(playerLocation) <= 10)
                    .forEach(entity -> {
                        entity.setVelocity(playerLocation.toVector().subtract(entity.getLocation().toVector()).normalize().multiply(0.5));
                    });

            for (int x = -10; x <= 10; x++) {
                for (int y = -10; y <= 10; y++) {
                    for (int z = -10; z <= 10; z++) {
                        Block block = playerLocation.getWorld().getBlockAt(playerLocation.clone().add(x, y, z));
                        if (!block.getType().isAir() && block.getType().isSolid()) {
                            Item droppedItem = playerLocation.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(block.getType()));
                            block.setType(Material.AIR);
                            droppedItem.setVelocity(playerLocation.toVector().subtract(droppedItem.getLocation().toVector()).normalize().multiply(0.5));
                        }
                    }
                }
            }
        }
    }

    private void addCustomEnchantment(ItemStack item, String enchantmentName) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        List<String> lore = meta.getLore();
        if (lore == null) lore = new java.util.ArrayList<>();

        lore.add(enchantmentName);
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    public ItemStack createCustomBook(String enchantmentName, String description) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = book.getItemMeta();
        meta.setDisplayName(enchantmentName);
        meta.setLore(Collections.singletonList(description));
        book.setItemMeta(meta);
        return book;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Команда доступна только игрокам.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission(PERMISSION_GIVE)) {
            player.sendMessage(ChatColor.RED + "У вас нет прав на использование этой команды.");
            return true;
        }

        if (args.length < 2 || !args[0].equalsIgnoreCase("give")) {
            player.sendMessage(ChatColor.RED + "Использование: /nCB give <название зачарования> <ник игрока>");
            return true;
        }

        String enchantmentName;
        String description;

        if (args[1].equalsIgnoreCase("byr")) {
            enchantmentName = CUSTOM_BOOK_NAME_BUR;
            description = CUSTOM_BOOK_DESCRIPTION_BUR;
        } else if (args[1].equalsIgnoreCase("magnit")) {
            enchantmentName = CUSTOM_BOOK_NAME_MAGNIT;
            description = CUSTOM_BOOK_DESCRIPTION_MAGNIT;
        } else {
            player.sendMessage(ChatColor.RED + "Неизвестное зачарование: " + args[1]);
            return true;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Игрок с ником " + args[2] + " не найден.");
            return true;
        }

        ItemStack book = createCustomBook(enchantmentName, description);
        target.getInventory().addItem(book);
        player.sendMessage(ChatColor.GREEN + "Вы выдали книгу с зачарованием " + enchantmentName + " игроку " + target.getName() + ".");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("give");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return Arrays.asList("byr", "magnit");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
