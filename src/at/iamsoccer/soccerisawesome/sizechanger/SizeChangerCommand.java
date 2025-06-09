package at.iamsoccer.soccerisawesome.sizechanger;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.Collectors;

import static io.papermc.paper.command.brigadier.Commands.argument;
import static io.papermc.paper.command.brigadier.Commands.literal;

public class SizeChangerCommand {

    private final static double METERS_PER_FEET = 0.3048;
    private final static double METERS_PER_INCH = 0.0254;

    public static LiteralCommandNode<CommandSourceStack> createCommand(SizeChangerModule module) {
        return literal("height")
            .requires(sender -> sender.getSender() instanceof Player && sender.getSender().hasPermission(module.usePerm))
            .then(literal("reload")
                .requires(sender -> sender.getSender().hasPermission(module.reloadPerm))
                .executes(ctx -> {
                    var sender = ctx.getSource().getSender();
                    sender.sendMessage(module.config.getComponent("commands.reload.start"));
                    module.reload();
                    sender.sendMessage(module.config.getComponent("commands.reload.finish"));
                    return Command.SINGLE_SUCCESS;
                })
            ).then(literal("reset")
                .executes(ctx -> {
                    var sender = ctx.getSource().getSender();
                    var tragetEntity = ctx.getSource().getExecutor();

                    LivingEntity livingEntity = getLivingEntity(tragetEntity, sender, module);
                    if (livingEntity == null) return Command.SINGLE_SUCCESS;

                    module.resetSize(livingEntity);
                    return Command.SINGLE_SUCCESS;
                })
            ).then(literal("blocks").then(argument("size-in-blocks", DoubleArgumentType.doubleArg(0.1, 32D))
                .executes(ctx -> {
                    var player = (Player) ctx.getSource().getSender();
                    var tragetEntity = ctx.getSource().getExecutor();
                    double size = ctx.getArgument("size-in-blocks", Double.class);

                    LivingEntity livingEntity = getLivingEntity(tragetEntity, player, module);
                    if (livingEntity == null) return Command.SINGLE_SUCCESS;

                    if (!module.isAllowedToUse(player, size)) {
                        var limits = module.getLimits(player);
                        player.sendMessage(module.config.getComponent("commands.outside-range", TagResolver.builder()
                            .tag("sizes", Tag.inserting(Component.text(
                                limits.stream().map(minMaxSize -> "[%s Blocks, %s Blocks]".formatted(minMaxSize.min(), minMaxSize.max())).collect(Collectors.joining(", "))
                            )))
                            .build()));
                        return Command.SINGLE_SUCCESS;
                    }

                    module.setSize(livingEntity, size);
                    return Command.SINGLE_SUCCESS;
                })
            )).then(literal("cm").then(argument("size-in-cm", DoubleArgumentType.doubleArg(0.1, 32D))
                .executes(ctx -> {
                    var player = (Player) ctx.getSource().getSender();
                    var tragetEntity = ctx.getSource().getExecutor();
                    double size = ctx.getArgument("size-in-cm", Double.class) * 100;

                    LivingEntity livingEntity = getLivingEntity(tragetEntity, player, module);
                    if (livingEntity == null) return Command.SINGLE_SUCCESS;

                    if (!module.isAllowedToUse(player, size)) {
                        var limits = module.getLimits(player);
                        player.sendMessage(module.config.getComponent("commands.outside-range", TagResolver.builder()
                            .tag("sizes", Tag.inserting(Component.text(
                                limits.stream().map(minMaxSize -> "[%s cm, %s cm]".formatted(minMaxSize.min() * 100, minMaxSize.max() * 100)).collect(Collectors.joining(", "))
                            )))
                            .build()));
                        return Command.SINGLE_SUCCESS;
                    }

                    module.setSize(livingEntity, size);
                    return Command.SINGLE_SUCCESS;
                })
            )).then(literal("ft").then(argument("size-in-feet", IntegerArgumentType.integer(0, 105))
                .then(literal("in").then(argument("size-in-inches", IntegerArgumentType.integer(0, 15))
                    .executes(ctx -> {
                        var player = (Player) ctx.getSource().getSender();
                        var tragetEntity = ctx.getSource().getExecutor();
                        int feet = ctx.getArgument("size-in-feet", Integer.class);
                        int inches = ctx.getArgument("size-in-inches", Integer.class);
                        double size = feetAndInchesToBlocks(feet, inches);

                        LivingEntity livingEntity = getLivingEntity(tragetEntity, player, module);
                        if (livingEntity == null) return Command.SINGLE_SUCCESS;

                        if (!module.isAllowedToUse(player, size)) {
                            var limits = module.getLimits(player);
                            player.sendMessage(module.config.getComponent("commands.outside-range", TagResolver.builder()
                                .tag("sizes", Tag.inserting(Component.text(
                                    limits.stream().map(SizeChangerCommand::blocksToFeetAndInches).collect(Collectors.joining(", "))
                                )))
                                .build()));
                            return Command.SINGLE_SUCCESS;
                        }

                        module.setSize(livingEntity, size);
                        return Command.SINGLE_SUCCESS;
                    })
                ))))
            .build();
    }

    private static double feetAndInchesToBlocks(int feet, int inches) {
        return feet * METERS_PER_FEET + inches * METERS_PER_INCH;
    }

    private static String blocksToFeetAndInches(SizeChangerModule.MinMaxSize minMaxSize) {
        return "[%s, %s]".formatted(blocksToFeetAndInches(minMaxSize.min(), RoundingMode.UP), blocksToFeetAndInches(minMaxSize.max(), RoundingMode.DOWN));
    }

    private static String blocksToFeetAndInches(double blocks, RoundingMode roundingMode) {
        int feet = (int) (blocks / METERS_PER_FEET);
        double remainder = blocks % METERS_PER_FEET;
        double inches = remainder / METERS_PER_INCH;
        return "%s ft %s in".formatted(feet, new BigDecimal(inches).setScale(0, roundingMode).doubleValue());
    }

    private static @Nullable LivingEntity getLivingEntity(Entity tragetEntity, CommandSender sender, SizeChangerModule module) {
        if (!(tragetEntity instanceof LivingEntity livingEntity)) {
            sender.sendMessage(module.config.getComponent("commands.target-must-be-a-living-entity"));
            return null;
        }
        return livingEntity;
    }


}
