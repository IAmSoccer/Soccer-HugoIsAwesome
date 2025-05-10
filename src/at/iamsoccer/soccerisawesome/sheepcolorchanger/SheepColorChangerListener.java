package at.iamsoccer.soccerisawesome.sheepcolorchanger;

import org.bukkit.DyeColor;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Lets a person change a sheep to a random color when the following are true:
 * + clicking a sheep with his main hand
 * + the hand is empty
 * + is sneaking
 * + has the permssion "sheepcolorchanger.use"
 */
public class SheepColorChangerListener implements Listener {
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSheepInteract(PlayerInteractEntityEvent event) {
        // only check sheep interactions and when the main hand was used.
        if(!(event.getRightClicked() instanceof Sheep sheep) || event.getHand() != EquipmentSlot.HAND || !event.getPlayer().isSneaking()) return;
        // check the permission
        if (!event.getPlayer().hasPermission("sheepcolorchanger.use")) return;
        // get the main hand item
        final ItemStack itemInHand = event.getPlayer().getInventory().getItem(event.getHand());
        // check if the item is air
        if(!itemInHand.getType().isAir()) return;
        // set the sheep color to a random color.
        sheep.setColor(DyeColor.values()[ThreadLocalRandom.current().nextInt(DyeColor.values().length)]);
    }
}
