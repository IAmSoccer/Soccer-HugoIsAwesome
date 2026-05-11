package at.iamsoccer.soccerisawesome.cauldronconcrete;

import at.iamsoccer.soccerisawesome.AbstractModule;
import at.iamsoccer.soccerisawesome.SoccerIsAwesomePlugin;
import co.aikar.commands.PaperCommandManager;
import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.keys.tags.BlockTypeTagKeys;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.BlockType;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.ItemType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CauldronConcreteModule extends AbstractModule implements Listener {
    private final HashSet<ItemType> powderedConcretes = new HashSet<>();
    private final HashMap<ItemType, ItemType> powderedConcreteResults = new HashMap<>();

    private final ConcurrentHashMap<UUID, Item> concretePowderItems = new ConcurrentHashMap<>();

    public CauldronConcreteModule(SoccerIsAwesomePlugin plugin) {
        super(plugin, "CauldronConcrete");
    }

    @Override
    public boolean enable(PaperCommandManager commandManager) {
        if (!super.enable(commandManager)) return false;
        RegistryAccess.registryAccess().getRegistry(RegistryKey.BLOCK).getTagValues(BlockTypeTagKeys.CONCRETE_POWDER).stream().map(BlockType::getItemType).forEach(powderedConcretes::add);
        for (ItemType powderedConcrete : powderedConcretes) {
            var powderedName = powderedConcrete.key().value();
            var concreteName = powderedName.substring(0, powderedName.lastIndexOf('_'));

            var concrete = RegistryAccess.registryAccess().getRegistry(RegistryKey.ITEM).get(Key.key(concreteName));
            if (concrete != null) {
                powderedConcreteResults.put(powderedConcrete, concrete);
            }
        }
        Bukkit.getScheduler().runTaskTimer(plugin, this::run, 0, 0);
        this.iterator = concretePowderItems.values().iterator();
        return true;
    }


    private Iterator<Item> iterator;

    private void run() {
        var size = concretePowderItems.size();
        if (size == 0) return;
        // limit it to 20 items per tick
        var iterations = Math.min(size, 20);
//        Bukkit.broadcast(Component.text("Tick " + Bukkit.getCurrentTick()));
        for (var i = 0; i < iterations; i++) {
            if (!iterator.hasNext()) iterator = concretePowderItems.values().iterator();
            var item = iterator.next();
            if(!item.isValid()) {
                iterator.remove();
                continue;
            }
            var cauldron = item.getLocation().getBlock();
            if (cauldron.getType() != Material.WATER_CAULDRON) return;
            var toType = powderedConcreteResults.get(item.getItemStack().getType().asItemType());
            if (toType == null) return;
            // set item to non-powdered concrete
            item.setItemStack(item.getItemStack().withType(toType.asMaterial()));
            // update cauldron water level
            var state = cauldron.getState(false);
            var levelled = (Levelled) state.getBlockData();
            if(levelled.getLevel() > 1) {
                levelled.setLevel(levelled.getLevel() - 1);
                state.setBlockData(levelled);
                state.update();
            } else {
                cauldron.setType(Material.CAULDRON);
            }
            // play feedback
            item.getLocation().getWorld().playSound(item.getLocation(), Sound.BLOCK_WET_GRASS_HIT, SoundCategory.BLOCKS, 0.75f, 0.65f);
            var loc = item.getLocation();
            loc.setY(loc.getBlockY() + 0.5 + levelled.getLevel() * 0.15);
            item.getLocation().getWorld().spawnParticle(Particle.WHITE_SMOKE,loc , 5, 0.1, 0, 0.1, 0);
            iterator.remove();
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onConcreteAddedToWorld(EntityAddToWorldEvent event) {
        if (!(event.getEntity() instanceof Item item)) return;
        if (!powderedConcreteResults.containsKey(item.getItemStack().getType().asItemType())) return;
        concretePowderItems.put(item.getUniqueId(), item);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onConcreteRemovedFromWorld(EntityRemoveFromWorldEvent event) {
        if (!(event.getEntity() instanceof Item item)) return;
        if (!powderedConcreteResults.containsKey(item.getItemStack().getType().asItemType())) return;
        concretePowderItems.remove(item.getUniqueId());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onConcreteMerge(ItemMergeEvent event) {
        var item = event.getEntity();
        if (!powderedConcreteResults.containsKey(item.getItemStack().getType().asItemType())) return;
        concretePowderItems.remove(item.getUniqueId());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onConcreteMerge(EntityPickupItemEvent event) {
        var item = event.getItem();
        if (!powderedConcreteResults.containsKey(item.getItemStack().getType().asItemType())) return;
        concretePowderItems.remove(item.getUniqueId());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onConcreteMerge(InventoryPickupItemEvent event) {
        var item = event.getItem();
        if (!powderedConcreteResults.containsKey(item.getItemStack().getType().asItemType())) return;
        concretePowderItems.remove(item.getUniqueId());
    }
}
