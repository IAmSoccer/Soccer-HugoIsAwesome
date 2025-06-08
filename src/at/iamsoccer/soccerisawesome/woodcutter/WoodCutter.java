package at.iamsoccer.soccerisawesome.woodcutter;

import at.iamsoccer.soccerisawesome.AbstractModule;
import at.iamsoccer.soccerisawesome.SoccerIsAwesomePlugin;
import co.aikar.commands.PaperCommandManager;
import com.google.common.base.Functions;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemType;
import org.bukkit.inventory.StonecuttingRecipe;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class WoodCutter extends AbstractModule {
    private static final String[] WOOD_ENDINGS = new String[]{
        "_wood",
        "_log",
        "_stem",
        "_hyphae"
    };
    private static final Map<ItemType, ItemType> WOOD_TYPES;

    static {
        WOOD_TYPES = Registry.ITEM.stream()
            .filter(WoodCutter::isWood)
            .filter(w -> WoodCutter.getStripped(w) != null)
            .collect(Collectors.toMap(Functions.identity(), WoodCutter::getStripped));
    }

    private final Set<NamespacedKey> registeredRecipes = new HashSet<>();

    public WoodCutter(SoccerIsAwesomePlugin plugin) {
        super(plugin, "WoodCutter");
    }

    @Override
    public boolean enable(PaperCommandManager commandManager) {
        if (!super.enable(commandManager)) return false; // should never fail
        return tryCreateStonecutterRecipes();
    }

    @Override
    public boolean disable(PaperCommandManager commandManager) {
        if (!super.disable(commandManager)) return false; // should never fail
        tryRemoveStonecutterRecipes();
        return true;
    }

    private boolean tryCreateStonecutterRecipes() {
        try {
            for (var entry : WOOD_TYPES.entrySet()) {
                final ItemType wood = entry.getKey();
                final ItemType stripped = entry.getValue();
                final NamespacedKey key = NamespacedKey.fromString(wood.key().value() + "_to_" + stripped.key().value(), plugin);
                final StonecuttingRecipe recipe = new StonecuttingRecipe(
                    Objects.requireNonNull(key),
                    stripped.createItemStack(),
                    Objects.requireNonNull(wood.asMaterial())
                );
                Bukkit.getServer().addRecipe(recipe);
                registeredRecipes.add(key);
            }
            return true;
        } catch (Exception e) {
            plugin.severe("Failed to load WoodCutter recipes", e);
            return false;
        }
    }

    private void tryRemoveStonecutterRecipes() {
        // this should never fail, since it only unregisters registered recipes
        for (var key : registeredRecipes) {
            Bukkit.getServer().removeRecipe(key);
        }
        registeredRecipes.clear();
    }

    private static @Nullable ItemType getStripped(ItemType type) {
        @Subst("oak_wood") final String itemName = type.key().value();
        return Registry.ITEM.get(Key.key("stripped_" + itemName));
    }

    private static boolean isWood(ItemType type) {
        for (var suffix : WOOD_ENDINGS) {
            if (type.key().value().endsWith(suffix) && !type.key().value().startsWith("stripped_")) {
                return true;
            }
        }
        return false;
    }
}
