package at.iamsoccer.soccerisawesome.waypoints;

import at.hugob.plugin.library.config.MiniMsgLegacyHybridSerializer;
import at.hugob.plugin.library.config.YamlFileConfig;
import at.iamsoccer.soccerisawesome.AbstractModule;
import at.iamsoccer.soccerisawesome.SoccerIsAwesomePlugin;
import co.aikar.commands.PaperCommandManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class WaypointModule extends AbstractModule {
    private final WaypointsDialogFactory waypointsDialogFactory = new WaypointsDialogFactory();

    private YamlFileConfig config;

    public WaypointModule(final SoccerIsAwesomePlugin plugin) {
        super(plugin, "Waypoints");
    }

    @Override public boolean enable(PaperCommandManager commandManager) {
        if (!super.enable(commandManager)) return false;
        config = new YamlFileConfig(plugin, "waypoints-config.yml");
        return true;
    }

    @Override
    public void lifecycleHandler(Consumer<@NotNull LiteralCommandNode<CommandSourceStack>> register) {
        register.accept(waypointCommand());
    }

    @NotNull
    private LiteralCommandNode<CommandSourceStack> waypointCommand() {
        return Commands.literal("waypoint")
            .requires(css -> css.getSender().hasPermission("sia.waypoint") && css.getExecutor() instanceof Player)
            .executes(ctx -> {
                Player player = (Player) ctx.getSource().getExecutor();
                player.showDialog(waypointsDialogFactory.create(player));
                return Command.SINGLE_SUCCESS;
            })
            .build();
    }

    @Override
    public void reload() {
        waypointsDialogFactory.clearOptions();
        config.reload();
        var waypointOptions = config.getConfigurationSection("waypoint-options");
        for (String distanceString : waypointOptions.getKeys(false)) {
            final int distance;
            try {
                distance = Integer.parseInt(distanceString);
            } catch (NumberFormatException e) {
                warn("Waypoint distance: \"" + distanceString + "\" is not a number!");
                continue;
            }
            final String display = waypointOptions.getString(distanceString);
            waypointsDialogFactory.option(distance, display);
        }
        waypointsDialogFactory.title(MiniMsgLegacyHybridSerializer.INSTANCE.deserialize(config.getString("title")));
        waypointsDialogFactory.close(MiniMsgLegacyHybridSerializer.INSTANCE.deserialize(config.getString("close")));
        waypointsDialogFactory.confirm(MiniMsgLegacyHybridSerializer.INSTANCE.deserialize(config.getString("confirm")));
        waypointsDialogFactory.confirm(MiniMsgLegacyHybridSerializer.INSTANCE.deserialize(config.getString("confirm")));
        waypointsDialogFactory.infoText(config.getStringList("info-text"));
    }
}
