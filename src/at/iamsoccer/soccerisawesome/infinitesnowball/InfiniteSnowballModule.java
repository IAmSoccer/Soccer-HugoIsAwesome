package at.iamsoccer.soccerisawesome.infinitesnowball;

import at.iamsoccer.soccerisawesome.AbstractModule;
import at.iamsoccer.soccerisawesome.SoccerIsAwesomePlugin;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.PaperCommandManager;

public class InfiniteSnowballModule extends AbstractModule {
    private BaseCommand command = null;

    public InfiniteSnowballModule(SoccerIsAwesomePlugin plugin) {
        super(plugin, "InfiniteSnowball",
            new InfiniteSnowballInteractListener(),
            new InfiniteSnowballInventoryListener()
        );
    }

    @Override
    public boolean enable(PaperCommandManager commandManager) {
        if (!super.enable(commandManager)) return false;

        command = new InfiniteSnowballCommands(plugin);
        commandManager.registerCommand(command);
        return true;
    }

    @Override
    public boolean disable(PaperCommandManager commandManager) {
        if (!super.disable(commandManager)) return false;
        if (command != null) {
            commandManager.unregisterCommand(command);
            command = null;
        }
        return true;
    }
}
