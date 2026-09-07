package com.tidal.builtin.mixin;

import com.tidal.builtin.client.PlayMods;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.components.BossHealthOverlay")
public class BossBarHideMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void tidal$hide(CallbackInfo ci) {
        if (PlayMods.hideBossBar) {
            ci.cancel();
        }
    }
}
