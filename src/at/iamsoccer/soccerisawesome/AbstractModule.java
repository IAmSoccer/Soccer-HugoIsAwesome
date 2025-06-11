package at.iamsoccer.soccerisawesome;

import co.aikar.commands.PaperCommandManager;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractModule {
    private final String name;
    private final List<Listener> listeners;

    public final SoccerIsAwesomePlugin plugin;

    public AbstractModule(SoccerIsAwesomePlugin plugin, String name, Listener... listeners) {
        this.plugin = plugin;
        this.name = name;
        this.listeners = new ArrayList<>(listeners.length + 1);
        this.listeners.addAll(Arrays.asList(listeners));
        if (this instanceof Listener listener) {
            this.listeners.add(listener);
        }
    }

    public boolean enable(PaperCommandManager commandManager) {
        for (Listener listener : listeners) {
            Bukkit.getPluginManager().registerEvents(listener, plugin);
        }
        return true;
    }

    public final void reloadConfigAndModule() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        reload();
    }

    public void reload() {}

    public void lifeCicleHandler(ReloadableRegistrarEvent<@NotNull Commands> commands) {}

    public boolean disable(PaperCommandManager commandManager) {
        for (Listener listener : listeners) {
            HandlerList.unregisterAll(listener);
        }
        return true;
    }

    public final String getName() {
        return name;
    }

    public final void warn(String s) {
        plugin.warn(String.format("[%s] %s", name, s));
    }
}
