package at.iamsoccer.soccerisawesome.damagenullifier;

import at.iamsoccer.soccerisawesome.AbstractModule;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import org.bukkit.command.CommandSender;

@CommandAlias("damagenullifier")
public class DamageNullifierOnTeleportOrJoinCommand extends BaseCommand {
    private final AbstractModule module;

    public DamageNullifierOnTeleportOrJoinCommand(AbstractModule module) {
        this.module = module;
    }

    @Subcommand("reload")
    @CommandPermission("damagenullifier.reload")
    public void onReload(CommandSender sender) {
        sender.sendMessage("[DamageNullifier] Reloading");
        module.reload();
        sender.sendMessage("[DamageNullifier] Reloaded");
    }
}
