package com.tidal.builtin.mixin;

import com.tidal.builtin.client.TidalBoost;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.client.renderer.state.level.WeatherRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WeatherEffectRenderer.class)
public class WeatherEffectRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void tidal$weather(Vec3 camera, WeatherRenderState state, CallbackInfo ci) {
        if (TidalBoost.skipWeather()) {
            ci.cancel();
        }
    }
}
