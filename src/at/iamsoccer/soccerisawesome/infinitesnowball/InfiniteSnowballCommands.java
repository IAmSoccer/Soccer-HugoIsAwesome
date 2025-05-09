package at.iamsoccer.soccerisawesome.infinitesnowball;

import at.iamsoccer.soccerisawesome.SoccerIsAwesomePlugin;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Snowman;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;


public class InfiniteSnowballCommands extends BaseCommand {
    private final SoccerIsAwesomePlugin plugin;

    public InfiniteSnowballCommands(SoccerIsAwesomePlugin plugin) {
        this.plugin = plugin;
    }

    public static final NamespacedKey magicSnowball = new NamespacedKey(SoccerIsAwesomePlugin.getPlugin(SoccerIsAwesomePlugin.class), "isMagicSnowball");

    @CommandAlias("infinitesnowball")
    @CommandPermission("infinitesnowball.give")
    @Description("Gives an infinite snowball to the user.")
    public void infiniteSnowballGive(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            sender.sendMessage("Too many/too few arguments.");
            return;
        }
        final Player target;
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("From console, include a player name.");
                return;
            }
            target = player;
        } else {
            target = SoccerIsAwesomePlugin.getPlugin(SoccerIsAwesomePlugin.class).getServer().getPlayer(args[0]);
        }
        if (target == null) {
            sender.sendMessage("Player could not be found.");
            return;
        }
        ItemStack item = new ItemStack(Material.SNOWBALL, 1);
        item.addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 1);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Magic Snowball").color(NamedTextColor.AQUA));
        meta.getPersistentDataContainer().set(magicSnowball, PersistentDataType.INTEGER, ThreadLocalRandom.current().nextInt());
        item.setItemMeta(meta);
        target.getInventory().addItem(item);
    }

    @CommandAlias("snowman")
    @CommandPermission("snowmancannon.shoot")
    @Description("Launches a fabulous snowman towards the cursor.")
    public void snowmanCannon(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("User command only");
            return;
        }
        final Snowman snowman = (Snowman) player.getWorld().spawnEntity(player.getEyeLocation(), EntityType.SNOWMAN);
        snowman.setDerp((ThreadLocalRandom.current().nextInt(16) == 7));
        snowman.setVelocity(player.getEyeLocation().getDirection().clone().multiply(3));
        new BukkitRunnable() {
            final Vector vec = snowman.getEyeLocation().getDirection().setY(0).normalize();

            int i = 0;

            public void run() {
                snowman.getWorld().spawnParticle(Particle.SNOW_SHOVEL, snowman.getLocation(), 8, 1.0D, 1.0D, 1.0D, 0.2D);
                if (++this.i > 15) {
                    Snowball b = (Snowball) snowman.getWorld().spawnEntity(snowman.getEyeLocation(), EntityType.SNOWBALL);
                    b.setVelocity(this.vec);
                    this.vec.rotateAroundY(12.0D);
                }
                if (this.i > 38) {
                    Location l = snowman.getLocation();
                    l.getWorld().spawnParticle(Particle.CLOUD, l, 30, 1.0D, 2.0D, 1.0D, 0.1D);
                    l.getWorld().spawnParticle(Particle.SNOW_SHOVEL, l, 30, 1.0D, 2.0D, 1.0D, 0.05D);
                    snowman.remove();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}

