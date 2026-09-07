package com.tidal.builtin.mixin;

import net.minecraft.client.ClientBrandRetriever;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ClientBrandRetriever.class, priority = 5000)
public class ClientBrandRetrieverMixin {
    @Inject(method = "getClientModName", at = @At("HEAD"), cancellable = true)
    private static void tidal$brandHead(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue("Tidal Client");
    }

    @Inject(method = "getClientModName", at = @At("RETURN"), cancellable = true)
    private static void tidal$brand(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue("Tidal Client");
    }
}
