package com.tidal.builtin.mixin;

import com.tidal.builtin.client.CapeShare;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Inject(method = "handleCustomPayload", at = @At("HEAD"))
    private void tidal$cape(CustomPacketPayload payload, CallbackInfo ci) {
        if (payload instanceof DiscardedPayload discarded) {
            CapeShare.receive(discarded.id());
        }
    }
}
