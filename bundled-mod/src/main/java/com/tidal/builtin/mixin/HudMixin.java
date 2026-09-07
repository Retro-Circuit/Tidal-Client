package com.tidal.builtin.mixin;

import com.tidal.builtin.client.ArmorHud;
import com.tidal.builtin.client.KeystrokesHud;
import com.tidal.builtin.client.StatHuds;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class HudMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void tidal$keystrokes(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        KeystrokesHud.extract(graphics, Minecraft.getInstance());
        ArmorHud.extract(graphics, Minecraft.getInstance());
        StatHuds.extract(graphics, Minecraft.getInstance());
    }
}
