package at.iamsoccer.soccerisawesome.sizechanger;

import at.hugob.plugin.library.config.YamlFileConfig;
import at.iamsoccer.soccerisawesome.AbstractModule;
import at.iamsoccer.soccerisawesome.SoccerIsAwesomePlugin;
import co.aikar.commands.PaperCommandManager;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SizeChangerModule extends AbstractModule {
    public static final NamespacedKey ATTRIBUTE_KEY = new NamespacedKey("shia", "size_changer");
    public final List<SizePermission> sizePermissions = new ArrayList<>();
    public YamlFileConfig config;
    public Permission usePerm;
    public Permission reloadPerm;
    public Permission bypassPerm;

    public final Set<AttributeInfo> attributeTypes = new HashSet<>();


    public SizeChangerModule(SoccerIsAwesomePlugin plugin) {
        super(plugin, "SizeChanger");
    }

    @Override
    public boolean enable(PaperCommandManager commandManager) {
        if (!super.enable(commandManager)) return false;
        config = new YamlFileConfig(plugin, "size-changer.yml");
        usePerm = new Permission("shia.sizechanger.use", "Allows the usage of the /height command", PermissionDefault.OP);
        Bukkit.getPluginManager().addPermission(usePerm);
        reloadPerm = new Permission("shia.sizechanger.reload", "Allows to reload via /height reload", PermissionDefault.OP);
        usePerm.addParent(reloadPerm, true);
        Bukkit.getPluginManager().addPermission(reloadPerm);
        bypassPerm = new Permission("shia.sizechanger.bypass", "AllowsToUseAnySize", PermissionDefault.OP);
        usePerm.addParent(bypassPerm, true);
        Bukkit.getPluginManager().addPermission(bypassPerm);
        return true;
    }

    @Override
    public void reload() {
        sizePermissions.stream().filter(p -> p.permission != null).forEach(sp -> Bukkit.getPluginManager().removePermission(sp.permission));
        sizePermissions.clear();
        config.reload();
        for (String permission : config.getConfigurationSection("permissions").getKeys(false)) {
            String path = "permissions." + permission + ".min";
            if (!config.isDouble(path) && !config.isInt(path)) {
                warn(path + " is missing! cant register " + permission);
                continue;
            }
            var min = config.getDouble(path);
            path = "permissions." + permission + ".max";
            if (!config.isDouble(path) && !config.isInt(path)) {
                warn(path + " is missing! cant register " + permission);
                continue;
            }
            var max = config.getDouble(path);
            sizePermissions.add(new SizePermission(
                new Permission(
                    "shia.sizechanger." + permission,
                    "Allows sizes between %s and %s".formatted(min, max),
                    PermissionDefault.getByName(config.getString("permissions." + permission + ".defaults-to", "op"))
                ),
                new MinMaxSize(min, max)
            ));
        }
        sizePermissions.stream().filter(p -> p.permission != null).forEach(sp -> {
            Bukkit.getPluginManager().addPermission(sp.permission);
            usePerm.addParent(sp.permission, true);
        });

        attributeTypes.clear();
        attributeTypes.add(new AttributeInfo(Attribute.SCALE, 1, 1));

        for (String attributeName : config.getConfigurationSection("attributes").getKeys(false)) {
            var key = Key.key(attributeName);
            var attribute = Registry.ATTRIBUTE.get(key);
            if (attribute == null) {
                warn("Attribute " + attributeName + " not found");
            }
            if (config.isDouble("attributes." + attributeName)) {
                attributeTypes.add(new AttributeInfo(attribute,
                    config.getDouble("attributes." + attributeName),
                    config.getDouble("attributes." + attributeName)
                ));
            } else {
                attributeTypes.add(new AttributeInfo(attribute,
                    config.getDouble("attributes." + attributeName + ".smaller", 0),
                    config.getDouble("attributes." + attributeName + ".bigger", 0)
                ));
            }
        }
    }

    @Override
    public boolean disable(PaperCommandManager commandManager) {
        return super.disable(commandManager);
    }

    @Override
    public void lifeCicleHandler(ReloadableRegistrarEvent<Commands> commands) {
        commands.registrar().register(new SizeChangerCommand(this).createCommand());
    }

    public boolean isAllowedToUse(Player player, double num) {
        if (player.hasPermission(bypassPerm)) return true;
        for (SizePermission perm : sizePermissions) {
            if (perm.isAllowedToUse(player, num)) return true;
        }
        return false;
    }

    public List<MinMaxSize> getLimits(Player player) {
        var limits = new ArrayList<MinMaxSize>();
        for (SizePermission sizePermission : sizePermissions) {
            if (sizePermission.permission == null || player.hasPermission(sizePermission.permission)) {
                var iter = limits.iterator();
                var newMinMax = sizePermission.minMax;
                while (limits.stream().anyMatch(newMinMax::overlap)) {
                    while (iter.hasNext()) {
                        var minMax = iter.next();
                        if (minMax.overlap(sizePermission.minMax)) {
                            iter.remove();
                            newMinMax = minMax.merge(newMinMax);
                            break;
                        }
                    }
                }
                limits.add(newMinMax);
            }
        }
        return limits;
    }

    public void resetSize(LivingEntity livingEntity) {
        for (var attributeType : Registry.ATTRIBUTE) {
            var attribute = livingEntity.getAttribute(attributeType);
            if (attribute != null) {
                attribute.removeModifier(ATTRIBUTE_KEY);
            }
        }
    }

    public boolean setSize(LivingEntity livingEntity, double sizeInBlocks) {
        // players are approcimately 1/10th smaller than a block, so I approximate it with an multiplication of 1.1
        final var factor = sizeInBlocks * 0.55D - 1;
        final var scale = livingEntity.getAttribute(Attribute.SCALE);
        if(scale != null) {
            var modifier = scale.getModifier(ATTRIBUTE_KEY);
            if(modifier != null && modifier.getAmount() == factor) {
                return false;
            }
        }

        for (var attributeInfo : attributeTypes) {
            var attribute = livingEntity.getAttribute(attributeInfo.type);
            if (attribute != null) {
                attribute.removeModifier(ATTRIBUTE_KEY);
                attribute.addModifier(new AttributeModifier(ATTRIBUTE_KEY,
                    factor * attributeInfo.getModifier(factor),
                    AttributeModifier.Operation.ADD_SCALAR
                ));
            }
        }
        return true;
    }


    public record MinMaxSize(
        double min,
        double max
    ) {
        public boolean contains(double num) {
            return min <= num && num <= max;
        }

        public boolean overlap(MinMaxSize other) {
            return contains(other.min) || contains(other.max) || other.contains(min) || other.contains(max);
        }

        public MinMaxSize merge(MinMaxSize other) {
            return new MinMaxSize(Math.min(min, other.min), Math.max(max, other.max));
        }
    }

    public record SizePermission(
        Permission permission,
        MinMaxSize minMax
    ) {
        public boolean isAllowedToUse(Player player, double num) {
            if (!minMax.contains(num)) return false;
            return permission == null || player.hasPermission(permission);
        }
    }

    public record AttributeInfo(
        Attribute type,
        double reducingModifier,
        double increasingModifier
    ) {
        public double getModifier(double factor) {
            if (factor + 1 < 1) return reducingModifier;
            else return increasingModifier;
        }
    }
}
