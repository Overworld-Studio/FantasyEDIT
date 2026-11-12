package com.fantasy.edit.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C：服务端校验通过后，把权威行为树广播回所有客户端，保证全场表现一致。
 */
public record BehaviorSyncPayload(String json) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BehaviorSyncPayload> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("fantasy_edit", "behavior_sync"));

    public static final StreamCodec<ByteBuf, BehaviorSyncPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, BehaviorSyncPayload::json, BehaviorSyncPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
