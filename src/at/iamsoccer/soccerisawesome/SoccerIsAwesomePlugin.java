package at.iamsoccer.soccerisawesome;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import at.iamsoccer.soccerisawesome.blockrotator.BlockRotatorListener;
import at.iamsoccer.soccerisawesome.colorfulshulkers.ColorfulShulkers;
import at.iamsoccer.soccerisawesome.damagenullifier.*;
import at.iamsoccer.soccerisawesome.essentialsafkhook.EssentialsAFKHookCommands;
import at.iamsoccer.soccerisawesome.essentialsafkhook.EssentialsAFKHookListener;
import at.iamsoccer.soccerisawesome.infinitesnowball.*;
import at.iamsoccer.soccerisawesome.lessannoyingitemframes.LessAnnoyingItemFramesCommands;
import at.iamsoccer.soccerisawesome.lessannoyingitemframes.LessAnnoyingItemFramesListener;
import at.iamsoccer.soccerisawesome.prettycoloredglass.PrettyColoredGlassListener;
import at.iamsoccer.soccerisawesome.sheepcolorchanger.*;
import at.iamsoccer.soccerisawesome.woodcutter.*;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;

import co.aikar.commands.PaperCommandManager;

public class SoccerIsAwesomePlugin extends JavaPlugin {
    public at.iamsoccer.soccerisawesome.essentialsafkhook.EssentialsAFKHookListener EssentialsAFKHookListener;
    private DamageNullifierOnTeleportOrJoinNullifyListener nullifyListener;

    @Override
    public void onEnable() {
        // DamageNullifier Stuff
        updateConfig();
        reloadConfig();
        PaperCommandManager commandManager = new PaperCommandManager(this);
        commandManager.registerCommand(new DamageNullifierOnTeleportOrJoinCommand(this));
        nullifyListener = new DamageNullifierOnTeleportOrJoinNullifyListener();
        getServer().getPluginManager().registerEvents(nullifyListener, this);
        damagenullifyReload();
        getServer().getConsoleSender().sendMessage(ChatColor.GRAY + "[" + ChatColor.GOLD + "SHIA" + ChatColor.GRAY + "]" + ChatColor.GREEN + " Module DamageNullifierOnTeleportOrJoin has been enabled!");

        //  Try to load in all the recipes from WoodCutter, if it fails disable the plugin and print error.
        if(WoodCutter.tryCreateStonecutterRecipes()) {
            getServer().getConsoleSender().sendMessage(ChatColor.GRAY + "[" + ChatColor.GOLD + "SHIA" + ChatColor.GRAY + "]" + ChatColor.GREEN + " Module WoodCutter has been enabled!");
        }
        else {
            getServer().getConsoleSender().sendMessage(ChatColor.GRAY + "[" + ChatColor.GOLD + "SHIA" + ChatColor.GRAY + "]" + ChatColor.RED + " Module WoodCutter Failed to load all or some recipes.... Disabling Plugin");
            this.getServer().getPluginManager().disablePlugin(this);
        }

        // Loads Listener for Sheep Color Changer
        getServer().getPluginManager().registerEvents(new SheepColorChangerListener(), this);
        getServer().getConsoleSender().sendMessage(ChatColor.GRAY + "[" + ChatColor.GOLD + "SHIA" + ChatColor.GRAY + "]" + ChatColor.GREEN + " Module SheepColorChanger has been enabled!");

        // InfiniteSnowball Stuff
        commandManager.registerCommand(new InfiniteSnowballCommands(this));
        getServer().getPluginManager().registerEvents(new InfiniteSnowballInventoryListener(), this);
        getServer().getPluginManager().registerEvents(new InfiniteSnowballInteractListener(), this);
        getServer().getConsoleSender().sendMessage(ChatColor.GRAY + "[" + ChatColor.GOLD + "SHIA" + ChatColor.GRAY + "]" + ChatColor.GREEN + " Module InfiniteSnowball has been enabled!");
        getServer().getConsoleSender().sendMessage("Hi -Lynch");

        // LessAnnoyingItemFrame Stuff
        commandManager.registerCommand(new LessAnnoyingItemFramesCommands(this));
        getServer().getPluginManager().registerEvents(new LessAnnoyingItemFramesListener(), this);
        getServer().getConsoleSender().sendMessage(ChatColor.GRAY + "[" + ChatColor.GOLD + "SHIA" + ChatColor.GRAY + "]" + ChatColor.GREEN + " Module LessAnnoyingItemFrames has been enabled!");

        // PrettyColoredGlass Stuff
        getServer().getPluginManager().registerEvents(new PrettyColoredGlassListener(), this);
        getServer().getConsoleSender().sendMessage(ChatColor.GRAY + "[" + ChatColor.GOLD + "SHIA" + ChatColor.GRAY + "]" + ChatColor.GREEN + " Module PrettyColoredClass has been enabled!");

        //EssentialsAFKHook Stuff
        commandManager.registerCommand(new EssentialsAFKHookCommands(this));
        EssentialsAFKHookListener = new EssentialsAFKHookListener(this);
        getServer().getPluginManager().registerEvents(EssentialsAFKHookListener, this);
        getServer().getConsoleSender().sendMessage(ChatColor.GRAY + "[" + ChatColor.GOLD + "SHIA" + ChatColor.GRAY + "]" + ChatColor.GREEN + " Module EssentialsAFKHook has been enabled!");

        //  Try to load in all the recipes from ColorfulShulkers, if it fails disable the plugin and print error.
        if(ColorfulShulkers.tryCreateColorfulShulkerRecipes()) {
            getServer().getConsoleSender().sendMessage(ChatColor.GRAY + "[" + ChatColor.GOLD + "SHIA" + ChatColor.GRAY + "]" + ChatColor.GREEN + " Module ColorfulShulkers has been enabled!");
        }
        else {
            getServer().getConsoleSender().sendMessage(ChatColor.GRAY + "[" + ChatColor.GOLD + "SHIA" + ChatColor.GRAY + "]" + ChatColor.RED + " Module ColorfulShulkers Failed to load all or some recipes.... Disabling Plugin");
            this.getServer().getPluginManager().disablePlugin(this);
        }
        //BlockRotator Stuff
        getServer().getPluginManager().registerEvents(new BlockRotatorListener(this), this);
        getServer().getConsoleSender().sendMessage(ChatColor.GRAY + "[" + ChatColor.GOLD + "SHIA" + ChatColor.GRAY + "]" + ChatColor.GREEN + " Module BlockRotator has been enabled!");
    }

