package at.iamsoccer.soccerisawesome.colorfulshulkers;

import at.iamsoccer.soccerisawesome.AbstractModule;
import at.iamsoccer.soccerisawesome.SoccerIsAwesomePlugin;
import co.aikar.commands.PaperCommandManager;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemType;
import org.bukkit.inventory.ShapelessRecipe;
import org.intellij.lang.annotations.Subst;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ColorfulShulkers extends AbstractModule {
    private static final Set<String> DYES_NAMES;

    static {
        //Add all the dyes to DYE_MATERIALS
        DYES_NAMES = Registry.ITEM.stream()
            .filter(i -> i.key().value().endsWith("_dye"))
            .map(i -> i.key().value().substring(0, i.key().value().lastIndexOf('_')))
            .collect(Collectors.toUnmodifiableSet());
    }

    private final Set<NamespacedKey> registeredRecipes = new HashSet<>();

    public ColorfulShulkers(SoccerIsAwesomePlugin plugin) {
        super(plugin, "ColorfulShulkers");
    }

    @Override
    public boolean enable(PaperCommandManager commandManager) {
        if (!super.enable(commandManager)) return false; // should never fail
        return tryCreateColorfulShulkerRecipes();
    }

    @Override
    public boolean disable(PaperCommandManager commandManager) {
        if (!super.disable(commandManager)) return false; // should never fail
        tryRemoveColorfulShulkerRecipes();
        return true;
    }

    public boolean tryCreateColorfulShulkerRecipes() {
        //Try to make the recipes!
        try {
            for (@Subst("red") var color : DYES_NAMES) {
                final ItemType dye = Objects.requireNonNull(Registry.ITEM.get(Key.key(color + "_dye")));
                final ItemType shulker = Objects.requireNonNull(Registry.ITEM.get(Key.key(color + "_shulker_box")));
                final NamespacedKey key = NamespacedKey.fromString(color + "_colorful_shulker", plugin);
                Objects.requireNonNull(key, "Key should never be null");
                final ShapelessRecipe recipe = new ShapelessRecipe(
                    key,
                    shulker.createItemStack()
                );
                recipe.addIngredient(Material.SHULKER_SHELL);
                recipe.addIngredient(Material.SHULKER_SHELL);
                recipe.addIngredient(Material.CHEST);
                Material dyeMaterial = Objects.requireNonNull(dye.asMaterial());
                recipe.addIngredient(dyeMaterial);

                Bukkit.getServer().addRecipe(recipe);
                registeredRecipes.add(key);
            }
            return true;
        } catch (Exception e) {
            plugin.severe("Failed to load ColorfulShulkers recipes", e);
            return false;
        }
    }

    public void tryRemoveColorfulShulkerRecipes() {
        //remove the recipes... :(
        // should never fail, since the set only includes registered ones
        for (var key : registeredRecipes) {
            Bukkit.getServer().removeRecipe(key);
        }
        registeredRecipes.clear();
    }
}
