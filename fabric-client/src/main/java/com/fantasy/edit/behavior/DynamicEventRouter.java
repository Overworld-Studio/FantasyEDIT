package com.fantasy.edit.behavior;

import com.fantasy.edit.FantasyEdit;
import com.fantasy.edit.definition.ItemDefinition;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/**
 * 动态事件总线：整个进程只注册一次 Fabric 回调，
 * 具体规则在事件发生时从定义仓库实时读取，因此 GUI 保存后立即生效。
 */
public final class DynamicEventRouter {

    private DynamicEventRouter() {
    }

    public static void register() {
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (level.isClientSide()) {
                dispatch(player, hand, TriggerType.ON_RIGHT_CLICK);
            }
            return InteractionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (level.isClientSide()) {
                ItemStack stack = player.getItemInHand(hand);
                if (stack.getItem() instanceof BlockItem) {
                    dispatch(player, hand, TriggerType.ON_BLOCK_PLACE);
                }
            }
            return InteractionResult.PASS;
        });

        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (level.isClientSide()) {
                dispatch(player, hand, TriggerType.ON_ENTITY_HIT);
            }
            return InteractionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && !client.player.isSpectator()) {
                dispatch(client.player, InteractionHand.MAIN_HAND, TriggerType.ON_TICK);
            }
        });
    }

    private static void dispatch(Player player, InteractionHand hand, TriggerType trigger) {
        ItemDefinition definition = definitionOf(player.getItemInHand(hand));
        if (definition != null) {
            ClientBehaviorRuntime.fire(definition, trigger, player);
        }
    }

    public static ItemDefinition definitionOf(ItemStack stack) {
        if (stack == null || stack.isEmpty() || FantasyEdit.store() == null) {
            return null;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) {
            return null;
        }
        ItemDefinition definition = FantasyEdit.store().get(id.toString());
        if (definition == null || definition.behaviors == null || definition.behaviors.isEmpty()) {
            return null;
        }
        return definition;
    }
}
