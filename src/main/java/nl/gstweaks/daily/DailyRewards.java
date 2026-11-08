package nl.gstweaks.daily;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class DailyRewards {
    private static final long NEVER = Long.MIN_VALUE;
    // Keep the old claim ID when replacing GSMod-DailyRewards, to preserve the last claim day.
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, "gsmod_daily");
    private static final Supplier<AttachmentType<Progress>> DATA = ATTACHMENTS.register("daily_reward",
            () -> AttachmentType.builder(() -> new Progress(NEVER, NEVER, 0))
                    .serialize(Progress.CODEC).copyOnDeath().build());

    private record Progress(long lastClaimDay, long playDay, long playedMillis) {
        private static final MapCodec<Progress> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
                Codec.LONG.optionalFieldOf("last_claim_day", NEVER).forGetter(Progress::lastClaimDay),
                Codec.LONG.optionalFieldOf("play_day", NEVER).forGetter(Progress::playDay),
                Codec.LONG.optionalFieldOf("played_millis", 0L).forGetter(Progress::playedMillis)
        ).apply(builder, Progress::new));
    }

    private final DailyConfig config = DailyConfig.load();
    private final Map<UUID, Long> online = new HashMap<>();

    public static void register(IEventBus modBus) {
        var daily = new DailyRewards();
        modBus.addListener(DailyRewards::registerPayload);
        if (ModList.get().isLoaded("gsmod_daily")) {
            return;
        }
        ATTACHMENTS.register(modBus);
        var bus = NeoForge.EVENT_BUS;
        bus.addListener(daily::registerCommands);
        if (daily.config.enabled) {
            bus.addListener(daily::onLogin);
            bus.addListener(daily::onLogout);
            bus.addListener(daily::onTick);
            bus.<ServerStoppedEvent>addListener(event -> daily.online.clear());
        }
    }

    private static void registerPayload(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(DailyStatus.TYPE, DailyStatus.CODEC);
    }

    private void registerCommands(RegisterCommandsEvent event) {
        var daily = Commands.literal("daily")
                .executes(context -> sendStatus(context.getSource().getPlayerOrException(), true))
                .then(Commands.literal("status")
                        .executes(context -> sendStatus(context.getSource().getPlayerOrException(), false)))
                .then(Commands.literal("claim")
                        .executes(context -> claim(context.getSource().getPlayerOrException())));
        daily.then(Commands.literal("admin")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("timezone")
                        .executes(context -> {
                            context.getSource().sendSuccess(() -> Component.translatable(
                                    "daily.gstweaks.timezone_current", zoneLabel()), false);
                            return 1;
                        })
                        .then(Commands.literal(DailyConfig.AUTO)
                                .executes(context -> setZone(context.getSource(), DailyConfig.AUTO)))
                        .then(Commands.argument("zone", StringArgumentType.greedyString())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        ZoneId.getAvailableZoneIds(), builder))
                                .executes(context -> setZone(context.getSource(),
                                        StringArgumentType.getString(context, "zone").trim())))));
        daily.then(Commands.literal("debug")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("player", EntityArgument.player()).executes(context -> {
                    var player = EntityArgument.getPlayer(context, "player");
                    var state = sample(player, Instant.now());
                    context.getSource().sendSuccess(() -> Component.literal(
                            player.getScoreboardName() + " | lastClaimDay=" + state.lastClaimDay()
                                    + " | playDay=" + state.playDay()
                                    + " | playedSeconds=" + state.playedMillis() / 1000
                                    + " | requiredMinutes=" + config.minimumOnlineMinutes), false);
                    return 1;
                })));
        event.getDispatcher().register(Commands.literal("gstweaks").then(daily));
    }

    private int setZone(CommandSourceStack source, String name) {
        try {
            config.setZone(name);
        } catch (DateTimeException exception) {
            source.sendFailure(Component.translatable("daily.gstweaks.timezone_invalid", name));
            return 0;
        } catch (IOException | com.google.gson.JsonParseException | IllegalStateException exception) {
            LogUtils.getLogger().warn("Could not update the GSTweaks Daily Rewards time zone", exception);
            source.sendFailure(Component.translatable("daily.gstweaks.timezone_error"));
            return 0;
        }
        online.clear();
        source.sendSuccess(() -> Component.translatable("daily.gstweaks.timezone_updated", zoneLabel()), true);
        return 1;
    }

    private Component zoneLabel() {
        String zone = config.zone().getId();
        return config.automaticZone() ? Component.translatable("daily.gstweaks.timezone_auto", zone)
                : Component.literal(zone);
    }

    private void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        online.put(event.getEntity().getUUID(), System.nanoTime());
    }

    private void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sample(player, Instant.now());
            online.remove(player.getUUID());
        }
    }

    private void onTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 20 == 0) {
            var now = Instant.now();
            event.getServer().getPlayerList().getPlayers().forEach(player -> sample(player, now));
        }
    }

    private Progress sample(ServerPlayer player, Instant now) {
        var progress = player.getData(DATA);
        if (!config.enabled) {
            return progress;
        }
        long day = day(now);
        long tick = System.nanoTime();
        var previous = online.put(player.getUUID(), tick);
        long elapsed = previous == null ? 0 : TimeUnit.NANOSECONDS.toMillis(tick - previous);
        long sinceReset = Math.max(0, Duration.between(startOf(day), now).toMillis());
        long played = progress.playDay() == day ? progress.playedMillis() : 0;
        var result = new Progress(progress.lastClaimDay(), day,
                Math.min(sinceReset, played + Math.min(Math.max(0, elapsed), sinceReset)));
        if (!result.equals(progress)) {
            player.setData(DATA, result);
        }
        return result;
    }

    private long day(Instant now) {
        long localDay = now.atZone(config.zone()).toLocalDate().toEpochDay();
        return now.isBefore(startOf(localDay)) ? localDay - 1 : localDay;
    }

    private Instant startOf(long day) {
        return LocalDate.ofEpochDay(day).atTime(config.resetHour, 0).atZone(config.zone()).toInstant();
    }

    private long requiredMillis() {
        return TimeUnit.MINUTES.toMillis(config.minimumOnlineMinutes);
    }

    private String availability(Progress progress, long day) {
        if (!config.enabled) return "disabled";
        if (progress.lastClaimDay() >= day) return "claimed";
        return progress.playedMillis() < requiredMillis() ? "waiting" : "ready";
    }

    private int sendStatus(ServerPlayer player, boolean open) {
        var now = Instant.now();
        var progress = sample(player, now);
        var day = day(now);
        var available = availability(progress, day);
        var message = Component.translatable("daily.gstweaks." + available);
        var played = Component.translatable("daily.gstweaks.playtime",
                format(progress.playedMillis() / 1000), format(requiredMillis() / 1000));
        var reset = Component.translatable("daily.gstweaks.reset",
                format(Math.max(0, Duration.between(now, startOf(day + 1)).toSeconds())), config.zone().getId());
        if (player.connection.hasChannel(DailyStatus.TYPE)) {
            PacketDistributor.sendToPlayer(player, new DailyStatus(open, available.equals("ready"), message, played, reset));
        } else {
            player.sendSystemMessage(message.copy().append(" ").append(played));
        }
        return 1;
    }

    private int claim(ServerPlayer player) {
        var now = Instant.now();
        var progress = sample(player, now);
        var day = day(now);
        var available = availability(progress, day);
        if (!available.equals("ready")) {
            player.sendSystemMessage(Component.translatable("daily.gstweaks." + available));
            sendStatus(player, false);
            return 0;
        }
        List<ItemStack> rewards = config.rewards.stream().map(reward -> {
            var item = BuiltInRegistries.ITEM.getValue(Identifier.parse(reward.item()));
            return new ItemStack(item, reward.count());
        }).filter(stack -> !stack.isEmpty()).toList();
        if (rewards.isEmpty() && config.experience == 0) {
            player.sendSystemMessage(Component.translatable("daily.gstweaks.no_rewards"));
            return 0;
        }
        player.setData(DATA, new Progress(day, day, progress.playedMillis()));
        for (var stack : rewards) {
            if (!player.getInventory().add(stack) && !stack.isEmpty()) {
                player.drop(stack, false, false);
            }
        }
        if (config.experience > 0) {
            player.giveExperiencePoints(config.experience);
        }
        player.containerMenu.broadcastChanges();
        player.sendSystemMessage(Component.translatable("daily.gstweaks.success"));
        sendStatus(player, false);
        return 1;
    }

    private static String format(long seconds) {
        return String.format(Locale.ROOT, "%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60);
    }
}
