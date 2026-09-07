package com.tidal.builtin.mixin;

import com.mojang.blaze3d.platform.FramerateLimitTracker;
import com.tidal.builtin.client.TidalBoost;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FramerateLimitTracker.class)
public class FramerateLimitTrackerMixin {
    @Inject(method = "getFramerateLimit", at = @At("RETURN"), cancellable = true)
    private void tidal$unfocused(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(TidalBoost.unfocusedLimit(cir.getReturnValue()));
    }
}
