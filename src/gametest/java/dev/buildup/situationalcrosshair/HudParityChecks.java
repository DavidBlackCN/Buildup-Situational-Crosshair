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
        var target = client.hitResult;
        client.hitResult = null;
        check(client, 0, 1, "null target vanilla fallback");
        client.hitResult = target;
        org.slf4j.LoggerFactory.getLogger("classic-parity-test").info("PASS: 7 transformed HUD visibility/submission cases");
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

        RecordingGraphics(Minecraft client) {
            super(client, new GuiRenderState(), 0, 0);
        }

        @Override
        public void blit(RenderPipeline pipeline, Identifier texture, int x, int y,
                         float u, float v, int width, int height, int textureWidth, int textureHeight) {
            if (texture.getNamespace().equals(BuildupSituationalCrosshairClient.MOD_ID)) {
                custom++;
                if (width != 15 || height != 15 || textureWidth != 15 || textureHeight != 15) {
                    throw new AssertionError("Classic dimensions changed");
                }
                if (Minecraft.getInstance().getResourceManager().getResource(texture).isEmpty()) {
                    throw new AssertionError("Missing Classic texture: " + texture);
                }
            }
        }

        @Override
        public void blitSprite(RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height) {
            if (sprite.equals(Identifier.withDefaultNamespace("hud/crosshair"))) vanilla++;
        }
    }
}
