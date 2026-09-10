package com.tidal.builtin.mixin;

import com.mojang.authlib.GameProfile;
import com.tidal.builtin.client.SkinArchive;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.function.Supplier;

@Mixin(SkinManager.class)
public class SkinManagerMixin {
    @Inject(
        method = "createLookup(Lcom/mojang/authlib/GameProfile;Z)Ljava/util/function/Supplier;",
        at = @At("RETURN"),
        cancellable = true,
        require = 0
    )
    private void tidal$menuCreate(GameProfile profile, boolean secure, CallbackInfoReturnable<Supplier<PlayerSkin>> cir) {
        wrap(profile, cir);
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

    private static void wrap(GameProfile profile, CallbackInfoReturnable<Supplier<PlayerSkin>> cir) {
        Supplier<PlayerSkin> original = cir.getReturnValue();
        if (original == null) {
            return;
        }
            cir.setReturnValue(() -> SkinArchive.forMenu(original.get(), SkinArchive.idOf(profile)));
    }
}
