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
    private record Treatment(String name, int size, int stateSize) { }
    private static final List<Treatment> TREATMENTS = List.of(
            new Treatment("A", 8, 6), new Treatment("B", 10, 8), new Treatment("C", 12, 10));

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
                var options = TestScreenshotOptions.of("stage-5.3-exploration-" + treatment.name() + "-scale-" + scale + (light ? "-light" : "-dark"))
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
        org.slf4j.LoggerFactory.getLogger("sidecar-exploration").info("PASS: 18 sprite exploration screenshots, actual GUI scales 1/2/3");
    }

    private static void sprite(GuiGraphicsExtractor g, Treatment family, String name, int size, int x, int y, float weight) {
        new dev.buildup.situationalcrosshair.hud.SidecarSprite(net.minecraft.resources.Identifier.fromNamespaceAndPath(
                BuildupSituationalCrosshairClient.MOD_ID, "textures/gui/sidecar/exploration/" + family.name().toLowerCase(java.util.Locale.ROOT) + "/" + name + ".png"), size)
                .draw(g, x, y, weight);
    }

    private static final class Gallery extends Screen {
        private final Treatment treatment;
        private final boolean light;
        Gallery(Treatment treatment, boolean light) { super(Component.literal("Test-only sidecar exploration")); this.treatment = treatment; this.light = light; }
        @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            g.fill(0, 0, width, height, light ? 0xffd4d4cc : 0xff344c32);
            int color = light ? 0xff15191d : 0xfff4f4f4;
            g.text(font, "Prototype " + treatment.name() + " | " + treatment.size() + "px | GUI " + minecraft.options.guiScale().get(), 10, 10, color);
            String[] labels = {"Interact", "Place", "Transform", "Target use", "Trade", "Blocked", "Block only", "Attack only", "Axe prototype"};
            String[] icons = {"interact", "place", "transform", "use", "interact", "place", null, null, "axe"};
            for (int i = 0; i < labels.length; i++) {
                int x = 10 + i % 3 * (width - 20) / 3, y = 42 + i / 3 * 65;
                g.text(font, labels[i], x, y, color);
                int bx = x + 30, by = y + 18, size = treatment.size();
                CrosshairHudRenderer.drawPresentation(g, dev.buildup.situationalcrosshair.presentation.TransitionController.Frame.immediate(
                        CrosshairPresentation.baseOnly(i == 4 || i == 7 ? ClassicCrosshairType.ATTACK : ClassicCrosshairType.BLOCK)), bx, by);
                if (icons[i] != null) sprite(g, treatment, icons[i], size, bx + 18, by + (15 - size) / 2, 0.70f);
                if (i == 5) sprite(g, treatment, "blocked", treatment.stateSize(), bx - treatment.stateSize() - 3,
                        by + (15 - treatment.stateSize()) / 2, 0.52f);
            }
            g.text(font, "Original sprite family | Axe: test only", 10, height - 18, color);
        }
        @Override public boolean isPauseScreen() { return false; }
    }
}
