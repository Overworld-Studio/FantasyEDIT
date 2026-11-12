package com.fantasy.item.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * C2S：客户端把一次建造操作推送给 Paper 服务端做权威落盘。
 */
public record BuildPayload(String json) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BuildPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("fantasy_item", "build"));

    public static final StreamCodec<ByteBuf, BuildPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, BuildPayload::json, BuildPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
