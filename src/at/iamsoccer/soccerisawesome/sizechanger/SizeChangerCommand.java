package at.iamsoccer.soccerisawesome.sizechanger;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import static io.papermc.paper.command.brigadier.Commands.argument;
import static io.papermc.paper.command.brigadier.Commands.literal;

public class SizeChangerCommand {
    private final static double METERS_PER_FEET = 0.3048;
    private final static double METERS_PER_INCH = 0.0254;

    private final static ConcurrentHashMap<UUID, LocalDateTime> lastUsed = new ConcurrentHashMap<>();

    private final SizeChangerModule module;

    public SizeChangerCommand(SizeChangerModule module) {
        this.module = module;
    }


    public LiteralCommandNode<CommandSourceStack> createCommand() {
        return literal("height")
            .requires(sender -> sender.getSender().hasPermission(module.usePerm))
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
                .requires(sender -> sender.getSender() instanceof Player)
                .executes(ctx -> {
                    var sender = ctx.getSource().getSender();
                    var tragetEntity = ctx.getSource().getExecutor();

                    LivingEntity livingEntity = getLivingEntity(tragetEntity, sender);
                    if (livingEntity == null) return Command.SINGLE_SUCCESS;

                    module.resetSize(livingEntity);
                    return Command.SINGLE_SUCCESS;
                })
            ).then(literal("blocks")
                .requires(sender -> sender.getSender() instanceof Player)
                .then(argument("size-in-blocks", DoubleArgumentType.doubleArg(0.1, 32D))
                    .executes(ctx -> {
                        var player = (Player) ctx.getSource().getSender();
                        var tragetEntity = ctx.getSource().getExecutor();
                        double size = ctx.getArgument("size-in-blocks", Double.class);

                        doSizeChangeCommand(tragetEntity, player, size, (num, rm) -> formatNumberToString(num, "formats.blocks", "blocks", 1));
                        return Command.SINGLE_SUCCESS;
                    })
                )).then(literal("cm")
                .requires(sender -> sender.getSender() instanceof Player)
                .then(argument("size-in-cm", DoubleArgumentType.doubleArg(10, 3200D))
                    .executes(ctx -> {
                        var player = (Player) ctx.getSource().getSender();
                        var tragetEntity = ctx.getSource().getExecutor();
                        double size = ctx.getArgument("size-in-cm", Double.class) * 100;

                        doSizeChangeCommand(tragetEntity, player, size, (num, rm) -> formatNumberToString(num, "formats.cm", "cm", 100));
                        return Command.SINGLE_SUCCESS;
                    })
                )).then(literal("ft")
                .requires(sender -> sender.getSender() instanceof Player)
                .then(argument("size-in-feet", IntegerArgumentType.integer(0, 105))
                    .executes(ctx -> {
                        var player = (Player) ctx.getSource().getSender();
                        var tragetEntity = ctx.getSource().getExecutor();
                        int feet = ctx.getArgument("size-in-feet", Integer.class);
                        double size = feetAndInchesToBlocks(feet, 0);

                        doSizeChangeCommand(tragetEntity, player, size, this::blocksToFeetAndInches);
                        return Command.SINGLE_SUCCESS;
                    })
                    .then(literal("in").then(argument("size-in-inches", IntegerArgumentType.integer(0, 15)).executes(ctx -> {
                        var player = (Player) ctx.getSource().getSender();
                        var tragetEntity = ctx.getSource().getExecutor();
                        int feet = ctx.getArgument("size-in-feet", Integer.class);
                        int inches = ctx.getArgument("size-in-inches", Integer.class);
                        double size = feetAndInchesToBlocks(feet, inches);

                        doSizeChangeCommand(tragetEntity, player, size, this::blocksToFeetAndInches);
                        return Command.SINGLE_SUCCESS;
                    })))))
            .build();
    }

    private Component formatNumberToString(double number, String formatPath, String tagName, int magnitute) {
        return module.config.getComponent(formatPath, TagResolver.builder()
            .tag(tagName, Tag.inserting(Component.text(number * magnitute)))
            .build());
    }

    private void doSizeChangeCommand(Entity tragetEntity, Player player, double size, BiFunction<Double, RoundingMode, Component> rangeToComponentFunction) {
        LivingEntity livingEntity = getLivingEntity(tragetEntity, player);
        if (livingEntity == null) return;

        if (lastUsed.containsKey(player.getUniqueId()) && !player.hasPermission(module.bypassPerm)) {
            var elapsedTime = Duration.between(lastUsed.get(player.getUniqueId()), LocalDateTime.now());
            if (elapsedTime.toSeconds() < module.config.getInt("cooldown")) {
                var timeUntilUsedAgain = Duration.of(module.config.getInt("cooldown"), ChronoUnit.SECONDS).minus(elapsedTime);
                player.sendMessage(module.config.getComponent("commands.on-cooldown", TagResolver.builder()
                    .tag("remaining", Tag.inserting(Component.text(DurationFormatUtils.formatDurationWords(timeUntilUsedAgain.toMillis(), true, true))))
                    .build()));
                return;
            }
        }

        if (!module.isAllowedToUse(player, size)) {
            var limits = module.getLimits(player);
            player.sendMessage(module.config.getComponent("commands.outside-range", TagResolver.builder()
                .tag("sizes", Tag.inserting(Component.join(
                    JoinConfiguration.builder().separator(module.config.getComponent("formats.list-separator")).build(),
                    limits.stream().map(range -> module.config.getComponent("formats.range", TagResolver.builder()
                        .tag("min", Tag.inserting(rangeToComponentFunction.apply(range.min(), RoundingMode.UP)))
                        .tag("max", Tag.inserting(rangeToComponentFunction.apply(range.max(), RoundingMode.DOWN)))
                        .build())).toList()
                )))
                .build()));
            return;
        }
        var sizeResolver = TagResolver.builder()
            .tag("size", Tag.inserting(rangeToComponentFunction.apply(size, RoundingMode.HALF_UP)))
            .build();
        if (!module.setSize(livingEntity, size)) {
            player.sendMessage(module.config.getComponent("commands.already-that-tall", sizeResolver));
            return;
        }
        lastUsed.put(player.getUniqueId(), LocalDateTime.now());
        player.sendMessage(module.config.getComponent("commands.change-success", sizeResolver));
    }

    private double feetAndInchesToBlocks(int feet, int inches) {
        return feet * METERS_PER_FEET + inches * METERS_PER_INCH;
    }

    private Component blocksToFeetAndInches(double blocks, RoundingMode roundingMode) {
        int feet = (int) (blocks / METERS_PER_FEET);
        double remainder = blocks % METERS_PER_FEET;
        double inches = remainder / METERS_PER_INCH;
        return module.config.getComponent("formats.feet", TagResolver.builder()
            .tag("ft", Tag.inserting(Component.text(feet)))
            .tag("in", Tag.inserting(Component.text(new BigDecimal(inches).setScale(0, roundingMode).intValue())))
            .build()
        );
    }

    private @Nullable LivingEntity getLivingEntity(Entity tragetEntity, CommandSender sender) {
        if (!(tragetEntity instanceof LivingEntity livingEntity)) {
            sender.sendMessage(module.config.getComponent("commands.target-must-be-a-living-entity"));
            return null;
        }
        return livingEntity;
    }


}
