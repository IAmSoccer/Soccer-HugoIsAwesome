package at.iamsoccer.soccerisawesome.prettycoloredglass;

import at.iamsoccer.soccerisawesome.AbstractModule;
import at.iamsoccer.soccerisawesome.SoccerIsAwesomePlugin;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Objects;

public class PrettyColoredGlassListener extends AbstractModule implements Listener {
    private static final String SOME_PERMISSION = "sia.colorglass.use";
    private static final EnumSet<Material> DYE_MATERIALS = EnumSet.noneOf(Material.class);
    private static final EnumMap<Material, Material> GLASS_MATERIALS = new EnumMap<>(Material.class);
    private static final EnumMap<Material, EnumMap<Material, Material>> GLASS_COLOR_MAP = new EnumMap<>(Material.class);

    static {
        // get all dyes
        Arrays.stream(Material.values()).filter(m -> m.name().endsWith("_DYE")).forEach(DYE_MATERIALS::add);
        // put dye and glass together
        for (var color : DYE_MATERIALS) {
            var colorName = color.name().substring(0, color.name().lastIndexOf('_'));
            Arrays.stream(Material.values()).filter(m -> m.name().endsWith("GLASS") || m.name().endsWith("GLASS_PANE")).filter(m -> m.name().startsWith(colorName)).forEach(m -> GLASS_MATERIALS.put(m, color));
        }
        // add non colored glass
        GLASS_MATERIALS.put(Material.GLASS, null);
        GLASS_MATERIALS.put(Material.GLASS_PANE, null);
        // link glass to other glass colors
        final var glassColorMap = new EnumMap<Material, Material>(Material.class);
        final var glassPaneColorMap = new EnumMap<Material, Material>(Material.class);
        for (var glass : GLASS_MATERIALS.keySet()) {
            if (glass.name().endsWith("PANE")) {
                GLASS_COLOR_MAP.put(glass, glassPaneColorMap);
                if (Material.GLASS_PANE.equals(glass)) continue;
                glassPaneColorMap.put(GLASS_MATERIALS.get(glass), glass);
            } else {
                GLASS_COLOR_MAP.put(glass, glassColorMap);
                if (Material.GLASS.equals(glass)) continue;
                glassColorMap.put(GLASS_MATERIALS.get(glass), glass);
            }
        }
    }

    public PrettyColoredGlassListener(SoccerIsAwesomePlugin plugin) {
        super(plugin, "PrettyColoredGlass");
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
// priority Highest so it will either be already cancelled when in a town for example or other stuff
    public void onInteract(PlayerInteractEvent event) {
        if (!event.hasBlock() || !event.hasItem()) return; // check if this is an item interacting with a block
        final Block clickedBlock = event.getClickedBlock();
        Objects.requireNonNull(clickedBlock, "Clicked Block should never be null since we check for it in the event");
        final Material clickedMaterial = clickedBlock.getType();
        if (!GLASS_MATERIALS.containsKey(clickedMaterial)) return; // check if the block is glass
        final Material handMaterial = event.getMaterial();
        if (!DYE_MATERIALS.contains(handMaterial)) return; // check if the item is dye
        final Player player = event.getPlayer();
        if (!player.hasPermission(SOME_PERMISSION))
            return; // check if the player has the permission before doing more expensive code
        if (handMaterial.equals(GLASS_MATERIALS.get(clickedMaterial)))
            return; // dont do anything if its already the correct color
        final Material newBlockMaterial = GLASS_COLOR_MAP.get(clickedMaterial).get(handMaterial);
        // check if he is allowed to break and place a block here
        final BlockBreakEvent breakEvent = new BlockBreakEvent(clickedBlock, player);
        breakEvent.setDropItems(false);
        breakEvent.callEvent();
        if (breakEvent.isCancelled()) return; // cant break blocks here
        // no need to check block place event, since if he can break it he can very probably also build here, else its just a weird area, no?
        // reduce item amount unless they in creative
        if (player.getGameMode() != GameMode.CREATIVE) {
            final ItemStack item = event.getItem();
            Objects.requireNonNull(item, "Item should never be null since we check for it in the event");
            item.setAmount(item.getAmount() - 1);
        }
        // set new color
        final var oldData = clickedBlock.getBlockData();
        clickedBlock.setType(newBlockMaterial);
        if (clickedBlock.getBlockData() instanceof MultipleFacing facing && oldData instanceof MultipleFacing oldFacing) {
            for (BlockFace face : oldFacing.getFaces()) {
                facing.setFace(face, true);
            }
            ((Waterlogged) facing).setWaterlogged(((Waterlogged) oldFacing).isWaterlogged());
            clickedBlock.setBlockData(facing);
        }
        // play some sound
        clickedBlock.getWorld().playSound(clickedBlock.getLocation(), org.bukkit.Sound.ENTITY_ITEM_FRAME_ADD_ITEM, SoundCategory.BLOCKS, 1, 1);
    }
}
