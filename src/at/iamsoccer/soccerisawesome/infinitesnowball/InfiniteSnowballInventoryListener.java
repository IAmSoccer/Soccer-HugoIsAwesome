package at.iamsoccer.soccerisawesome.infinitesnowball;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;

import java.util.EnumSet;

public class InfiniteSnowballInventoryListener implements Listener {
    private static final EnumSet<InventoryType> BLOCKED_INVENTORIES = EnumSet.of(
            InventoryType.ANVIL, InventoryType.DISPENSER, InventoryType.GRINDSTONE
    );

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInvClick(InventoryClickEvent e) {
        if(!BLOCKED_INVENTORIES.contains(e.getInventory().getType())) return;
        if (!InfiniteSnowballInteractListener.isMagicSnowball(e.getCurrentItem())) return;

        e.getWhoClicked().sendMessage("Gonna stop you right there chief.");
        e.getWhoClicked().sendMessage("You don't wanna lose that sucker by mistake.");

        e.setCancelled(true);
    }
}