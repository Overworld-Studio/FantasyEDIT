package com.fantasy.edit.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * C2S：客户端点击「应用并注入」时把行为树推送到服务端。
 */
public record BehaviorPayload(String json) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BehaviorPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("fantasy_edit", "behavior"));

    public static final StreamCodec<ByteBuf, BehaviorPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, BehaviorPayload::json, BehaviorPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
