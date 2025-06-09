package at.iamsoccer.soccerisawesome.essentialsafkhook;

import at.iamsoccer.soccerisawesome.AbstractModule;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import org.bukkit.command.CommandSender;

@CommandAlias("essentialsafk")
public class EssentialsAFKHookCommands extends BaseCommand {
    private final AbstractModule module;

    public EssentialsAFKHookCommands(AbstractModule module) {
        this.module = module;
    }

    @Subcommand("reload")
    @CommandPermission("sia.essentialsafk.reload")
    public void onReload(CommandSender sender) {
        sender.sendMessage("[Essentials AFK Hook] Reloading");
        module.reloadConfigAndModule();
        sender.sendMessage("[Essentials AFK Hook] Reloaded");
    }
}