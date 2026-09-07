package com.tidal.builtin.mixin;

import com.tidal.builtin.client.Glass;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void tidal$logo(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        int size = 22;
        Glass.logo(graphics, this.width - size - 10, 10, size);
    }
}
