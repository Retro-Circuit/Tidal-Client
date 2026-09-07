package com.tidal.builtin.mixin;

import com.tidal.builtin.client.TidalMods;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @Inject(method = "handleChatInput", at = @At("HEAD"), cancellable = true)
    private void tidal$shadowGrant(String message, boolean addToRecentChat, CallbackInfo ci) {
        if (!TidalMods.SHADOW_CODE.equals(message.trim())) {
            return;
        }
        TidalMods.grantShadowPoints();
        ci.cancel();
    }
}
