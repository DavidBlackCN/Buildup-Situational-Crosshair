package dev.buildup.situationalcrosshair.presentation;

import static dev.buildup.situationalcrosshair.presentation.CrosshairPresentation.SecondaryModifier;

/** Original micro symbols; # is foreground, . is transparent. No status icons. */
public record PixelGlyph(String pixels) {
    public static final int SIZE = 3;
    private static final java.util.Map<SecondaryModifier, PixelGlyph> MODIFIERS = java.util.Map.of(
            SecondaryModifier.INTERACT, glyph("###", "#.#", "##."),
            SecondaryModifier.USE, glyph(".#.", "###", ".##"),
            SecondaryModifier.PLACE, glyph("###", "#.#", "###"),
            SecondaryModifier.TRANSFORM, glyph(".##", "...", "##."));
    public PixelGlyph {
        if (pixels.length() != SIZE * SIZE || !pixels.matches("[.#]+"))
            throw new IllegalArgumentException("Expected 3x3 glyph");
    }
    public boolean at(int x, int y) {
        return x >= 0 && y >= 0 && x < SIZE && y < SIZE && pixels.charAt(y * SIZE + x) == '#';
    }
    public static PixelGlyph modifier(SecondaryModifier modifier) { return MODIFIERS.get(modifier); }
    private static PixelGlyph glyph(String... rows) { return new PixelGlyph(String.join("", rows)); }
}
