package com.tidal.builtin.mixin;

import com.tidal.builtin.client.AccountCapes;
import com.tidal.builtin.client.CapeShare;
import com.tidal.builtin.client.Capes;
import com.tidal.builtin.client.LocalSkins;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.player.PlayerSkin;
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
            ClientAsset.Texture cape = Capes.equippedAsset();
            ClientAsset.Texture body = LocalSkins.bodyAsset();
            if (cape == null && body == null) {
                return;
            }
            cir.setReturnValue(new PlayerSkin(
                body != null ? body : skin.body(),
                cape != null ? cape : skin.cape(),
                skin.elytra(),
                LocalSkins.modelOr(skin.model()),
                skin.secure()
            ));
            return;
        }
        ClientAsset.Texture tidal = CapeShare.asset(self.getUUID());
        if (tidal != null) {
            cir.setReturnValue(new PlayerSkin(skin.body(), tidal, skin.elytra(), skin.model(), skin.secure()));
            return;
        }
        if (skin.cape() != null) {
            return;
        }
        ClientAsset.Texture shared = AccountCapes.other(self.getUUID());
        if (shared != null) {
            cir.setReturnValue(new PlayerSkin(skin.body(), shared, skin.elytra(), skin.model(), skin.secure()));
        }
    }
}
