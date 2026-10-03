package dev.buildup.situationalcrosshair;

import dev.buildup.situationalcrosshair.crosshair.ClassicCrosshairResolver;
import dev.buildup.situationalcrosshair.crosshair.ClassicCrosshairType;
import dev.buildup.situationalcrosshair.presentation.*;
import dev.buildup.situationalcrosshair.presentation.CrosshairPresentation.ActionSidecar;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import java.nio.file.Path;

/** Server-backed scenery, real camera raycast, normal HUD (no gallery Screen). */
public final class SpriteGameplayTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksDownload();
            context.waitFor(c -> c.gui.screen() == null);
            world.getServer().runCommand("gamemode survival @a");
            world.getServer().runCommand("time set day");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("fill -12 -61 -8 12 -61 16 minecraft:grass_block");
            world.getServer().runCommand("tp @a 0.5 -60 0.5 0 20.47");
            world.getServer().runCommand("clear @a");
            context.waitFor(c -> Math.abs(c.player.getX() - 0.5) < 0.05 && Math.abs(c.player.getZ() - 0.5) < 0.05);
            int scale = context.computeOnClient(c -> c.options.guiScale().get());
            try {
                context.runOnClient(c -> {
                    PresentationOptions.theme(CrosshairTheme.CLASSIC_PLUS);
                    PresentationOptions.animation(TransitionMode.SUBTLE);
                    c.options.setCameraType(CameraType.FIRST_PERSON);
                    if (c.gui.hud.isHidden()) c.gui.hud.toggle();
                    c.options.guiScale().set(2); c.resizeGui();
                    c.player.input.keyPresses = net.minecraft.world.entity.player.Input.EMPTY;
                });
                world.getServer().runCommand("setblock 0 -60 3 minecraft:dirt");
                captureBlock(context, "normal-block", ActionSidecar.NONE);
                world.getServer().runCommand("setblock 0 -60 3 minecraft:chest");
                captureBlock(context, "chest", ActionSidecar.INTERACT);
                world.getServer().runCommand("setblock 0 -60 3 minecraft:oak_log");
                world.getServer().runCommand("item replace entity @a weapon.mainhand with minecraft:diamond_axe");
                captureBlock(context, "transform-log", ActionSidecar.TRANSFORM);
                world.getServer().runCommand("setblock 0 -60 3 minecraft:air");
                world.getServer().runCommand("clear @a");
                world.getServer().runCommand("summon minecraft:villager 0.5 -60 3.5 {NoAI:1b,Silent:1b}");
                context.runOnClient(c -> c.player.setXRot(9.83f));
                context.waitFor(c -> c.hitResult instanceof EntityHitResult &&
                        PresentationResolver.resolve(ClassicCrosshairResolver.semanticState(c), CrosshairTheme.CLASSIC_PLUS)
                                .rightSidecar() == ActionSidecar.INTERACT);
                capture(context, "villager", ClassicCrosshairType.ATTACK, ActionSidecar.INTERACT);
            } finally {
                context.runOnClient(c -> { c.options.guiScale().set(scale); c.resizeGui(); });
            }
        }
        org.slf4j.LoggerFactory.getLogger("sprite-gameplay-test").info("PASS: 4 server-backed gameplay screenshots with actual raycast and production HUD");
    }
    private static void captureBlock(ClientGameTestContext context, String name, ActionSidecar action) {
        context.waitFor(c -> c.hitResult instanceof BlockHitResult hit && hit.getBlockPos().getZ() == 3 &&
                PresentationResolver.resolve(ClassicCrosshairResolver.semanticState(c), CrosshairTheme.CLASSIC_PLUS).rightSidecar() == action);
        capture(context, name, ClassicCrosshairType.BLOCK, action);
    }
    private static void capture(ClientGameTestContext context, String name, ClassicCrosshairType base, ActionSidecar action) {
        context.waitTicks(8); // Real HUD's SUBTLE entry is settled, not a constructed frame.
        context.runOnClient(c -> {
            if (c.gui.screen() != null) throw new AssertionError("Gameplay evidence has a synthetic screen");
            var p = PresentationResolver.resolve(ClassicCrosshairResolver.semanticState(c), CrosshairTheme.CLASSIC_PLUS);
            if (p.base() != base || p.rightSidecar() != action) throw new AssertionError("Wrong real gameplay scene: " + name);
        });
        context.takeScreenshot(TestScreenshotOptions.of("stage-5.3-gameplay-" + name)
                .disableCounterPrefix().withDestinationDir(Path.of("screenshots").toAbsolutePath()));
    }
}
