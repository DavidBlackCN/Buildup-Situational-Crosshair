package dev.buildup.situationalcrosshair.presentation;

import static dev.buildup.situationalcrosshair.presentation.CrosshairPresentation.ActionSidecar;
import static dev.buildup.situationalcrosshair.presentation.CrosshairPresentation.StateSidecar;

/** Independently authored sparse 7px icons. Cached; no runtime item rasterization. */
public record PixelGlyph(String pixels) {
    public static final int SIZE = 7;
    private static final java.util.Map<ActionSidecar, PixelGlyph> ACTIONS = java.util.Map.of(
            ActionSidecar.INTERACT, glyph("..#....", "..#....", "..###..", ".##..#.", "#.#..#.", "#....#.", ".####.."),
            ActionSidecar.PLACE, glyph("..###..", ".#..##.", "#..#..#", "#..#..#", ".#.#.#.", "..###..", "......."),
            ActionSidecar.TRANSFORM, glyph("....#.#", "...#..#", "...###.", "..##...", ".##....", "##.....", ".#....."),
            ActionSidecar.USE, glyph("...#...", "...#...", ".#...#.", "#..#..#", ".#...#.", "...#...", "...#..."));
    private static final PixelGlyph BLOCKED = glyph("..###..", ".#...#.", ".#...#.", ".#####.", ".#.#.#.", ".#...#.", ".#####.");
    public PixelGlyph {
        if (pixels.length() != SIZE * SIZE || !pixels.matches("[.#]+")) throw new IllegalArgumentException("Expected 7x7 glyph");
    }
    public boolean at(int x, int y) {
        return x >= 0 && y >= 0 && x < SIZE && y < SIZE && pixels.charAt(y * SIZE + x) == '#';
    }
    public static PixelGlyph action(ActionSidecar icon) { return ACTIONS.get(icon); }
    public static PixelGlyph state(StateSidecar icon) { return icon == StateSidecar.BLOCKED ? BLOCKED : null; }
    private static PixelGlyph glyph(String... rows) { return new PixelGlyph(String.join("", rows)); }
}
