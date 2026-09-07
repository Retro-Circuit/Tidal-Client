package com.tidal.builtin.mixin;

import com.tidal.builtin.client.TidalBoost;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {
    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void tidal$cap(Particle particle, CallbackInfo ci) {
        if (TidalBoost.dropParticle()) {
            ci.cancel();
        }
    }
}
