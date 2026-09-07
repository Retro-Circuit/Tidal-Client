package com.tidal.builtin.mixin;

import com.tidal.builtin.client.TidalBoost;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Options.class)
public class OptionsMixin {
    @Inject(method = "getCloudsType", at = @At("RETURN"), cancellable = true)
    private void tidal$fastClouds(CallbackInfoReturnable<CloudStatus> cir) {
        if (TidalBoost.fastClouds(cir.getReturnValue())) {
            cir.setReturnValue(CloudStatus.FAST);
        }
    }
}
