package at.iamsoccer.soccerisawesome;

import at.iamsoccer.soccerisawesome.blockrotator.BlockRotatorListener;
import at.iamsoccer.soccerisawesome.colorfulshulkers.ColorfulShulkers;
import at.iamsoccer.soccerisawesome.damagenullifier.DamageNullifierOnTeleportOrJoinNullifyListener;
import at.iamsoccer.soccerisawesome.essentialsafkhook.EssentialsAFKHookListener;
import at.iamsoccer.soccerisawesome.infinitesnowball.InfiniteSnowballModule;
import at.iamsoccer.soccerisawesome.lessannoyingitemframes.LessAnnoyingItemFramesListener;
import at.iamsoccer.soccerisawesome.prettycoloredglass.PrettyColoredGlassListener;
import at.iamsoccer.soccerisawesome.sheepcolorchanger.SheepColorChangerListener;
import at.iamsoccer.soccerisawesome.woodcutter.WoodCutter;
import co.aikar.commands.PaperCommandManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

public class SoccerIsAwesomePlugin extends JavaPlugin {
    private List<AbstractModule> modules = Collections.emptyList();

    private PaperCommandManager commandManager;

    @Override
    public void onEnable() {
        // update and reload the default config
        updateConfig();
        // command manager
        commandManager = new PaperCommandManager(this);

        modules = new ArrayList<>(List.of(
            new DamageNullifierOnTeleportOrJoinNullifyListener(this),
            new WoodCutter(this),
            new SheepColorChangerListener(this),
            new InfiniteSnowballModule(this),
            new LessAnnoyingItemFramesListener(this),
            new PrettyColoredGlassListener(this),
            new EssentialsAFKHookListener(this),
            new ColorfulShulkers(this),
            new BlockRotatorListener(this)
        ));

        var iter = modules.iterator();
        while (iter.hasNext()) {
            var module = iter.next();
            if (module.enable(commandManager)) {
                info("Module " + module.getName() + " has been enabled!");
            } else {
                warn("Module " + module.getName() + " Failed to properly load and has been disabled!");
                if (!disableModule(module)) return;
                iter.remove();
            }
        }

        getServer().getConsoleSender().sendMessage("Hi -Lynch");
        reload();
    }

    private boolean disableModule(AbstractModule module) {
        if (module.disable(commandManager)) {
            return true;
        } else {
            warn("Could not disable module " + module.getName() + "! Disabling plugin...");
            this.getServer().getPluginManager().disablePlugin(this);
            return false;
        }
    }

    @Override
    public void onDisable() {
        for (AbstractModule module : modules) {
            if (!disableModule(module)) continue;
            warn("Module " + module.getName() + " has been disabled!");
        }
    }

    public void reload() {
        saveDefaultConfig();
        reloadConfig();
        modules.forEach(AbstractModule::reload);
    }

    public void severe(String message, Exception e) {
        var lines = new ArrayList<String>();
        lines.add(message);
        lines.add(e.getMessage());
        Arrays.stream(e.getStackTrace())
            .map(Object::toString)
            .forEach(lines::add);
        var logged = String.join("<newline>", lines);
        log(NamedTextColor.RED, logged);
    }

    public void warn(String message) {
        log(NamedTextColor.RED, message);
    }

    public void info(String message) {
        log(NamedTextColor.GREEN, message);
    }

    public void log(TextColor color, String message) {
        getServer().getConsoleSender().sendMessage(MiniMessage.miniMessage().deserialize(
            "<gray>[<gold>SHIA<gray>] <color><message>",
            TagResolver.builder()
                .tag("color", Tag.styling(color))
                .tag("message", Tag.inserting(Component.text(message)))
                .build()
        ));
    }

    private void updateConfig() {
        saveDefaultConfig();
        reloadConfig();
        Path configFilePath = Path.of(getDataFolder().getPath(), "config.yml");
        if (!Files.exists(configFilePath)) {
            saveDefaultConfig();
            return;
        }

        int version = getConfig().getInt("version", 1);
        LinkedList<String> lines;
        try {
            lines = new LinkedList<>(Files.readAllLines(configFilePath));
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not update config!", e);
            return;
        }
        if (version < 2) {
            addConfigOption(lines, "min-tp-distance");
            updateVersion(lines, 2);
        }
        try {
            Files.write(configFilePath, lines, StandardOpenOption.CREATE);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not update config!", e);
        }
    }

    private void addConfigOption(List<String> lines, String configOption) {
        if (!configOption.contains("."))
            lines.add(String.format("%s: %s", configOption, getConfig().getString(configOption)));
    }

    private void updateVersion(List<String> lines, int version) {
        lines.removeIf(line -> line.startsWith("version: "));
        lines.add("version: " + version);
    }
}
