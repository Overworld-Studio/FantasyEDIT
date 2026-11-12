package com.fantasy.edit.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C：服务端执行结果回执（改了多少格、是否被拒绝）。
 */
public record BuildAckPayload(String json) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BuildAckPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("fantasy_edit", "build_ack"));

    public static final StreamCodec<ByteBuf, BuildAckPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, BuildAckPayload::json, BuildAckPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
