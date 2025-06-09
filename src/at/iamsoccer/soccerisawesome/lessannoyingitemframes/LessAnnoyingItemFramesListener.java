package at.iamsoccer.soccerisawesome.lessannoyingitemframes;

import at.iamsoccer.soccerisawesome.AbstractModule;
import at.iamsoccer.soccerisawesome.SoccerIsAwesomePlugin;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.PaperCommandManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LessAnnoyingItemFramesListener extends AbstractModule implements Listener {

    private final Map<Player, Long> lastClick = new HashMap<>();

    private @Nullable BaseCommand command = null;

    public LessAnnoyingItemFramesListener(SoccerIsAwesomePlugin plugin) {
        super(plugin, "LessAnnoyingItemFrames");
    }

    @Override
    public boolean enable(PaperCommandManager commandManager) {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            warn("LuckPerms is not installed!");
            return false;
        }

        if (!super.enable(commandManager)) return false;

        command = new LessAnnoyingItemFramesCommands(plugin);
        commandManager.registerCommand(command);
        return true;
    }

    @Override
    public boolean disable(PaperCommandManager commandManager) {
        if (!super.disable(commandManager)) return false;

        if (command != null) {
            commandManager.unregisterCommand(command);
            command = null;
        }
        return true;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onItemFrameRotate(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (player.isSneaking() || (!player.hasPermission("sia.rotationtoggle.enabled") && !player.hasPermission("sia.clickthroughtoggle.enabled") && !player.hasPermission("sia.containertoggle.enabled"))) {
            return;
        }
        Entity entity = event.getRightClicked();
        if (entity instanceof ItemFrame frame) {
            if (frame.getItem().getType().equals(Material.AIR)) {
                return;
            }
            BlockFace attachedFace = frame.getAttachedFace();
            Block block = frame.getLocation().add(attachedFace.getDirection()).getBlock();
            if (player.hasPermission("sia.rotationtoggle.enabled")) {
                spamChecker(player, "sia rotationtoggle");
                event.setCancelled(true);
                if (block.getType().equals(Material.CHEST) && player.hasPermission("sia.clickthroughtoggle.enabled") || block.getType().equals(Material.BARREL) && player.hasPermission("sia.clickthroughtoggle.enabled")) {
                    BlockState state = block.getState();
                    blockTypeChecker(player, state);
                }
                return;
            }
            if (block.getType().equals(Material.CHEST) || block.getType().equals(Material.BARREL)) {
                if (player.hasPermission("sia.clickthroughtoggle.enabled")) {
                    BlockState state = block.getState();
                    blockTypeChecker(player, state);
                    event.setCancelled(true);
                } else if (player.hasPermission("sia.containertoggle.enabled")) {
                    spamChecker(player, "sia containertoggle");
                    event.setCancelled(true);
                }
            }
        }
    }

    // For those players who spam the item frame trying to get it to rotate not realizing they have the frame rotate toggled
    public void spamChecker(Player player, String type) {
        JavaPlugin plugin = JavaPlugin.getPlugin(SoccerIsAwesomePlugin.class);
        long now = System.currentTimeMillis();
        if (lastClick.containsKey(player) && now - lastClick.get(player) < 2000) {
            int clickCount = 0;
            List<MetadataValue> clickCountData = player.getMetadata("clickCount");
            if (!clickCountData.isEmpty()) {
                clickCount = clickCountData.get(0).asInt();
            }
            clickCount++;
            if (clickCount >= 5) {
                player.sendMessage("Toggle the item frame rotation feature, by running /" + type);
                clickCount = 0;
            }
            player.setMetadata("clickCount", new FixedMetadataValue(plugin, clickCount));
        }
        lastClick.put(player, now);
    }

    // Checks what inventory block it is (We just want to check Chests and Barrels) then handle each block properly
    public void blockTypeChecker(Player player, BlockState state) {
        if (state instanceof Chest chest) {
            Inventory inventory = chest.getInventory();
            player.openInventory(inventory);
        } else if (state instanceof Barrel barrel) {
            Inventory inventory = barrel.getInventory();
            player.openInventory(inventory);
        }
    }
}

