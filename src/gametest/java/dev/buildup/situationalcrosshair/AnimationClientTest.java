package dev.buildup.situationalcrosshair;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.buildup.situationalcrosshair.crosshair.ClassicCrosshairType;
import dev.buildup.situationalcrosshair.crosshair.CrosshairTransition.Mode;
import dev.buildup.situationalcrosshair.hud.CrosshairHudRenderer;
import dev.buildup.situationalcrosshair.hud.CrosshairTextures;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public final class AnimationClientTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        context.runOnClient(c -> {
            CrosshairOptions.setAdaptiveContrast(false);
            if (CrosshairTextures.masks() == null) throw new AssertionError("Classic masks failed to load");
            for (Mode mode : new Mode[]{Mode.FADE, Mode.EXPAND}) for (var a : ClassicCrosshairType.values())
                for (var b : ClassicCrosshairType.values()) for (long ms : new long[]{0, 20, 50, 100, 150}) {
                    var record = new Recording(c);
                    sample(record, a, b, mode, ms, 0, 0);
                    if (record.whole == 0 && record.center != 1) throw new AssertionError("Center missing/repeated");
                }
            CrosshairOptions.setAnimation(Mode.OFF);
            var direct = new Recording(c);
            CrosshairHudRenderer.draw(direct, ClassicCrosshairType.ERROR, 0, 0, 0);
            if (direct.whole != 1) throw new AssertionError("Animation OFF must use original texture");
        });
        var previousMasks = context.computeOnClient(c -> CrosshairTextures.masks());
        var reload = context.computeOnClient(c -> c.reloadResourcePacks());
        context.waitFor(c -> reload.isDone());
        context.runOnClient(c -> {
            reload.join();
            if (CrosshairTextures.masks() == null || CrosshairTextures.masks() == previousMasks)
                throw new AssertionError("Resource reload did not replace masks");
        });
        context.waitFor(c -> c.gui.overlay() == null);
        int originalScale = context.computeOnClient(c -> c.options.guiScale().get());
        int originalWidth = context.computeOnClient(c -> c.getWindow().getWidth());
        int originalHeight = context.computeOnClient(c -> c.getWindow().getHeight());
        try {
            for (int scale : new int[]{1, 2, 3}) for (boolean light : new boolean[]{false, true}) {
                context.runOnClient(c -> {
                    c.getWindow().setWidth(1280); c.getWindow().setHeight(960);
                    c.options.guiScale().set(scale); c.resizeGui();
                });
                context.setScreen(() -> new Gallery(light));
                context.waitTicks(3);
                context.takeScreenshot(TestScreenshotOptions.of("animation-scale-" + scale + (light ? "-light" : "-dark"))
                        .disableCounterPrefix().withSize(1280, 960)
                        .withDestinationDir(Path.of("screenshots").toAbsolutePath()));
            }
        } finally {
            context.runOnClient(c -> {
                c.gui.setScreen(null);
                c.getWindow().setWidth(originalWidth); c.getWindow().setHeight(originalHeight);
                c.options.guiScale().set(originalScale); c.resizeGui();
                CrosshairOptions.setAnimation(Mode.EXPAND);
            });
        }
        org.slf4j.LoggerFactory.getLogger("crosshair-animation-test").info(
                "PASS: 160 production transition samples; single inverse blend per pixel; resource reload; six GUI scale/background galleries");
    }

    static void sample(GuiGraphicsExtractor g, ClassicCrosshairType a, ClassicCrosshairType b,
                       Mode mode, long ms, int x, int y) {
        CrosshairOptions.setAnimation(mode);
        var sink = new Recording(Minecraft.getInstance());
        CrosshairHudRenderer.draw(sink, a, 0, 0, 0);
        CrosshairHudRenderer.draw(new Recording(Minecraft.getInstance()), b, 0, 0, 0);
        CrosshairHudRenderer.draw(g, b, x, y, ms * 1_000_000L);
    }

    private static final class Recording extends GuiGraphicsExtractor {
        int whole, center;
        Set<Integer> pixels = new HashSet<>();
        Recording(Minecraft c) { super(c, new GuiRenderState(), 0, 0); }
        @Override public void blit(RenderPipeline pipeline, Identifier texture, int x, int y, float u, float v,
                                   int w, int h, int tw, int th) {
            whole++;
            if (w != 15 || h != 15) throw new AssertionError("Resting texture changed");
        }
        @Override public void blit(RenderPipeline pipeline, Identifier texture, int x, int y, float u, float v,
                                   int w, int h, int tw, int th, int color) {
            if (pipeline != RenderPipelines.CROSSHAIR || w != 1 || h != 1 || tw != 15 || th != 15 || u != 7 || v != 7)
                throw new AssertionError("Incorrect animation texture/pipeline");
            if (x < 0 || x >= 15 || y < 0 || y >= 15 || !pixels.add(y * 15 + x))
                throw new AssertionError("Repeated/outside pixel");
            int r = color >> 16 & 255, green = color >> 8 & 255, blue = color & 255;
            if (r != green || r != blue || (color >>> 24) != 255) throw new AssertionError("Invalid inverse strength");
            if (x == 7 && y == 7) {
                center++;
                if (color != 0xffffffff) throw new AssertionError("Center must stay full strength");
            }
        }
    }

    private static final class Gallery extends Screen {
        private final boolean light;
        Gallery(boolean light) { super(Component.literal("Main crosshair transitions")); this.light = light; }
        @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            int text = light ? 0xff202020 : 0xffeeeeee;
            g.fill(0, 0, width, height, light ? 0xffd4d4cc : 0xff344c32);
            g.text(font, "Main crosshair | GUI " + minecraft.options.guiScale().get(), 10, 8, text);
            String[] labels = {"DOT > BLOCK / FADE", "DOT > BLOCK / EXPAND", "BLOCK > DOT", "BLOCK > ERROR", "DOT > ATTACK", "BLOCK > ATTACK"};
            var starts = new ClassicCrosshairType[]{ClassicCrosshairType.DOT, ClassicCrosshairType.DOT, ClassicCrosshairType.BLOCK,
                    ClassicCrosshairType.BLOCK, ClassicCrosshairType.DOT, ClassicCrosshairType.BLOCK};
            var ends = new ClassicCrosshairType[]{ClassicCrosshairType.BLOCK, ClassicCrosshairType.BLOCK, ClassicCrosshairType.DOT,
                    ClassicCrosshairType.ERROR, ClassicCrosshairType.ATTACK, ClassicCrosshairType.ATTACK};
            long[] times = {0, 20, 50, 100, 150};
            int cell = (width - 30) / 5;
            for (int row = 0; row < labels.length; row++) {
                int y = 30 + row * 45;
                g.text(font, labels[row], 10, y, text);
                for (int col = 0; col < times.length; col++) {
                    int x = 10 + col * cell;
                    sample(g, starts[row], ends[row], row == 0 ? Mode.FADE : Mode.EXPAND, times[col], x + 5, y + 15);
                    g.text(font, times[col] + "ms", x + 25, y + 19, text);
                }
            }
        }
    }
}
