package dev.buildup.situationalcrosshair.hud;

import dev.buildup.situationalcrosshair.presentation.CrosshairPresentation;
import dev.buildup.situationalcrosshair.presentation.PixelGlyph;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** One embedded micro hint; no world, player or semantic queries. */
public final class CrosshairDecorationRenderer {
    // 3x3 foreground + one-sided shadow occupies [11,14] within the old 15x15 canvas.
    public static final int ACTION_X = 11, ACTION_Y = 11;
    private CrosshairDecorationRenderer() { }

    public static void draw(GuiGraphicsExtractor graphics, CrosshairPresentation presentation, int x, int y, float opacity) {
        var glyph = PixelGlyph.modifier(presentation.modifier());
        if (glyph == null || !presentation.customVisible()) return;
        int alpha = Math.round(255 * Math.clamp(opacity, 0, 1));
        if (alpha == 0) return;
        x += ACTION_X;
        y += ACTION_Y;
        // A bottom/right drop shadow, not a halo. Submit each pixel only once,
        // so the fade does not accumulate alpha where foreground and shadow meet.
        for (int row = 1; row <= PixelGlyph.SIZE; row++) for (int col = 1; col <= PixelGlyph.SIZE; col++) {
            if (glyph.at(col - 1, row - 1) && !glyph.at(col, row))
                graphics.fill(x + col, y + row, x + col + 1, y + row + 1, alpha << 24 | 0x101010);
        }
        for (int row = 0; row < PixelGlyph.SIZE; row++) for (int col = 0; col < PixelGlyph.SIZE; col++) {
            if (glyph.at(col, row)) graphics.fill(x + col, y + row, x + col + 1, y + row + 1, alpha << 24 | 0xf4f4f4);
        }
    }
}