    @Override
    public void onDisable() {
        getServer().getConsoleSender().sendMessage(ChatColor.GRAY + "[" + ChatColor.GOLD + "SHIA" + ChatColor.GRAY + "]" + ChatColor.RED + " Module DamageNullifierOnTeleportOrJoin has been disabled!");
        if(WoodCutter.tryRemoveStonecutterRecipes()) {
            getServer().getConsoleSender().sendMessage(ChatColor.GRAY + "[" + ChatColor.GOLD + "SHIA" + ChatColor.GRAY + "]" + ChatColor.RED + " Module Woodcutter has been disabled!");
        }
        else {
            getServer().getConsoleSender().sendMessage(ChatColor.GRAY + "[" + ChatColor.GOLD + "SHIA" + ChatColor.GRAY + "]" + ChatColor.RED + " Module Woodcutter has Failed to remove all or some recipes.... Disabling Plugin");
            this.getServer().getPluginManager().disablePlugin(this);
        }
        getServer().getConsoleSender().sendMessage(ChatColor.GRAY + "[" + ChatColor.GOLD + "SHIA" + ChatColor.GRAY + "]" + ChatColor.RED + " Module SheepColorChanger has been disabled!");
        getServer().getConsoleSender().sendMessage(ChatColor.GRAY + "[" + ChatColor.GOLD + "SHIA" + ChatColor.GRAY + "]" + ChatColor.RED + " Module InfiniteSnowball has been disabled!");
        getServer().getConsoleSender().sendMessage(ChatColor.GRAY + "[" + ChatColor.GOLD + "SHIA" + ChatColor.GRAY + "]" + ChatColor.RED + " Module LessAnnoyingItemFrames has been disabled!");
        getServer().getConsoleSender().sendMessage(ChatColor.GRAY + "[" + ChatColor.GOLD + "SHIA" + ChatColor.GRAY + "]" + ChatColor.RED + " Module PrettyColoredClass has been disabled!");
        getServer().getConsoleSender().sendMessage(ChatColor.GRAY + "[" + ChatColor.GOLD + "SHIA" + ChatColor.GRAY + "]" + ChatColor.RED + " Module EssentialsAFKHook has been disabled!");
        getServer().getConsoleSender().sendMessage(ChatColor.GRAY + "[" + ChatColor.GOLD + "SHIA" + ChatColor.GRAY + "]" + ChatColor.RED + " Module BlockRotator has been disabled!");
        if(ColorfulShulkers.tryRemoveColorfulShulkerRecipes()) {
            getServer().getConsoleSender().sendMessage(ChatColor.GRAY + "[" + ChatColor.GOLD + "SHIA" + ChatColor.GRAY + "]" + ChatColor.RED + " Module ColorfulShulkers has been disabled!");
        }
        else {
            getServer().getConsoleSender().sendMessage(ChatColor.GRAY + "[" + ChatColor.GOLD + "SHIA" + ChatColor.GRAY + "]" + ChatColor.RED + " Module ColorfulShulkers has Failed to remove all or some recipes.... Disabling Plugin");
            this.getServer().getPluginManager().disablePlugin(this);
        }
    }

    public void damagenullifyReload() {
        saveDefaultConfig();
        reloadConfig();
        nullifyListener.update(getConfig().getLong("min-tp-distance"), getConfig().getLong("immunity-time"),
                getConfig().getBoolean("bossbar.show"), getConfig().getString("bossbar.name"),
                getConfig().getString("bossbar.color"), getConfig().getString("bossbar.type"));
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
