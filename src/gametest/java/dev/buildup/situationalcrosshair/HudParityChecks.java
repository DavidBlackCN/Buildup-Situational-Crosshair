package dev.buildup.situationalcrosshair;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/** Observe actual transformed HUD draw submissions, not a duplicate visibility rule. */
final class HudParityChecks {
    static void run(Minecraft client) {
        client.options.setCameraType(CameraType.FIRST_PERSON);
        if (client.gui.hud.isHidden()) client.gui.hud.toggle();
        check(client, 1, 0, "normal first person");
        client.gui.hud.toggle();
        check(client, 0, 0, "F1");
        client.gui.hud.toggle();
        client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        check(client, 0, 0, "third person");
        client.options.setCameraType(CameraType.FIRST_PERSON);
        client.gameMode.setLocalMode(GameType.SPECTATOR);
        check(client, 0, 0, "spectator air");
        client.gameMode.setLocalMode(GameType.SURVIVAL);
        client.debugEntries.setStatus(DebugScreenEntries.THREE_DIMENSIONAL_CROSSHAIR, DebugScreenEntryStatus.ALWAYS_ON);
        check(client, 0, 0, "debug 3D crosshair");
        client.debugEntries.setStatus(DebugScreenEntries.THREE_DIMENSIONAL_CROSSHAIR, DebugScreenEntryStatus.NEVER);
        client.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SPYGLASS));
        client.player.startUsingItem(InteractionHand.MAIN_HAND);
        // 26.3 still submits its central sprite while scoping. Follow vanilla.
        check(client, 1, 0, "spyglass follows vanilla");
        client.player.stopUsingItem();
        client.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        for (var mode : dev.buildup.situationalcrosshair.crosshair.CrosshairTransition.Mode.values()) {
            command(client, "crosshair animation " + mode.name().toLowerCase(java.util.Locale.ROOT));
            if (CrosshairOptions.animation() != mode) throw new AssertionError("Animation command");
        }
        command(client, "crosshair animation off");
        command(client, "crosshair disable");
        if (CrosshairOptions.enabled()) throw new AssertionError("Disable command");
        check(client, 0, 1, "disabled restores Vanilla");
        command(client, "crosshair enable");
        if (!CrosshairOptions.enabled()) throw new AssertionError("Enable command");
        check(client, 1, 0, "re-enabled immediately");
        var indicator = client.options.attackIndicator().get();
        client.options.attackIndicator().set(net.minecraft.client.AttackIndicatorStatus.CROSSHAIR);
        client.player.resetAttackStrengthTicker();
        var attack = new RecordingGraphics(client);
        client.gui.hud.extractRenderState(attack, DeltaTracker.ONE);
        if (attack.attackIndicator == 0 || attack.custom != 1 || attack.vanilla != 0)
            throw new AssertionError("Vanilla attack indicator must survive replacement");
        client.options.attackIndicator().set(indicator);
        int originalScale = client.options.guiScale().get();
        try {
            for (int scale = 1; scale <= 3; scale++) {
                client.options.guiScale().set(scale);
                client.resizeGui();
                check(client, 1, 0, "GUI scale " + scale);
            }
        } finally {
            client.options.guiScale().set(originalScale);
            client.resizeGui();
        }
        // Exercise the actual wrapped HUD with animation enabled, not only the drawing helper.
        CrosshairOptions.setAnimation(dev.buildup.situationalcrosshair.crosshair.CrosshairTransition.Mode.EXPAND);
        check(client, 1, 0, "animated HUD first frame");
        var airTarget = client.hitResult;
        client.hitResult = new net.minecraft.world.phys.EntityHitResult(client.player);
        var animated = new RecordingGraphics(client);
        client.gui.hud.extractRenderState(animated, DeltaTracker.ONE);
        if (animated.animatedPixels == 0 || animated.animatedCenter != 1 || animated.vanilla != 0 || animated.custom != 0)
            throw new AssertionError("Animated HUD must replace only the Vanilla center");
        CrosshairOptions.setEnabled(false);
        check(client, 0, 1, "disable during animation");
        CrosshairOptions.setEnabled(true);
        CrosshairOptions.setAnimation(dev.buildup.situationalcrosshair.crosshair.CrosshairTransition.Mode.OFF);
        client.hitResult = airTarget;
        var target = client.hitResult;
        client.hitResult = null;
        check(client, 0, 1, "null target vanilla fallback");
        client.hitResult = target;
        org.slf4j.LoggerFactory.getLogger("classic-parity-test").info("PASS: HUD visibility, enable switch, attack indicator and GUI scales 1/2/3");
    }

    private static void command(Minecraft client, String command) {
        var dispatcher = net.fabricmc.fabric.api.client.command.v2.ClientCommands.getActiveDispatcher();
        if (dispatcher.getRoot().getChild("crosshair").getChildren().size() != 3)
            throw new AssertionError("Expected enable/disable/animation commands");
        try {
            if (dispatcher.execute(command, (net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource)
                    client.getConnection().getSuggestionsProvider()) != 1)
                throw new AssertionError("Command failed: " + command);
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException ex) {
            throw new AssertionError(command, ex);
        }
    }

    static void check(Minecraft client, int custom, int vanilla, String label) {
        var graphics = new RecordingGraphics(client);
        client.gui.hud.extractRenderState(graphics, DeltaTracker.ONE);
        if (graphics.custom != custom || graphics.vanilla != vanilla) {
            throw new AssertionError(label + ": custom=" + graphics.custom + ", vanilla=" + graphics.vanilla);
        }
    }

    private static final class RecordingGraphics extends GuiGraphicsExtractor {
        int custom;
        int vanilla;
        int attackIndicator;
        int animatedPixels, animatedCenter;
        final java.util.Set<Long> drawnPixels = new java.util.HashSet<>();

        RecordingGraphics(Minecraft client) {
            super(client, new GuiRenderState(), 0, 0);
        }

        @Override
        public void blit(RenderPipeline pipeline, Identifier texture, int x, int y,
                         float u, float v, int width, int height, int textureWidth, int textureHeight) {
            if (texture.getNamespace().equals(BuildupSituationalCrosshairClient.MOD_ID)) {
                custom++;
                if (x != (guiWidth() - 15) / 2 || y != (guiHeight() - 15) / 2)
                    throw new AssertionError("Classic crosshair is not centered");
                if (width != 15 || height != 15 || textureWidth != 15 || textureHeight != 15) {
                    throw new AssertionError("Classic dimensions changed");
                }
                if (Minecraft.getInstance().getResourceManager().getResource(texture).isEmpty()) {
                    throw new AssertionError("Missing Classic texture: " + texture);
                }
            }
        }

        @Override public void blit(RenderPipeline pipeline, Identifier texture, int x, int y,
                                   float u, float v, int w, int h, int tw, int th, int color) {
            if (!texture.getNamespace().equals(BuildupSituationalCrosshairClient.MOD_ID)) return;
            animatedPixels++;
            if (!drawnPixels.add(((long) x << 32) | (y & 0xffffffffL)))
                throw new AssertionError("Double inverse blend in actual HUD");
            if (x == (guiWidth() - 15) / 2 + 7 && y == (guiHeight() - 15) / 2 + 7) {
                animatedCenter++;
                if (color != 0xffffffff) throw new AssertionError("Animated center faded");
            }
        }

        @Override
        public void blitSprite(RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height) {
            if (sprite.getPath().contains("attack_indicator")) attackIndicator++;
            if (sprite.equals(Identifier.withDefaultNamespace("hud/crosshair"))) vanilla++;
        }
    }
}
