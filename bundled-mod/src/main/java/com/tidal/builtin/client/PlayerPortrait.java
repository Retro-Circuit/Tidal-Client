package com.tidal.builtin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

final class PlayerPortrait {
    private Model.Simple head;
    private Model.Simple wide;
    private Model.Simple slim;
    private float yaw;
    private float pitch;
    private boolean looking;

    void drawHead(GuiGraphicsExtractor graphics, Minecraft minecraft, int cx, int cy, int size, int mouseX, int mouseY) {
        PlayerSkin skin = resolveSkin(minecraft);
        if (skin == null) {
            return;
        }
        this.ensure(minecraft);
        Identifier texture = skin.body().texturePath();
        float[] look = look(cx, cy, mouseX, mouseY);
        if (!this.looking) {
            this.yaw = look[0];
            this.pitch = look[1];
            this.looking = true;
        } else {
            this.yaw += (look[0] - this.yaw) * 0.18f;
            this.pitch += (look[1] - this.pitch) * 0.18f;
        }
        int side = Math.max(32, Math.round(size * 1.5f));
        int x0 = cx - side / 2;
        int y0 = cy - Math.round(side * 0.18f);
        float scale = 0.97f * side / 2.125f;
        graphics.skin(this.head, texture, scale, -this.pitch, this.yaw, -1.0625f, x0, y0, x0 + side, y0 + side);
    }

    void drawBody(GuiGraphicsExtractor graphics, Minecraft minecraft, int x0, int y0, int x1, int y1, int mouseX, int mouseY) {
        PlayerSkin skin = resolveSkin(minecraft);
        if (skin == null) {
            return;
        }
        this.ensure(minecraft);
        Model.Simple model = skin.model() == PlayerModelType.SLIM ? this.slim : this.wide;
        int cx = (x0 + x1) / 2;
        int cy = (y0 + y1) / 2;
        float yaw = 30.0f + (float) Math.toDegrees(Math.atan((mouseX - cx) / 40.0));
        float pitch = -5.0f + (float) Math.toDegrees(Math.atan((mouseY - cy) / 40.0));
        float scale = 0.97f * (y1 - y0) / 2.125f;
        graphics.skin(model, skin.body().texturePath(), scale, pitch, yaw, -1.0625f, x0, y0, x1, y1);
    }

    private void ensure(Minecraft minecraft) {
        if (this.head != null) {
            return;
        }
        var models = minecraft.getEntityModels();
        this.head = new Model.Simple(models.bakeLayer(ModelLayers.PLAYER_HEAD), texture -> RenderTypes.entityCutout(texture));
        this.wide = new Model.Simple(models.bakeLayer(ModelLayers.PLAYER), texture -> RenderTypes.entityCutout(texture));
        this.slim = new Model.Simple(models.bakeLayer(ModelLayers.PLAYER_SLIM), texture -> RenderTypes.entityCutout(texture));
    }

    private static float[] look(int cx, int cy, int mouseX, int mouseY) {
        float dx = mouseX - cx;
        float dy = mouseY - cy;
        float yaw = 20.0f * (float) Math.tanh(dx / 72.0);
        float pitch = 30.0f * (float) Math.tanh(dy / 56.0);
        return new float[] {yaw, pitch};
    }

    private static PlayerSkin resolveSkin(Minecraft minecraft) {
        if (minecraft.player instanceof AbstractClientPlayer player) {
            return player.getSkin();
        }
        try {
            var user = minecraft.getUser();
            var profile = new com.mojang.authlib.GameProfile(user.getProfileId(), user.getName());
            return minecraft.getSkinManager().createLookup(profile, true).get();
        } catch (Exception ignored) {
            return null;
        }
    }
}
