package com.tidal.builtin.mixin;

import com.tidal.builtin.client.PlayMods;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class OverlayHideMixin {
    @Inject(method = "displayScoreboardSidebar", at = @At("HEAD"), cancellable = true, require = 0)
    private void tidal$hideScoreboard(CallbackInfo ci) {
        if (PlayMods.hideScoreboard) {
            ci.cancel();
        }
    }

    @Inject(method = "renderScoreboardSidebar", at = @At("HEAD"), cancellable = true, require = 0)
    private void tidal$hideScoreboardRender(CallbackInfo ci) {
        if (PlayMods.hideScoreboard) {
            ci.cancel();
        }
    }

    @Inject(method = "renderVignette", at = @At("HEAD"), cancellable = true, require = 0)
    private void tidal$noVignette(CallbackInfo ci) {
        if (PlayMods.noVignette) {
            ci.cancel();
        }
    }

    @Inject(method = "renderTextureOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private void tidal$pumpkin(CallbackInfo ci) {
        if (PlayMods.hidePumpkin) {
            ci.cancel();
        }
    }
}
