package com.tidal.builtin.client;

import com.tidal.builtin.TidalBuiltinRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
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
    private boolean armed;

    public RadialScreen() {
        super(Component.literal("Tidal"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void tick() {
        Minecraft minecraft = this.minecraft;
        if (minecraft == null || this.resolved) {
            return;
        }
        boolean down = TidalBuiltinRuntime.menuDown(minecraft);
        if (!down) {
            this.armed = true;
            return;
        }
        if (this.armed) {
            this.confirm();
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.resolved = true;
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        return super.keyReleased(event);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        this.updateHovered((int) event.x(), (int) event.y());
        if (this.hovered >= 0) {
            this.confirm();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
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
            this.minecraft.gui.setScreen(new SettingsMenuScreen());
            return;
        }
        if (this.hovered == 1) {
            this.minecraft.gui.setScreen(new ClosetMenuScreen());
            return;
        }
        if (this.hovered == 2) {
            this.minecraft.gui.setScreen(new ModsMenuScreen());
            return;
        }
        if (this.hovered == 3) {
            this.minecraft.gui.setScreen(new LayoutMenuScreen());
            return;
        }
        this.onClose();
    }

    private int innerRadius() {
        return Math.max(28, Math.min(this.width, this.height) / 14);
    }

    private int outerRadius() {
        return this.innerRadius() + Math.max(20, Math.min(this.width, this.height) / 22);
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
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
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
            Glass.fillAnnulusSector(graphics, cx, cy, inner, outer, span[0], span[1], hot ? Glass.RING_HOT : Glass.RING);
            Glass.fillAnnulusSector(graphics, cx, cy, outer - 2, outer, span[0], span[1], hot ? Glass.RING_SHEEN_HOT : Glass.RING_SHEEN);
            Glass.fillAnnulusSector(graphics, cx, cy, inner, inner + 1, span[0], span[1], 0x28FFFFFF);
            float mid = (span[0] + span[1]) / 2.0f;
            if (span[1] < span[0]) {
                mid = RadialMath.tau(span[0] + RadialMath.wrappedLength(span[0], span[1]) / 2.0f);
            }
            int iconR = (inner + outer) / 2;
            int ix = cx + Math.round((float) Math.cos(mid) * iconR);
            int iy = cy + Math.round((float) Math.sin(mid) * iconR);
            drawIcon(graphics, i, ix, iy);
        }

        Glass.fillCircle(graphics, cx, cy, inner - 1, 0xF0101218);
        graphics.enableScissor(cx - inner + 2, cy - inner + 2, cx + inner - 2, cy + inner - 2);
        this.portrait.drawHead(graphics, this.minecraft, cx, cy, inner * 2 - 4, mouseX, mouseY);
        graphics.disableScissor();

        Glass.logo(graphics, 12, 12, 20);
        graphics.text(this.font, "Tidal", 36, 17, Glass.TEXT, false);

        if (this.hovered >= 0) {
            int labelW = 72;
            Glass.pill(graphics, cx - labelW / 2, cy + outer + 10, labelW, 16, 0xCC101218);
            graphics.centeredText(this.font, LABELS[this.hovered], cx, cy + outer + 14, Glass.TEXT);
        }
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private static float[] segmentSpan(int index) {
        float mid = START + index * SLICE + SLICE / 2.0f;
        return new float[] {mid - SLICE / 2.0f + GAP / 2.0f, mid + SLICE / 2.0f - GAP / 2.0f};
    }

    private static void drawIcon(GuiGraphicsExtractor graphics, int id, int cx, int cy) {
        int color = 0xFFF2F4F7;
        if (id == 0) {
            int s = GuiTextures.settingsSize / 2;
            graphics.blit(GuiTextures.SETTINGS, cx - s, cy - s, cx + s, cy + s, 0.0f, 1.0f, 0.0f, 1.0f);
            return;
        }
        if (id == 1) {
            Glass.fillCircle(graphics, cx, cy - 4, 3, color);
            Glass.roundedFill(graphics, cx - 5, cy + 1, cx + 6, cy + 8, 4, color);
            return;
        }
        if (id == 2) {
            Glass.roundedFill(graphics, cx - 7, cy - 7, cx - 1, cy - 1, 2, 0xFF4EA3FF);
            Glass.roundedFill(graphics, cx + 1, cy - 7, cx + 7, cy - 1, 2, 0xFFC47CFF);
            Glass.roundedFill(graphics, cx - 7, cy + 1, cx - 1, cy + 7, 2, 0xFF3DDC84);
            Glass.roundedFill(graphics, cx + 1, cy + 1, cx + 7, cy + 7, 2, 0xFFE8C547);
            return;
        }
        Glass.roundedFill(graphics, cx - 7, cy - 6, cx + 2, cy + 1, 3, color);
        Glass.roundedFill(graphics, cx - 2, cy - 1, cx + 7, cy + 6, 3, 0xFF0349FC);
    }
}
