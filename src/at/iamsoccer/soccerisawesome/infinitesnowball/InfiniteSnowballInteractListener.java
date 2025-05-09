package at.iamsoccer.soccerisawesome.infinitesnowball;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;

public class InfiniteSnowballInteractListener implements Listener {
    @EventHandler(priority = EventPriority.LOW)
    public void onInvChange(PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        Action action = event.getAction();
        ItemStack item = event.getItem();
        // just for backwards parity set offhand and mainhand amount to 1 if they are magic snowballs
        limitMagicSnowballTo1(player.getInventory().getItemInMainHand());
        limitMagicSnowballTo1(player.getInventory().getItemInOffHand());

        if (!action.isRightClick()) return;
        if (!isMagicSnowball(item)) return;

        if (player.hasPermission("infinitesnowball.use")) {
            player.launchProjectile(Snowball.class);
            player.getWorld().playSound(player, org.bukkit.Sound.ENTITY_SNOWBALL_THROW, 0.5F, 0.4F / (ThreadLocalRandom.current().nextFloat() * 0.4F + 0.8F));
        } else {
            player.sendMessage("You aren't allowed to use the infinite snowball.");
        }
        event.setCancelled(true);
    }

    private void limitMagicSnowballTo1(final @Nullable ItemStack itemStack) {
        if (isMagicSnowball(itemStack) && itemStack.getAmount() != 1) itemStack.setAmount(1);
    }

    public static boolean isMagicSnowball(final @Nullable ItemStack itemStack) {
        return itemStack != null
                && itemStack.getType().equals(Material.SNOWBALL)
                && itemStack.getItemMeta().getPersistentDataContainer().has(InfiniteSnowballCommands.magicSnowball);
    }
}