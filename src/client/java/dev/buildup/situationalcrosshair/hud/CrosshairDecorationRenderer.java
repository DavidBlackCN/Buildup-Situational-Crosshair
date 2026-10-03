package dev.buildup.situationalcrosshair.hud;

import dev.buildup.situationalcrosshair.presentation.PixelGlyph;
import dev.buildup.situationalcrosshair.presentation.TransitionController;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Fixed sidecar roles and drawing only. No semantic, world or held-item queries. */
public final class CrosshairDecorationRenderer {
    // 8px shadow envelopes with 2px canvas gaps; full envelope [-10,25) x [0,15).
    public static final int LEFT_X = -10, RIGHT_X = 17, SIDECAR_Y = 4;
    public static final int FOREGROUND = 0xf2eedf, SHADOW = 0x161714;
    public static final float ACTION_WEIGHT = 0.68f, STATE_WEIGHT = 0.52f, SHADOW_WEIGHT = 0.8f;
    private CrosshairDecorationRenderer() { }

    public static void draw(GuiGraphicsExtractor graphics, TransitionController.Frame frame, int x, int y) {
        var p = frame.presentation();
        if (!p.customVisible()) return;
        drawGlyph(graphics, PixelGlyph.state(p.leftSidecar()), x + LEFT_X + frame.leftOffset(), y + SIDECAR_Y, frame.leftOpacity() * STATE_WEIGHT);
        drawGlyph(graphics, PixelGlyph.action(p.rightSidecar()), x + RIGHT_X + frame.rightOffset(), y + SIDECAR_Y, frame.rightOpacity() * ACTION_WEIGHT);
    }
    private static void drawGlyph(GuiGraphicsExtractor graphics, PixelGlyph glyph, int x, int y, float opacity) {
        if (glyph == null) return;
        opacity = Math.clamp(opacity, 0, 1);
        int alpha = Math.round(255 * opacity), shadowAlpha = Math.round(255 * opacity * SHADOW_WEIGHT);
        if (alpha == 0) return;
        // One bottom/right shadow, no surrounding halo or repeated alpha blending.
        for (int row = 1; row <= PixelGlyph.SIZE; row++) for (int col = 1; col <= PixelGlyph.SIZE; col++) {
            if (glyph.at(col - 1, row - 1) && !glyph.at(col, row))
                graphics.fill(x + col, y + row, x + col + 1, y + row + 1, shadowAlpha << 24 | SHADOW);
        }
        for (int row = 0; row < PixelGlyph.SIZE; row++) for (int col = 0; col < PixelGlyph.SIZE; col++)
            if (glyph.at(col, row)) graphics.fill(x + col, y + row, x + col + 1, y + row + 1, alpha << 24 | FOREGROUND);
    }
}
