package dev.buildup.situationalcrosshair;

import dev.buildup.situationalcrosshair.crosshair.ClassicCrosshairType;
import dev.buildup.situationalcrosshair.hud.CrosshairHudRenderer;
import dev.buildup.situationalcrosshair.presentation.CrosshairPresentation;
import dev.buildup.situationalcrosshair.semantic.Visibility;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.nio.file.Path;
import java.util.List;

/** Bounded design exploration only. None of these choices is a runtime theme. */
public final class SidecarExplorationTest implements FabricClientGameTest {
    private record Treatment(String name, String[][] icons) { int size() { return icons[0].length; } }
    private static final List<Treatment> TREATMENTS = List.of(
            new Treatment("A", new String[][] {
                    {"##..##", "#....#", "..#...", "..##..", "#....#", "##..##"},
                    {"..##..", ".#..#.", "#..#.#", "#..#.#", ".#.#.#", "..###."},
                    {"...#.#", "..#..#", "..###.", ".##...", "##....", ".#...."},
                    {"..#...", ".#..#.", "#..#.#", ".#..#.", "..#...", "..#..."},
                    {"..##..", ".#..#.", ".#..#.", ".####.", ".#.##.", ".####."}}),
            new Treatment("B", new String[][] {
                    {"##...##", "#.....#", "...#...", "..###..", "...#...", "#.....#", "##...##"},
                    {"..###..", ".#..##.", "#..#..#", "#..#..#", ".#.#.#.", "..###..", "......."},
                    {"....#.#", "...#..#", "...###.", "..##...", ".##....", "##.....", ".#....."},
                    {"...#...", "...#...", ".#...#.", "#..#..#", ".#...#.", "...#...", "...#..."},
                    {"..###..", ".#...#.", ".#...#.", ".#####.", ".#.#.#.", ".#...#.", ".#####."}}),
            new Treatment("C", new String[][] {
                    {"###..###", "#......#", "...##...", "..####..", "...##...", "#......#", "#......#", "###..###"},
                    {"...##...", "..#..#..", ".#..#.#.", "#..#...#", "#..#...#", ".#.#..#.", "..####..", "........"},
                    {".....#.#", "....#..#", "....###.", "...##...", "..##....", ".##.....", "##......", ".#......"},
                    {"...##...", "...##...", ".#....#.", "#..##..#", "#..##..#", ".#....#.", "...##...", "...##..."},
                    {"..####..", ".#....#.", ".#....#.", ".######.", ".#....#.", ".#.##.#.", ".#....#.", ".######."}}),
            new Treatment("B-refined", new String[][] {
                    {"..#....", "..#....", "..###..", ".##..#.", "#.#..#.", "#....#.", ".####.."},
                    {"..###..", ".#..##.", "#..#..#", "#..#..#", ".#.#.#.", "..###..", "......."},
                    {"....#.#", "...#..#", "...###.", "..##...", ".##....", "##.....", ".#....."},
                    {"...#...", "...#...", ".#...#.", "#..#..#", ".#...#.", "...#...", "...#..."},
                    {"..###..", ".#...#.", ".#...#.", ".#####.", ".#.#.#.", ".#...#.", ".#####."}}));

    @Override public void runTest(ClientGameTestContext context) {
        int scaleBefore = context.computeOnClient(c -> c.options.guiScale().get());
        int widthBefore = context.computeOnClient(c -> c.getWindow().getWidth());
        int heightBefore = context.computeOnClient(c -> c.getWindow().getHeight());
        try {
            for (int scale : List.of(1, 2, 3)) for (boolean light : List.of(false, true)) for (var treatment : TREATMENTS) {
                context.runOnClient(c -> {
                    c.getWindow().setWidth(scale == 3 ? 1280 : 854);
                    c.getWindow().setHeight(scale == 3 ? 960 : 480);
                    c.options.guiScale().set(scale); c.resizeGui();
                    if (c.getWindow().getGuiScale() != scale) throw new AssertionError("Exploration GUI scale mismatch");
                });
                context.setScreen(() -> new Gallery(treatment, light));
                context.waitTicks(3);
                var options = TestScreenshotOptions.of("stage-5.2-exploration-" + treatment.name() + "-scale-" + scale + (light ? "-light" : "-dark"))
                        .disableCounterPrefix().withDestinationDir(Path.of("screenshots").toAbsolutePath())
                        .withSize(scale == 3 ? 1280 : 854, scale == 3 ? 960 : 480);
                context.takeScreenshot(options);
            }
        } finally {
            context.runOnClient(c -> {
                c.gui.setScreen(null); c.getWindow().setWidth(widthBefore); c.getWindow().setHeight(heightBefore);
                c.options.guiScale().set(scaleBefore); c.resizeGui();
            });
        }
        org.slf4j.LoggerFactory.getLogger("sidecar-exploration").info("PASS: 24 sidecar exploration screenshots, actual GUI scales 1/2/3");
    }

    private static void glyph(GuiGraphicsExtractor g, String[] rows, int x, int y, float weight, float shadow) {
        int size = rows.length;
        for (int dy = 1; dy <= size; dy++) for (int dx = 1; dx <= size; dx++) {
            if (rows[dy - 1].charAt(dx - 1) == '#' && !(dy < size && dx < size && rows[dy].charAt(dx) == '#'))
                g.fill(x + dx, y + dy, x + dx + 1, y + dy + 1, Math.round(255 * weight * shadow) << 24 | 0x161714);
        }
        for (int dy = 0; dy < size; dy++) for (int dx = 0; dx < size; dx++) if (rows[dy].charAt(dx) == '#')
            g.fill(x + dx, y + dy, x + dx + 1, y + dy + 1, Math.round(255 * weight) << 24 | 0xf2eedf);
    }

    private static final class Gallery extends Screen {
        private final Treatment treatment;
        private final boolean light;
        Gallery(Treatment treatment, boolean light) { super(Component.literal("Test-only sidecar exploration")); this.treatment = treatment; this.light = light; }
        @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            g.fill(0, 0, width, height, light ? 0xffd4d4cc : 0xff344c32);
            int color = light ? 0xff15191d : 0xfff4f4f4;
            g.text(font, "Prototype " + treatment.name() + " | " + treatment.size() + "px | GUI " + minecraft.options.guiScale().get(), 10, 10, color);
            String[] labels = {"Interact", "Place", "Transform", "Target use", "Trade", "Blocked"};
            for (int i = 0; i < labels.length; i++) {
                int x = 10 + i % 3 * (width - 20) / 3, y = 45 + i / 3 * 70;
                g.text(font, labels[i], x, y, color);
                int bx = x + 25, by = y + 20, size = treatment.size();
                CrosshairHudRenderer.drawPresentation(g, dev.buildup.situationalcrosshair.presentation.TransitionController.Frame.immediate(
                        CrosshairPresentation.baseOnly(i == 4 ? ClassicCrosshairType.ATTACK : ClassicCrosshairType.BLOCK)), bx, by);
                float shadow = treatment.name().equals("B-refined") ? 0.8f : 0.65f;
                glyph(g, treatment.icons()[i < 4 ? i : i == 4 ? 0 : 1], bx + 17, by + (15 - size) / 2, 0.68f, shadow);
                if (i == 5) glyph(g, treatment.icons()[4], bx - size - 3, by + (15 - size) / 2, 0.52f, shadow);
            }
            g.text(font, "Test only | right action 68%, left state 52%", 10, height - 18, color);
        }
        @Override public boolean isPauseScreen() { return false; }
    }
}
