package com.tidal.builtin.mixin;

import com.tidal.builtin.client.AccountCapes;
import com.tidal.builtin.client.CapeShare;
import com.tidal.builtin.client.Capes;
import com.tidal.builtin.client.LocalSkins;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public class PlayerSkinMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void tidal$cape(CallbackInfoReturnable<PlayerSkin> cir) {
        AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }
        Capes.ensure(minecraft);
        LocalSkins.ensure(minecraft);
        PlayerSkin skin = cir.getReturnValue();
        if (minecraft.player == self) {
            ResourceLocation cape = Capes.equippedLocation();
            ResourceLocation body = LocalSkins.bodyLocation();
            if (cape == null && body == null) {
                return;
            }
            cir.setReturnValue(new PlayerSkin(
                body != null ? body : skin.texture(),
                skin.textureUrl(),
                cape != null ? cape : skin.capeTexture(),
                skin.elytraTexture(),
                LocalSkins.modelOr(skin.model()),
                skin.secure()
            ));
            return;
        }
        ResourceLocation tidal = CapeShare.location(self.getUUID());
        if (tidal != null) {
            cir.setReturnValue(new PlayerSkin(skin.texture(), skin.textureUrl(), tidal, skin.elytraTexture(), skin.model(), skin.secure()));
            return;
        }
        if (skin.capeTexture() != null) {
            return;
        }
        ResourceLocation shared = AccountCapes.other(self.getUUID());
        if (shared != null) {
            cir.setReturnValue(new PlayerSkin(skin.texture(), skin.textureUrl(), shared, skin.elytraTexture(), skin.model(), skin.secure()));
        }
    }
}
