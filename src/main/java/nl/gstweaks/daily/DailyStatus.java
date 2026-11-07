package nl.gstweaks.daily;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record DailyStatus(boolean open, boolean canClaim, Component message, Component progress,
                          Component reset) implements CustomPacketPayload {
    public static final Type<DailyStatus> TYPE = new Type<>(Identifier.parse("gstweaks:daily_status"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DailyStatus> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, DailyStatus::open,
            ByteBufCodecs.BOOL, DailyStatus::canClaim,
            ComponentSerialization.STREAM_CODEC, DailyStatus::message,
            ComponentSerialization.STREAM_CODEC, DailyStatus::progress,
            ComponentSerialization.STREAM_CODEC, DailyStatus::reset,
            DailyStatus::new);

    @Override
    public Type<DailyStatus> type() {
        return TYPE;
    }
}
