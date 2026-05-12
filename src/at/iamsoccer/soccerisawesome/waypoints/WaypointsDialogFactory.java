package at.iamsoccer.soccerisawesome.waypoints;

import at.hugob.plugin.library.config.MiniMsgLegacyHybridSerializer;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.body.PlainMessageDialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.dialog.DialogLike;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

public class WaypointsDialogFactory {
    public static final ClickCallback.Options UNLIMITED_CALLBACK_OPTIONS = ClickCallback.Options.builder().uses(-1).lifetime(ChronoUnit.FOREVER.getDuration()).build();

    private Component title = Component.empty();
    private Component confirm = Component.empty();
    private Component close = Component.empty();
    private final TreeMap<Integer, Component> range_id_map = new TreeMap<>();
    private final HashMap<String, Integer> id_range_map = new HashMap<>();
    private final List<PlainMessageDialogBody> infoText = new ArrayList<>();

    WaypointsDialogFactory() {
    }

    public void clearOptions() {
        range_id_map.clear();
        id_range_map.clear();
        infoText.clear();
    }

    public void option(int distance, String display) {
        range_id_map.put(distance, MiniMsgLegacyHybridSerializer.INSTANCE.deserialize(display));
        id_range_map.put(Integer.toString(distance), distance);
    }
    public void infoText(final List<String> infoTexts) {
        for (String text : infoTexts) {
            infoText.add(DialogBody.plainMessage(MiniMsgLegacyHybridSerializer.INSTANCE.deserialize(text)));
        }
    }
    public void title(final Component title) {
        this.title = title;
    }
    public void confirm(final Component confirm) {
        this.confirm = confirm;
    }
    public void close(final Component close) {
        this.close = close;
    }

    @NotNull
    public DialogLike create(Player player) {
        return Dialog.create(builder -> builder.empty()
            .type(DialogType.confirmation(
                ActionButton.builder(close).build(),
                ActionButton.builder(confirm)
                    .action(confirmAction)
                    .build()
            ))
            .base(DialogBase.builder(title)
                .body(infoText)
                .inputs(List.of(
                    getRangeInput("transmit", "Transmit Range", player, Attribute.WAYPOINT_TRANSMIT_RANGE),
                    getRangeInput("receive", "Receive Range", player, Attribute.WAYPOINT_RECEIVE_RANGE)
                ))
                .build()
            )
        );
    }

    @NotNull
    private DialogInput getRangeInput(String id, String name, Player player, Attribute attribute) {
        final ArrayList<SingleOptionDialogInput.OptionEntry> options = new ArrayList<>(range_id_map.size());
        final int currentDistance = (int) player.getAttribute(attribute).getBaseValue();
        final int selectedDistance = currentDistance > range_id_map.lastKey() ? range_id_map.lastKey() : range_id_map.ceilingKey(currentDistance);
        range_id_map.forEach((distance, displayName) -> {
            options.add(SingleOptionDialogInput.OptionEntry.create(Integer.toString(distance), displayName, distance == selectedDistance));
        });
        return DialogInput.singleOption(id, MiniMsgLegacyHybridSerializer.INSTANCE.deserialize(name), options).build();
//        return DialogInput.numberRange(id, Component.text(name), 0, MAXIMUM_SELECTABLE_RANGE)
//            .initial(Math.min((float) player.getAttribute(attribute).getValue() / SCALE, MAXIMUM_SELECTABLE_RANGE))
//            .step(1f)
//            .labelFormat("%s: %sk")
//            .build();
    }

    private final DialogAction confirmAction = DialogAction.customClick(this::confirmCallback, UNLIMITED_CALLBACK_OPTIONS);

    private void confirmCallback(DialogResponseView response, Audience audience) {
        if (!(audience instanceof Player player)) return;
        final var transmit = response.getText("transmit");
        final var receive = response.getText("receive");
        if (!id_range_map.containsKey(transmit) || !id_range_map.containsKey(receive)) {
            audience.sendMessage(MiniMsgLegacyHybridSerializer.INSTANCE.deserialize("<red>[Waypoint] Something went wrong, please try again."));
            return;
        }
        player.getAttribute(Attribute.WAYPOINT_TRANSMIT_RANGE).setBaseValue(id_range_map.get(transmit));
        player.getAttribute(Attribute.WAYPOINT_RECEIVE_RANGE).setBaseValue(id_range_map.get(receive));
    }
}
