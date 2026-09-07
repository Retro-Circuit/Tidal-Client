package com.tidal.builtin.mixin;

import com.tidal.builtin.TidalBuiltin;
import com.tidal.builtin.client.TidalBoost;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void tidal$begin(CallbackInfo ci) {
        TidalBoost.beginTick();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tidal$tick(CallbackInfo ci) {
        TidalBuiltin.tick((Minecraft) (Object) this);
    }
}
