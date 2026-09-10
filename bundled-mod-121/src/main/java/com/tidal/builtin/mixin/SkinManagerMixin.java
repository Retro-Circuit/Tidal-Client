package com.tidal.builtin.mixin;

import com.mojang.authlib.GameProfile;
import com.tidal.builtin.client.SkinArchive;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.SkinManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.function.Supplier;

@Mixin(SkinManager.class)
public class SkinManagerMixin {
    @Inject(
        method = "getInsecureSkin(Lcom/mojang/authlib/GameProfile;)Lnet/minecraft/client/resources/PlayerSkin;",
        at = @At("RETURN"),
        cancellable = true,
        require = 0
    )
    private void tidal$menuSkin(GameProfile profile, CallbackInfoReturnable<PlayerSkin> cir) {
        try {
            PlayerSkin next = SkinArchive.forMenu(cir.getReturnValue(), profile == null ? null : profile.getId());
            if (next != null) {
                cir.setReturnValue(next);
            }
        } catch (Throwable ignored) {
        }
    }

    @Inject(
        method = "lookupInsecure(Lcom/mojang/authlib/GameProfile;)Ljava/util/function/Supplier;",
        at = @At("RETURN"),
        cancellable = true,
        require = 0
    )
    private void tidal$menuLookup(GameProfile profile, CallbackInfoReturnable<Supplier<PlayerSkin>> cir) {
        wrap(profile, cir);
    }

    @Inject(
        method = "createLookup(Lcom/mojang/authlib/GameProfile;Z)Ljava/util/function/Supplier;",
        at = @At("RETURN"),
        cancellable = true,
        require = 0
    )
    private void tidal$menuCreate(GameProfile profile, boolean secure, CallbackInfoReturnable<Supplier<PlayerSkin>> cir) {
        wrap(profile, cir);
    }

    private static void wrap(GameProfile profile, CallbackInfoReturnable<Supplier<PlayerSkin>> cir) {
        try {
            Supplier<PlayerSkin> original = cir.getReturnValue();
            if (original == null) {
                return;
            }
            cir.setReturnValue(() -> SkinArchive.forMenu(original.get(), profile == null ? null : profile.getId()));
        } catch (Throwable ignored) {
        }
    }
}
