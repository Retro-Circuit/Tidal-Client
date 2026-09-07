package com.tidal.builtin.mixin;

import com.tidal.builtin.client.MenuKeys;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
    @Inject(method = "keyPress(JIIII)V", at = @At("HEAD"), require = 0)
    private void tidal$keyOld(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        MenuKeys.onKey(key, action);
    }

    @Inject(method = "keyPress", at = @At("HEAD"), require = 0)
    private void tidal$key(long window, Object event, CallbackInfo ci) {
        try {
            int key = (int) event.getClass().getMethod("key").invoke(event);
            int action = 1;
            try {
                action = (int) event.getClass().getMethod("action").invoke(event);
            } catch (ReflectiveOperationException ignored) {
            }
            MenuKeys.onKey(key, action);
        } catch (Throwable ignored) {
        }
    }
}
