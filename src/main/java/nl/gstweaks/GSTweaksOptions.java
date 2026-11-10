package nl.gstweaks;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.io.IOException;

final class GSTweaksOptions {
    private final GSTweaksConfig config = GSTweaksConfig.get();
    private boolean reloading;

    static void register(IEventBus modBus) {
        var options = new GSTweaksOptions();
        modBus.<RegisterPayloadHandlersEvent>addListener(event ->
                event.registrar("1").optional().playToClient(Status.TYPE, Status.CODEC));
        NeoForge.EVENT_BUS.addListener(options::registerCommands);
    }

    private void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("gstweaks").then(Commands.literal("options")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> show(context.getSource()))
                .then(Commands.literal("wooden_tools_only")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> setWoodenToolsOnly(context.getSource(),
                                        BoolArgumentType.getBool(context, "enabled")))))));
    }

    private int show(CommandSourceStack source) {
        if (!sendStatus(source, true)) {
            source.sendSuccess(() -> woodenToolsStatus(config.woodenToolsOnly), false);
        }
        return 1;
    }

    private boolean sendStatus(CommandSourceStack source, boolean open) {
        var player = source.getPlayer();
        if (player == null || !player.connection.hasChannel(Status.TYPE)) {
            return false;
        }
        PacketDistributor.sendToPlayer(player, new Status(open, config.woodenToolsOnly));
        return true;
    }

    private int setWoodenToolsOnly(CommandSourceStack source, boolean enabled) {
        if (reloading) {
            source.sendFailure(Component.translatable("options.gstweaks.busy"));
            sendStatus(source, false);
            return 0;
        }
        boolean previous = config.woodenToolsOnly;
        if (previous == enabled) {
            source.sendSuccess(() -> woodenToolsStatus(enabled), false);
            sendStatus(source, false);
            return 1;
        }
        try {
            config.setWoodenToolsOnly(enabled);
        } catch (IOException | RuntimeException exception) {
            LogUtils.getLogger().warn("Could not save GSTweaks Hard Mode", exception);
            source.sendFailure(Component.translatable("options.gstweaks.save_failed"));
            sendStatus(source, false);
            return 0;
        }

        reloading = true;
        var server = source.getServer();
        source.sendSuccess(() -> Component.translatable("options.gstweaks.reloading"), false);
        server.reloadResources(server.getPackRepository().getSelectedIds()).whenCompleteAsync((unused, failure) -> {
            try {
                if (failure == null) {
                    source.sendSuccess(() -> woodenToolsStatus(enabled), true);
                } else {
                    LogUtils.getLogger().warn("Could not reload recipes for GSTweaks Hard Mode", failure);
                    config.woodenToolsOnly = previous;
                    try {
                        config.setWoodenToolsOnly(previous);
                    } catch (IOException | RuntimeException exception) {
                        LogUtils.getLogger().warn("Could not restore the previous GSTweaks configuration", exception);
                        source.sendFailure(Component.translatable("options.gstweaks.save_failed"));
                    }
                    source.sendFailure(Component.translatable("options.gstweaks.reload_failed"));
                }
            } finally {
                reloading = false;
                sendStatus(source, false);
            }
        }, server);
        return 1;
    }

    static Component woodenToolsStatus(boolean enabled) {
        return Component.translatable(enabled ? "options.gstweaks.enabled" : "options.gstweaks.disabled");
    }

    record Status(boolean open, boolean woodenToolsOnly) implements CustomPacketPayload {
        static final Type<Status> TYPE = new Type<>(Identifier.parse("gstweaks:options_status"));
        static final StreamCodec<RegistryFriendlyByteBuf, Status> CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, Status::open,
                ByteBufCodecs.BOOL, Status::woodenToolsOnly,
                Status::new);

        @Override
        public Type<Status> type() {
            return TYPE;
        }
    }
}
