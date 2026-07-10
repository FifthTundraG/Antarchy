package com.craisinlord.antarchy.content.network;

import com.craisinlord.antarchy.Antarchy;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** C2S: the rider pressed the roll keybind — toggle the rolly polly's wheel mode. */
public record RollyPollyRollPayload() implements CustomPacketPayload {
    public static final Type<RollyPollyRollPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Antarchy.MODID, "rolly_polly_roll"));
    public static final StreamCodec<ByteBuf, RollyPollyRollPayload> STREAM_CODEC =
            StreamCodec.unit(new RollyPollyRollPayload());

    @Override
    public Type<RollyPollyRollPayload> type() {
        return TYPE;
    }
}
