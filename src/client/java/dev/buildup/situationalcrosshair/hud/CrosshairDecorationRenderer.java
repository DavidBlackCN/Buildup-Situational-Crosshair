package dev.buildup.situationalcrosshair.hud;

import dev.buildup.situationalcrosshair.presentation.CrosshairPresentation;
import dev.buildup.situationalcrosshair.presentation.PixelGlyph;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Small original pixels with a dark outline; no world, player or semantic queries. */
public final class CrosshairDecorationRenderer {
    public static final int ACTION_X = 13, ACTION_Y = 10;
    public static final int SECONDARY_STATUS_X = 13, SECONDARY_STATUS_Y = 1;
    public static final int PRIMARY_STATUS_X = -3, PRIMARY_STATUS_Y = 5;
    private CrosshairDecorationRenderer() { }

    public static void draw(GuiGraphicsExtractor graphics, CrosshairPresentation presentation, int x, int y, float opacity) {
        drawGlyph(graphics, PixelGlyph.modifier(presentation.modifier()), x + ACTION_X, y + ACTION_Y, opacity);
        drawGlyph(graphics, PixelGlyph.status(presentation.primaryStatus()), x + PRIMARY_STATUS_X, y + PRIMARY_STATUS_Y, opacity);
        drawGlyph(graphics, PixelGlyph.status(presentation.secondaryStatus()), x + SECONDARY_STATUS_X, y + SECONDARY_STATUS_Y, opacity);
    }

    public static void drawGlyph(GuiGraphicsExtractor graphics, PixelGlyph glyph, int x, int y, float opacity) {
        if (glyph == null) return;
        int alpha = Math.round(255 * Math.clamp(opacity, 0, 1));
        if (alpha == 0) return;
        int outline = alpha << 24 | 0x101010;
        int foreground = alpha << 24 | 0xf4f4f4;
        // Build the union of the one-pixel halo once, rather than repeatedly blending
        // overlapping rectangles (which would make SUBTLE opacity inconsistent).
        for (int row = -1; row <= PixelGlyph.SIZE; row++) for (int col = -1; col <= PixelGlyph.SIZE; col++) {
            if (glyph.at(col, row)) continue;
            boolean neighbor = false;
            for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) neighbor |= glyph.at(col + dx, row + dy);
            if (neighbor) graphics.fill(x + col, y + row, x + col + 1, y + row + 1, outline);
        }
        for (int row = 0; row < PixelGlyph.SIZE; row++) for (int col = 0; col < PixelGlyph.SIZE; col++) {
            if (glyph.at(col, row)) graphics.fill(x + col, y + row, x + col + 1, y + row + 1, foreground);
        }
    }
}
