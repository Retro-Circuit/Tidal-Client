package com.tidal.builtin.client;

import com.tidal.builtin.TidalBuiltin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class RadialScreen extends Screen {
    static final String[] LABELS = {"Settings", "Skins", "Mods", "Layout"};
    private static final float SLICE = (float) (Math.PI * 2.0 / 4.0);
    private static final float GAP = 0.2f;
    private static final float START = (float) (-Math.PI / 2.0 - SLICE / 2.0);

    private final PlayerPortrait portrait = new PlayerPortrait();
    private int hovered = -1;
    private boolean resolved;
    private int holdTicks;

    public RadialScreen() {
        super(Component.literal("Tidal"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void tick() {
        Minecraft minecraft = this.minecraft;
        if (minecraft == null || this.resolved) {
            return;
        }
        boolean down = TidalBuiltin.menuDown(minecraft);
        if (down) {
            this.holdTicks++;
            return;
        }
        if (this.holdTicks >= 4) {
            this.confirm();
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            this.resolved = true;
            this.onClose();
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.updateHovered((int) mouseX, (int) mouseY);
        if (this.hovered >= 0) {
            this.confirm();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        this.updateHovered((int) mouseX, (int) mouseY);
        super.mouseMoved(mouseX, mouseY);
    }

    private void confirm() {
        if (this.resolved) {
            return;
        }
        this.resolved = true;
        if (this.minecraft == null) {
            return;
        }
        if (this.hovered == 0) {
            this.minecraft.setScreen(new SettingsMenuScreen());
            return;
        }
        if (this.hovered == 1) {
            this.minecraft.setScreen(new ClosetMenuScreen());
            return;
        }
        if (this.hovered == 2) {
            this.minecraft.setScreen(new ModsMenuScreen());
            return;
        }
        if (this.hovered == 3) {
            this.minecraft.setScreen(new LayoutMenuScreen());
            return;
        }
        this.onClose();
    }

    private int innerRadius() {
        return Math.max(34, Math.min(this.width, this.height) / 12);
    }

    private int outerRadius() {
        return this.innerRadius() + Math.max(26, Math.min(this.width, this.height) / 18);
    }

    private void updateHovered(int mouseX, int mouseY) {
        int cx = this.width / 2;
        int cy = this.height / 2;
        int dx = mouseX - cx;
        int dy = mouseY - cy;
        int distSq = dx * dx + dy * dy;
        int inner = this.innerRadius();
        int outer = this.outerRadius();
        this.hovered = -1;
        if (distSq < inner * inner || distSq > outer * outer) {
            return;
        }
        float angle = (float) Math.atan2(dy, dx);
        for (int i = 0; i < LABELS.length; i++) {
            float[] span = segmentSpan(i);
            if (RadialMath.inArc(angle, span[0], span[1])) {
                this.hovered = i;
                return;
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.updateHovered(mouseX, mouseY);
        int cx = this.width / 2;
        int cy = this.height / 2;
        int inner = this.innerRadius();
        int outer = this.outerRadius();

        Glass.scrim(graphics, this.width, this.height);
        GuiTextures.ensure(this.minecraft);

        for (int i = 0; i < LABELS.length; i++) {
            float[] span = segmentSpan(i);
            boolean hot = this.hovered == i;
            float mid = (span[0] + span[1]) / 2.0f;
            if (span[1] < span[0]) {
                mid = RadialMath.tau(span[0] + RadialMath.wrappedLength(span[0], span[1]) / 2.0f);
            }
            int iconR = (inner + outer) / 2;
            int ix = cx + Math.round((float) Math.cos(mid) * iconR);
            int iy = cy + Math.round((float) Math.sin(mid) * iconR);
            GuiTextures.blitRadial(graphics, i, ix, iy, Math.max(36, (outer - inner) * 2 + 8), hot);
        }

        Glass.fillCircle(graphics, cx, cy, inner - 1, 0xF0101218);
        graphics.enableScissor(cx - inner + 2, cy - inner + 2, cx + inner - 2, cy + inner - 2);
        this.portrait.drawHead(graphics, this.minecraft, cx, cy, inner * 2 - 4, mouseX, mouseY);
        graphics.disableScissor();

        Glass.logo(graphics, 14, 14, 22);
        graphics.drawString(this.font, "TIDAL", 42, 16, Glass.TEXT, false);
        graphics.drawString(this.font, "Hold Alt  ·  release on a slice", 42, 27, Glass.MUTE, false);

        if (this.hovered >= 0) {
            int labelW = 88;
            Glass.pill(graphics, cx - labelW / 2, cy + outer + 14, labelW, 18, 0xE0101218);
            graphics.drawCenteredString(this.font, LABELS[this.hovered], cx, cy + outer + 18, Glass.TEXT);
        }
        super.render(graphics, mouseX, mouseY, delta);
    }

    private static float[] segmentSpan(int index) {
        float mid = START + index * SLICE + SLICE / 2.0f;
        return new float[] {mid - SLICE / 2.0f + GAP / 2.0f, mid + SLICE / 2.0f - GAP / 2.0f};
    }
}
