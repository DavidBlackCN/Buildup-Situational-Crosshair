package dev.buildup.situationalcrosshair;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.buildup.situationalcrosshair.crosshair.ClassicCrosshairType;
import dev.buildup.situationalcrosshair.crosshair.CrosshairTransition.Mode;
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
import java.util.ArrayList;
import java.util.HashSet;

public final class OutlineClientTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        context.runOnClient(c -> {
            for (var mode : Mode.values()) for (var a : ClassicCrosshairType.values())
                for (var b : ClassicCrosshairType.values()) for (long ms : new long[]{0, 35, 70, 139, 140}) {
                    var plain = new Recording(c);
                    CrosshairOptions.setOutline(false);
                    AnimationClientTest.sample(plain, a, b, mode, ms, 0, 0);
                    if (plain.edges != 0) throw new AssertionError("Outline OFF still draws edges");
                    var outlined = new Recording(c);
                    CrosshairOptions.setOutline(true);
                    AnimationClientTest.sample(outlined, a, b, mode, ms, 0, 0);
                    if (!plain.body.equals(outlined.body)) throw new AssertionError("Outline changed inverse body");
                    if (outlined.edges == 0) throw new AssertionError("Missing contour");
                }
        });
        int scaleBefore = context.computeOnClient(c -> c.options.guiScale().get());
        int widthBefore = context.computeOnClient(c -> c.getWindow().getWidth());
        int heightBefore = context.computeOnClient(c -> c.getWindow().getHeight());
        try {
            for (int scale : new int[]{1, 2, 3}) {
                context.runOnClient(c -> {
                    c.getWindow().setWidth(1280); c.getWindow().setHeight(960);
                    c.options.guiScale().set(scale); c.resizeGui();
                });
                context.setScreen(Gallery::new);
                context.waitFor(c -> c.gui.overlay() == null);
                context.waitTicks(3);
                context.takeScreenshot(TestScreenshotOptions.of("outline-comparison-scale-" + scale)
                        .disableCounterPrefix().withSize(1280, 960)
                        .withDestinationDir(Path.of("screenshots").toAbsolutePath()));
            }
        } finally {
            context.runOnClient(c -> {
                c.gui.setScreen(null);
                c.getWindow().setWidth(widthBefore); c.getWindow().setHeight(heightBefore);
                c.options.guiScale().set(scaleBefore); c.resizeGui();
                CrosshairOptions.setOutline(true);
                CrosshairOptions.setAnimation(Mode.EXPAND);
            });
        }
        org.slf4j.LoggerFactory.getLogger("crosshair-outline-test").info(
                "PASS: 240 paired outline samples; inverse body unchanged; three native-texture comparison galleries");
    }

    private static final class Recording extends GuiGraphicsExtractor {
        final ArrayList<String> body = new ArrayList<>();
        final HashSet<Integer> edgePixels = new HashSet<>();
        int edges;
        Recording(Minecraft client) { super(client, new GuiRenderState(), 0, 0); }
        @Override public void fill(int x1, int y1, int x2, int y2, int color) {
            if (!body.isEmpty()) throw new AssertionError("Contour must precede inverse body");
            if (x2 != x1 + 1 || y2 != y1 + 1 || (color & 0xffffff) != 0 || (color >>> 24) > 107)
                throw new AssertionError("Contour must be a subtle black one-pixel alpha fill");
            if (x1 == 7 && y1 == 7) throw new AssertionError("Contour under aiming point");
            if (x1 < -1 || x1 > 15 || y1 < -1 || y1 > 15 || !edgePixels.add((y1 + 1) * 17 + x1 + 1))
                throw new AssertionError("Contour bounds/overlap");
            edges++;
        }
        @Override public void blit(RenderPipeline p, Identifier tex, int x, int y, float u, float v,
                                   int w, int h, int tw, int th) {
            body.add(tex + ":" + x + ":" + y + ":" + w + ":" + h);
        }
        @Override public void blit(RenderPipeline p, Identifier tex, int x, int y, float u, float v,
                                   int w, int h, int tw, int th, int color) {
            if (color == 0xffffffff && edgePixels.contains((y + 1) * 17 + x + 1))
                throw new AssertionError("Contour under fully covered body pixel");
            body.add(tex + ":" + x + ":" + y + ":" + color);
        }
    }

    private static final class Gallery extends Screen {
        Gallery() { super(Component.literal("Inverse crosshair contour comparison")); }
        @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
            g.fill(0, 0, width, height, 0xff25282b);
            g.text(font, "Inverse: left = pure / right = outline | GUI " + minecraft.options.guiScale().get(), 8, 8, 0xffffffff);
            String[] names = {"Stone", "Snow", "Moss", "Dim stone"};
            String[] textures = {"stone", "snow", "moss_block", "stone"};
            String[] rows = {"DOT", "BLOCK", "ERROR", "ATTACK", "ERROR 70ms", "DOT 70ms"};
            var goals = new ClassicCrosshairType[]{ClassicCrosshairType.DOT, ClassicCrosshairType.BLOCK,
                    ClassicCrosshairType.ERROR, ClassicCrosshairType.ATTACK, ClassicCrosshairType.ERROR, ClassicCrosshairType.DOT};
            int cell = (width - 82) / 4;
            for (int col = 0; col < 4; col++) g.text(font, names[col], 80 + col * cell, 24, 0xffeeeeee);
            for (int row = 0; row < rows.length; row++) {
                int y = 38 + row * 43;
                g.text(font, rows[row], 5, y + 13, 0xffeeeeee);
                for (int col = 0; col < 4; col++) {
                    int x = 80 + col * cell;
                    var texture = Identifier.withDefaultNamespace("textures/block/" + textures[col] + ".png");
                    for (int dy = 0; dy < 36; dy += 16) for (int dx = 0; dx < cell - 4; dx += 16)
                        g.blit(RenderPipelines.GUI_TEXTURED, texture, x + dx, y + dy, 0, 0,
                                Math.min(16, cell - 4 - dx), Math.min(16, 36 - dy), 16, 16);
                    if (col == 3) g.fill(x, y, x + cell - 4, y + 36, 0xc0000000);
                    // Both sides have the same local texture sample, by using a 16-pixel offset.
                    int left = x + Math.max(0, (cell - 36) / 2);
                    CrosshairOptions.setOutline(false);
                    AnimationClientTest.sample(g, row >= 4 ? ClassicCrosshairType.BLOCK : goals[row], goals[row],
                            Mode.EXPAND, row >= 4 ? 70 : 140, left, y + 10);
                    CrosshairOptions.setOutline(true);
                    AnimationClientTest.sample(g, row >= 4 ? ClassicCrosshairType.BLOCK : goals[row], goals[row],
                            Mode.EXPAND, row >= 4 ? 70 : 140, left + 16, y + 10);
                }
            }
        }
    }
}
