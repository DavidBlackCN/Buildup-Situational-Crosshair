package dev.buildup.situationalcrosshair;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.buildup.situationalcrosshair.crosshair.AdaptiveContrast;
import dev.buildup.situationalcrosshair.crosshair.ClassicCrosshairResolver;
import dev.buildup.situationalcrosshair.crosshair.ClassicCrosshairType;
import dev.buildup.situationalcrosshair.crosshair.CrosshairTransition.Mode;
import dev.buildup.situationalcrosshair.hud.CrosshairBackground;
import dev.buildup.situationalcrosshair.hud.CrosshairHudRenderer;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.resources.Identifier;
import java.nio.file.Path;

/** Real world -> GPU copy -> fenced callback -> actual HUD; no injected luminance. */
public final class ContrastClientTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        int scale = context.computeOnClient(c -> c.options.guiScale().get());
        int width = context.computeOnClient(c -> c.getWindow().getWidth());
        int height = context.computeOnClient(c -> c.getWindow().getHeight());
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksDownload();
            context.waitFor(c -> c.gui.screen() == null);
            world.getServer().runCommand("gamemode creative @a");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("fill -8 99 -8 8 99 6 stone");
            world.getServer().runCommand("tp @a 0.5 100 0.5 0 0");
            context.runOnClient(c -> {
                c.getWindow().setWidth(1280); c.getWindow().setHeight(960);
                c.options.guiScale().set(3); c.resizeGui();
                CrosshairOptions.setEnabled(true);
                CrosshairOptions.setAnimation(Mode.EXPAND);
                CrosshairOptions.setAdaptiveContrast(true);
            });
            String[] blocks = {"stone", "snow_block", "oak_leaves[persistent=true]"};
            String[] labels = {"stone", "snow", "leaves"};
            for (int i = 0; i < blocks.length; i++) {
                world.getServer().runCommand("fill -8 100 3 8 108 5 " + blocks[i]);
                context.waitTicks(35);
                context.waitFor(c -> ClassicCrosshairResolver.resolve(c) == ClassicCrosshairType.BLOCK
                        && CrosshairBackground.color() != 0);
                if (i == 1) context.waitFor(c -> CrosshairBackground.color() == AdaptiveContrast.DARK);
                capturePair(context, labels[i]);
            }
            world.getServer().runCommand("fill -8 100 3 8 108 5 magma_block");
            context.waitTicks(25);
            int surfaceTone = context.computeOnClient(c -> CrosshairBackground.color());
            var surfacePos = context.computeOnClient(c -> ((net.minecraft.world.phys.BlockHitResult)c.hitResult).getBlockPos());
            for (int yaw : new int[]{-8, -4, 0, 4, 8, 4, 0, -4, -8, 0}) {
                context.runOnClient(c -> c.player.setYRot(yaw));
                context.waitTicks(5);
                context.runOnClient(c -> {
                    if (!(c.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit) || !hit.getBlockPos().equals(surfacePos))
                        throw new AssertionError("Texture sweep must stay on one block");
                    if (surfaceTone == 0 || CrosshairBackground.color() != surfaceTone)
                        throw new AssertionError("High-contrast texture caused tone jump on the same face");
                });
            }
            screenshot(context, "adaptive-texture-sweep");
            world.getServer().runCommand("fill -8 99 -8 8 108 6 stone hollow");
            world.getServer().runCommand("fill -8 100 3 8 108 5 stone");
            context.waitTicks(40);
            context.waitFor(c -> CrosshairBackground.color() == AdaptiveContrast.LIGHT);
            capturePair(context, "cave");

            // Real source changes from dark enclosed room to bright snow, plus resize/reload invalidation.
            world.getServer().runCommand("fill -8 108 -8 8 108 6 air");
            world.getServer().runCommand("fill -8 100 3 8 107 5 snow_block");
            for (int guiScale : new int[]{1, 2, 3}) {
                context.runOnClient(c -> { c.options.guiScale().set(guiScale); c.resizeGui(); });
                context.waitFor(c -> CrosshairBackground.color() == AdaptiveContrast.DARK);
                context.waitTicks(3);
                screenshot(context, "contrast-snow-scale-" + guiScale);
            }
            var reload = context.computeOnClient(c -> c.reloadResourcePacks());
            context.waitFor(c -> reload.isDone() && c.gui.overlay() == null);
            context.runOnClient(c -> reload.join());
            context.waitFor(c -> CrosshairBackground.color() != 0);
            context.runOnClient(ContrastClientTest::checkDraws);
            context.runOnClient(c -> c.gui.hud.toggle());
            context.waitTicks(3);
            context.runOnClient(c -> {
                if (CrosshairBackground.color() != 0) throw new AssertionError("Hidden HUD kept sample");
                c.gui.hud.toggle();
            });
            context.waitFor(c -> CrosshairBackground.color() != 0);
            world.getServer().runCommand("fill -8 100 3 8 107 5 stone");
            world.getServer().runCommand("gamemode survival @a");
            context.waitFor(c -> ClassicCrosshairResolver.resolve(c) == ClassicCrosshairType.ERROR
                    && CrosshairBackground.color() != 0);
            context.waitTicks(8);
            screenshot(context, "adaptive-error");
            world.getServer().runCommand("gamemode creative @a");
            world.getServer().runCommand("summon armor_stand 0.5 100 2.2 {NoGravity:1b}");
            context.waitFor(c -> ClassicCrosshairResolver.resolve(c) == ClassicCrosshairType.ATTACK
                    && CrosshairBackground.color() != 0);
            context.waitTicks(8);
            screenshot(context, "adaptive-attack");
            world.getServer().runCommand("tp @a 0.5 100 0.5 0 -70");
            context.waitFor(c -> ClassicCrosshairResolver.resolve(c) == ClassicCrosshairType.DOT
                    && CrosshairBackground.color() != 0);
            context.waitTicks(5);
            screenshot(context, "contrast-dot-sky");
        } finally {
            context.runOnClient(c -> {
                CrosshairOptions.setAdaptiveContrast(true);
                CrosshairOptions.setAnimation(Mode.EXPAND);
                c.getWindow().setWidth(width); c.getWindow().setHeight(height);
                c.options.guiScale().set(scale); c.resizeGui();
            });
        }
        context.runOnClient(c -> {
            if (CrosshairBackground.color() != 0) throw new AssertionError("World exit kept sample");
        });
        org.slf4j.LoggerFactory.getLogger("crosshair-contrast-test").info(
                "PASS: live GPU sampling, same-block high-contrast texture sweep, scene tone switch, inverse A/B, GUI 1/2/3, reload, F1, exit, all four states and interrupted transitions");
    }

    private static void capturePair(ClientGameTestContext context, String name) {
        screenshot(context, "contrast-" + name + "-adaptive");
        context.runOnClient(c -> CrosshairOptions.setAdaptiveContrast(false));
        context.waitTicks(4);
        screenshot(context, "contrast-" + name + "-invert");
        context.runOnClient(c -> CrosshairOptions.setAdaptiveContrast(true));
        context.waitFor(c -> CrosshairBackground.color() != 0);
        context.waitTicks(4);
    }

    private static void screenshot(ClientGameTestContext context, String name) {
        context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix().withSize(1280, 960)
                .withDestinationDir(Path.of("screenshots").toAbsolutePath()));
    }

    private static void checkDraws(Minecraft c) {
        int tone = CrosshairBackground.color();
        if (tone == 0) throw new AssertionError("No GPU sample");
        CrosshairHudRenderer.resetAnimation();
        var record = new Recording(c, tone);
        CrosshairHudRenderer.draw(record, ClassicCrosshairType.DOT, 0, 0, 0);
        CrosshairHudRenderer.draw(record, ClassicCrosshairType.BLOCK, 0, 0, 0);
        CrosshairHudRenderer.draw(record, ClassicCrosshairType.BLOCK, 0, 0, 50_000_000L);
        if (!record.fading || record.whole != 1 || record.center != 2) throw new AssertionError("Adaptive coverage/center");
        for (var mode : new Mode[]{Mode.FADE, Mode.EXPAND}) for (var from : ClassicCrosshairType.values())
            for (var to : ClassicCrosshairType.values()) {
                CrosshairOptions.setAnimation(mode);
                var all = new Recording(c, tone);
                CrosshairHudRenderer.draw(all, from, 0, 0, 0);
                CrosshairHudRenderer.draw(all, to, 0, 0, 0);
                CrosshairHudRenderer.draw(all, to, 0, 0, 50_000_000L);
                CrosshairHudRenderer.draw(all, ClassicCrosshairType.ERROR, 0, 0, 60_000_000L);
                CrosshairHudRenderer.draw(all, ClassicCrosshairType.ERROR, 0, 0, 210_000_000L);
                if (all.inverse != 0) throw new AssertionError("Adaptive state reverted to inverse");
            }
        CrosshairOptions.setAnimation(Mode.EXPAND);
        CrosshairHudRenderer.resetAnimation();
    }

    private static final class Recording extends GuiGraphicsExtractor {
        final int tone;
        int whole, center, inverse;
        boolean fading;
        Recording(Minecraft c, int tone) { super(c, new GuiRenderState(), 0, 0); this.tone = tone; }
        @Override public void blit(RenderPipeline p, Identifier t, int x, int y, float u, float v, int w, int h, int tw, int th) {
            if (p != RenderPipelines.CROSSHAIR) throw new AssertionError("Fallback pipeline");
            inverse++;
        }
        @Override public void blit(RenderPipeline p, Identifier t, int x, int y, float u, float v, int w, int h, int tw, int th, int color) {
            if (p != RenderPipelines.GUI_TEXTURED || (color & 0xffffff) != (tone & 0xffffff))
                throw new AssertionError("Adaptive pipeline/tone");
            if (w == 15 && h == 15) { whole++; return; }
            if (w != 1 || h != 1 || u != 7 || v != 7) throw new AssertionError("Pixel shape changed");
            if (x == 7 && y == 7) { center++; if ((color >>> 24) != 255) throw new AssertionError("Center faded"); }
            if ((color >>> 24) < 255) fading = true;
        }
    }
}
