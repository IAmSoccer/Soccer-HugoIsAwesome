package at.iamsoccer.soccerisawesome.blockrotator;

import at.iamsoccer.soccerisawesome.AbstractModule;
import at.iamsoccer.soccerisawesome.SoccerIsAwesomePlugin;
import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.type.Piston;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.Material;

import java.util.*;

public class BlockRotatorListener extends AbstractModule implements Listener {

    public BlockRotatorListener(SoccerIsAwesomePlugin plugin) {
        super(plugin, "BlockRotator");
    }

    private static final String PERMISSION_USE = "sia.blockrotator.use";
    private static final Material ROTATION_TOOL = Material.BLAZE_ROD;

    private static final Set<Material> BANNED_BLOCKS = Set.of(
        Material.WHITE_BED,
        Material.ORANGE_BED,
        Material.MAGENTA_BED,
        Material.LIGHT_BLUE_BED,
        Material.YELLOW_BED,
        Material.LIME_BED,
        Material.PINK_BED,
        Material.GRAY_BED,
        Material.LIGHT_GRAY_BED,
        Material.CYAN_BED,
        Material.PURPLE_BED,
        Material.BLUE_BED,
        Material.BROWN_BED,
        Material.GREEN_BED,
        Material.RED_BED,
        Material.BLACK_BED,
        Material.CHEST,
        Material.TRAPPED_CHEST,
        Material.ENDER_CHEST,
        Material.PISTON_HEAD
    );

    // Precompute BlockFace → next BlockFace for O(1) lookup
    private static final Map<BlockFace, BlockFace> NEXT_ROTATION = createRotationMap();

    private static Map<BlockFace, BlockFace> createRotationMap() {
        BlockFace[] faces = {
            BlockFace.NORTH, BlockFace.NORTH_NORTH_EAST, BlockFace.NORTH_EAST, BlockFace.EAST_NORTH_EAST,
            BlockFace.EAST, BlockFace.EAST_SOUTH_EAST, BlockFace.SOUTH_EAST, BlockFace.SOUTH_SOUTH_EAST,
            BlockFace.SOUTH, BlockFace.SOUTH_SOUTH_WEST, BlockFace.SOUTH_WEST, BlockFace.WEST_SOUTH_WEST,
            BlockFace.WEST, BlockFace.WEST_NORTH_WEST, BlockFace.NORTH_WEST, BlockFace.NORTH_NORTH_WEST
        };
        Map<BlockFace, BlockFace> map = new EnumMap<>(BlockFace.class);
        for (int i = 0; i < faces.length; i++) {
            map.put(faces[i], faces[(i + 1) % faces.length]);
        }
        return Collections.unmodifiableMap(map);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK
            || event.getHand() != EquipmentSlot.HAND
            || !event.getPlayer().isSneaking()
            || event.getPlayer().getInventory().getItemInMainHand().getType() != ROTATION_TOOL
            || !event.getPlayer().hasPermission(PERMISSION_USE)) {
            return;
        }

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        if (BANNED_BLOCKS.contains(clicked.getType())) {
            event.getPlayer().sendMessage("§cYou cannot rotate this block.");
            return;
        }

        BlockData data = clicked.getBlockData();
        if ((data instanceof Piston piston) && piston.isExtended()) {
            event.getPlayer().sendMessage("§cYou cannot rotate an extended piston.");
            return;
        }

        // Protection check
        BlockBreakEvent breakEvent = new BlockBreakEvent(clicked, event.getPlayer());
        breakEvent.setDropItems(false);
        breakEvent.callEvent();
        if (breakEvent.isCancelled()) return;

        // Attempt rotation
        if (rotateBlock(clicked)) {
            event.setCancelled(true);
        }
    }

    private boolean rotateBlock(Block block) {
        BlockData data = block.getBlockData();

        // 1) Handle two‑block‑high (doors, beds, etc.)
        if (data instanceof Bisected) {
            return rotateBisected(block, (Bisected) data);
        }
        // 2) Single‑block Rotatable (signs, skulls…)
        else if (data instanceof Rotatable) {
            rotateRotatable((Rotatable) data);
            block.setBlockData(data, true);
            Bukkit.getScheduler().runTask(plugin, block::tick);

            return true;
        }
        // 3) Axis‑based (logs, pillars…)
        else if (data instanceof Orientable) {
            rotateOrientable((Orientable) data);
            block.setBlockData(data, true);
            Bukkit.getScheduler().runTask(plugin, block::tick);
            return true;
        }
        // 4) Face‑based (repeaters, pistons, etc.)
        else if (data instanceof Directional) {
            rotateDirectional((Directional) data);
            block.setBlockData(data, true);
            Bukkit.getScheduler().runTask(plugin, block::tick);
            return true;
        }

        return false;
    }

    private boolean rotateBisected(Block block, Bisected bisected) {
        // Determine the lower‑half block
        Block lower = bisected.getHalf() == Bisected.Half.TOP
            ? block.getRelative(BlockFace.DOWN)
            : block;
        BlockData lowerData = lower.getBlockData();

        // Only rotate if it's a Directional type
        if (!(lowerData instanceof Directional dir)) return false;
        rotateDirectional(dir);
        lower.setBlockData(dir, false);

        // Mirror the facing on the upper half
        Block upper = lower.getRelative(BlockFace.UP);
        BlockData upperData = upper.getBlockData();
        if (upperData instanceof Directional upperDir) {
            upperDir.setFacing(dir.getFacing());
            upper.setBlockData(upperDir, false);
        }
        return true;
    }

    private void rotateDirectional(Directional dir) {
        List<BlockFace> faces = new ArrayList<>(dir.getFaces());
        int idx = faces.indexOf(dir.getFacing());
        dir.setFacing(faces.get((idx + 1) % faces.size()));
    }

    private void rotateOrientable(Orientable ori) {
        Axis[] axes = Axis.values();
        int idx = ori.getAxis().ordinal();
        ori.setAxis(axes[(idx + 1) % axes.length]);
    }

    private void rotateRotatable(Rotatable rot) {
        BlockFace next = NEXT_ROTATION.getOrDefault(rot.getRotation(), BlockFace.NORTH);
        rot.setRotation(next);
    }
}
