package com.fantasy.edit.client.mixin;

import com.fantasy.edit.client.gui.StudioScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在创造模式物品栏右上角放一个“虚空造物”按钮。
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin {

    @Shadow
    protected int leftPos;

    @Shadow
    protected int topPos;

    @Shadow
    @Final
    protected int imageWidth;

    @Shadow
    protected abstract <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T widget);

    @Inject(method = "init", at = @At("TAIL"))
    private void fantasyItem$addStudioButton(CallbackInfo ci) {
        Button button = Button.builder(Component.literal("虚空造物"), pressed ->
                        Minecraft.getInstance().gui.setScreen(new StudioScreen()))
                .bounds(this.leftPos + this.imageWidth + 6, this.topPos + 4, 84, 20)
                .build();
        this.addRenderableWidget(button);
    }
}
