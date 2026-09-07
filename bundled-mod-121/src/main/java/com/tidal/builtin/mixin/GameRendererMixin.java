package com.tidal.builtin.mixin;

import com.tidal.builtin.client.PlayFeatures;
import com.tidal.builtin.client.TidalMods;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void tidal$noHurtCam(CallbackInfo ci) {
        if (TidalMods.noHurtCam) {
            ci.cancel();
        }
    }

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true, require = 0)
    private void tidal$zoomFloat(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(PlayFeatures.zoomFov(cir.getReturnValueF()));
    }

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true, require = 0)
    private void tidal$zoomDouble(CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(PlayFeatures.zoomFov(cir.getReturnValueD()));
    }
}
