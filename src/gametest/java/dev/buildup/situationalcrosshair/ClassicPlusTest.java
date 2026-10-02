package dev.buildup.situationalcrosshair;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.buildup.situationalcrosshair.crosshair.ClassicCrosshairResolver;
import dev.buildup.situationalcrosshair.crosshair.ClassicCrosshairType;
import dev.buildup.situationalcrosshair.hud.CrosshairHudRenderer;
import dev.buildup.situationalcrosshair.hud.CrosshairDecorationRenderer;
import dev.buildup.situationalcrosshair.presentation.*;
import dev.buildup.situationalcrosshair.semantic.*;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.*;

public final class ClassicPlusTest implements FabricClientGameTest {
    private static int checks;
    @Override public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksDownload();
            context.waitFor(c -> c.gui.screen() == null);
            world.getServer().runCommand("gamemode survival @a");
            context.waitFor(c -> !c.player.isSpectator() && !c.player.isCreative());
            context.runOnClient(c -> {
                command(c, "crosshair theme classic_plus");
                command(c, "crosshair animation off");
                check(PresentationOptions.theme() == CrosshairTheme.CLASSIC_PLUS && PresentationOptions.animation() == TransitionMode.OFF,
                        "actual client commands select theme and OFF");
                c.player.input.keyPresses = net.minecraft.world.entity.player.Input.EMPTY;
                c.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                if (c.gui.hud.isHidden()) c.gui.hud.toggle();
                var pos = c.player.blockPosition().offset(2, 0, 0);
                c.level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 3);
                c.level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
                c.hitResult = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
                c.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COBBLESTONE));
                c.player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                var mapped = PresentationResolver.resolve(ClassicCrosshairResolver.semanticState(c), PresentationOptions.theme());
                check(mapped.base() == ClassicCrosshairType.BLOCK && mapped.modifier() == CrosshairPresentation.SecondaryModifier.INTERACT,
                        "real chest shows BLOCK + INTERACT");
                var hud = record(c);
                check(hud.custom == 1 && hud.vanilla == 0 && hud.foreground > 0, "one custom base plus details, no vanilla duplication");
                c.gui.hud.toggle();
                var hidden = record(c);
                check(hidden.custom == 0 && hidden.foreground == 0 && hidden.vanilla == 0, "F1 has no detail ghost");
                c.gui.hud.toggle();
                c.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
                check(record(c).foreground == 0, "third person has no decorations");
                c.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                var target = c.hitResult; c.hitResult = null;
                var fallback = record(c);
                check(fallback.custom == 0 && fallback.foreground == 0 && fallback.vanilla == 1, "vanilla fallback has no stale decorations");
                c.hitResult = target;
                command(c, "crosshair theme classic");
                check(record(c).foreground == 0 && record(c).custom == 1, "Classic removes all modifiers");
                command(c, "crosshair theme classic_plus");
                command(c, "crosshair animation subtle");
                check(PresentationOptions.animation() == TransitionMode.SUBTLE, "SUBTLE selectable through client command");
                for (var action : CrosshairPresentation.SecondaryModifier.values()) {
                    if (action == CrosshairPresentation.SecondaryModifier.NONE) continue;
                    var p = new CrosshairPresentation(Visibility.SHOW, ClassicCrosshairType.BLOCK, action, ActionState.NORMAL, ActionState.NORMAL);
                    var graphics = new RecordingGraphics(c);
                    CrosshairHudRenderer.drawPresentation(graphics, p, 20, 30, 1);
                    check(graphics.custom == 1 && graphics.vanilla == 0, "stable base for " + action);
                    check(graphics.foregroundPixels.equals(expectedPixels(PixelGlyph.modifier(action), 20 + CrosshairDecorationRenderer.ACTION_X,
                            30 + CrosshairDecorationRenderer.ACTION_Y)), "actual action pixel submission " + action);
                }
                for (var status : ActionState.values()) {
                    if (status == ActionState.NORMAL) continue;
                    var p = new CrosshairPresentation(Visibility.SHOW, ClassicCrosshairType.ATTACK, CrosshairPresentation.SecondaryModifier.NONE,
                            status, ActionState.NORMAL);
                    var graphics = new RecordingGraphics(c);
                    CrosshairHudRenderer.drawPresentation(graphics, p, 20, 30, 0.5f);
                    check(graphics.foregroundPixels.equals(expectedPixels(PixelGlyph.status(status), 20 + CrosshairDecorationRenderer.PRIMARY_STATUS_X,
                            30 + CrosshairDecorationRenderer.PRIMARY_STATUS_Y)), "actual primary status pixels " + status);
                    check(graphics.foregroundAlpha == 128, "detail alpha survives GUI extraction " + status);
                }
            });
            int originalScale = context.computeOnClient(c -> c.options.guiScale().get());
            try {
                context.runOnClient(c -> { c.options.guiScale().set(1); c.resizeGui(); });
                context.setScreen(Gallery::new);
                context.waitTicks(3);
                context.takeScreenshot("stage-5-gallery-scale-1");
                context.runOnClient(c -> { c.options.guiScale().set(2); c.resizeGui(); });
                context.waitTicks(3);
                context.takeScreenshot("stage-5-gallery-scale-2");
            } finally {
                context.runOnClient(c -> { c.gui.setScreen(null); c.options.guiScale().set(originalScale); c.resizeGui(); });
            }
            org.slf4j.LoggerFactory.getLogger("classic-plus-test").info("PASS: {} Classic+ HUD/command/pixel checks", checks);
        }
    }
    private static void command(Minecraft c, String command) {
        try { check(ClientCommands.getActiveDispatcher().execute(command, (FabricClientCommandSource) c.getConnection().getSuggestionsProvider()) == 1,
                "command executed: " + command); }
        catch (com.mojang.brigadier.exceptions.CommandSyntaxException ex) { throw new AssertionError(command, ex); }
    }
    private static RecordingGraphics record(Minecraft c) {
        var graphics = new RecordingGraphics(c); c.gui.hud.extractRenderState(graphics, DeltaTracker.ONE); return graphics;
    }
    private static Set<String> expectedPixels(PixelGlyph glyph, int x, int y) {
        var pixels = new HashSet<String>();
        for (int row = 0; row < 5; row++) for (int col = 0; col < 5; col++) if (glyph.at(col, row)) pixels.add((x + col) + ":" + (y + row));
        return pixels;
    }
    private static void check(boolean value, String label) { if (!value) throw new AssertionError(label); checks++; }
    private static final class RecordingGraphics extends GuiGraphicsExtractor {
        int custom, vanilla, foreground, foregroundAlpha;
        int baseX, baseY;
        final Set<String> foregroundPixels = new HashSet<>();
        RecordingGraphics(Minecraft c) { super(c, new GuiRenderState(), 0, 0); }
        @Override public void blit(RenderPipeline pipeline, Identifier texture, int x, int y, float u, float v,
                int width, int height, int textureWidth, int textureHeight) {
            if (texture.getNamespace().equals(BuildupSituationalCrosshairClient.MOD_ID)) {
                custom++;
                baseX = x; baseY = y;
                check(width == 15 && height == 15 && textureWidth == 15 && textureHeight == 15, "base remains 15x15");
            }
        }
        @Override public void blitSprite(RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height) {
            if (sprite.equals(Identifier.withDefaultNamespace("hud/crosshair"))) vanilla++;
        }
        @Override public void fill(int x1, int y1, int x2, int y2, int color) {
            if (((color & 0xffffff) == 0xf4f4f4 || (color & 0xffffff) == 0x101010)
                    && x1 >= baseX + 4 && x1 <= baseX + 10 && y1 >= baseY + 4 && y1 <= baseY + 10)
                throw new AssertionError("Decoration overlapped original Classic pixels");
            if ((color & 0xffffff) == 0xf4f4f4) {
                foreground++; foregroundAlpha = color >>> 24; foregroundPixels.add(x1 + ":" + y1);
                if (x2 != x1 + 1 || y2 != y1 + 1) throw new AssertionError("detail pixels changed size");
            }
        }
    }

    /** Test-only gallery draws through the production renderer on contrasting backgrounds. */
    private static final class Gallery extends Screen {
        Gallery() { super(Component.literal("Classic+ visual acceptance")); }
        @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            g.fill(0, 0, width, height, 0xff15191d);
            g.text(font, "Classic+ | actual renderer | GUI scale " + minecraft.options.guiScale().get(), 10, 8, 0xfff4f4f4);
            String[] labels = {"Interact", "Use", "Place", "Transform", "Special"};
            var modifiers = List.of(CrosshairPresentation.SecondaryModifier.INTERACT, CrosshairPresentation.SecondaryModifier.USE,
                    CrosshairPresentation.SecondaryModifier.PLACE, CrosshairPresentation.SecondaryModifier.TRANSFORM, CrosshairPresentation.SecondaryModifier.SPECIAL);
            var statuses = List.of(ActionState.CHARGING, ActionState.READY, ActionState.COOLDOWN, ActionState.BLOCKED, ActionState.INVALID);
            for (int row = 0; row < 2; row++) {
                int y = 30 + row * 100;
                g.fill(8, y, width - 8, y + 90, row == 0 ? 0xff344c32 : 0xffd4d4cc);
                int text = row == 0 ? 0xfff4f4f4 : 0xff15191d;
                for (int col = 0; col < 5; col++) {
                    int x = 20 + col * (width - 40) / 5;
                    g.text(font, labels[col], x, y + 5, text);
                    var p = new CrosshairPresentation(Visibility.SHOW, ClassicCrosshairType.BLOCK, modifiers.get(col), ActionState.NORMAL, ActionState.NORMAL);
                    CrosshairHudRenderer.drawPresentation(g, p, x + 6, y + 19, 1);
                    g.text(font, statuses.get(col).name().toLowerCase(Locale.ROOT), x, y + 45, text);
                    p = new CrosshairPresentation(Visibility.SHOW, ClassicCrosshairType.ATTACK, CrosshairPresentation.SecondaryModifier.USE,
                            ActionState.COOLDOWN, statuses.get(col));
                    CrosshairHudRenderer.drawPresentation(g, p, x + 6, y + 59, 1);
                }
            }
        }
        @Override public boolean isPauseScreen() { return false; }
    }
}
