package dev.buildup.situationalcrosshair.presentation;

import dev.buildup.situationalcrosshair.semantic.ActionState;
import static dev.buildup.situationalcrosshair.presentation.CrosshairPresentation.SecondaryModifier;

/** Original 5x5 pixel symbols; # is foreground, . is transparent. No third-party artwork. */
public record PixelGlyph(String pixels) {
    public static final int SIZE = 5;
    private static final java.util.Map<SecondaryModifier, PixelGlyph> MODIFIERS = java.util.Map.of(
            SecondaryModifier.INTERACT, glyph("#####", "#...#", "#...#", "###.#", "..#.."),
            SecondaryModifier.USE, glyph("..#..", ".###.", ".###.", "#####", ".###."),
            SecondaryModifier.PLACE, glyph("#####", "#...#", "#.#.#", "#...#", "#####"),
            SecondaryModifier.TRANSFORM, glyph("...#.", "#####", ".....", "#####", ".#..."),
            SecondaryModifier.SPECIAL, glyph("..#..", ".#.#.", "#...#", ".#.#.", "..#.."));
    private static final java.util.Map<ActionState, PixelGlyph> STATUSES = java.util.Map.of(
            ActionState.CHARGING, glyph("..#..", ".#.#.", "#...#", "#...#", "#####"),
            ActionState.READY, glyph(".....", "....#", "#..#.", ".##..", "..#.."),
            ActionState.COOLDOWN, glyph("#####", ".#.#.", "..#..", ".#.#.", "#####"),
            ActionState.BLOCKED, glyph(".###.", ".#.#.", "#####", "#.#.#", "#####"),
            ActionState.INVALID, glyph("#...#", ".#.#.", "..#..", ".#.#.", "#...#"));
    public PixelGlyph {
        if (pixels.length() != SIZE * SIZE || !pixels.matches("[.#]+")) throw new IllegalArgumentException("Expected 5x5 glyph");
    }
    public boolean at(int x, int y) {
        return x >= 0 && y >= 0 && x < SIZE && y < SIZE && pixels.charAt(y * SIZE + x) == '#';
    }
    public static PixelGlyph modifier(SecondaryModifier modifier) {
        return MODIFIERS.get(modifier);
    }
    public static PixelGlyph status(ActionState state) {
        return STATUSES.get(state);
    }
    private static PixelGlyph glyph(String... rows) { return new PixelGlyph(String.join("", rows)); }
}
