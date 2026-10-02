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
                for (var base : ClassicCrosshairType.values()) for (var action : CrosshairPresentation.SecondaryModifier.values()) {
                    var p = new CrosshairPresentation(Visibility.SHOW, base, action);
                    var graphics = new RecordingGraphics(c);
                    CrosshairHudRenderer.drawPresentation(graphics, p, 20, 30, 0.5f);
                    check(graphics.custom == 1 && graphics.vanilla == 0, "one stable base for " + base + "/" + action);
                    var glyph = PixelGlyph.modifier(action);
                    check(graphics.foregroundPixels.equals(expectedPixels(glyph, 20 + CrosshairDecorationRenderer.ACTION_X,
                            30 + CrosshairDecorationRenderer.ACTION_Y)), "exact micro foreground " + base + "/" + action);
                    if (glyph != null) check(graphics.foregroundAlpha == 128, "micro fade alpha survives extraction");
                }
                // Exercise native provenance through actual item components and capture.
                c.hitResult = BlockHitResult.miss(c.player.getEyePosition().add(0, 0, 5), Direction.UP, c.player.blockPosition());
                var crossbow = new ItemStack(Items.CROSSBOW);
                crossbow.set(net.minecraft.core.component.DataComponents.CHARGED_PROJECTILES,
                        net.minecraft.world.item.component.ChargedProjectiles.of(new net.minecraft.world.item.ItemStackTemplate(Items.ARROW)));
                c.player.setItemInHand(InteractionHand.MAIN_HAND, crossbow);
                var ready = ClassicCrosshairResolver.semanticState(c);
                check(ready.secondary().action() == ActionKind.USE && ready.secondary().state() == ActionState.READY, "real loaded crossbow remains USE/READY");
                var compact = PresentationResolver.resolve(ready, CrosshairTheme.CLASSIC_PLUS);
                check(compact.base() == ClassicCrosshairType.ATTACK && compact.modifier() == CrosshairPresentation.SecondaryModifier.NONE,
                        "real loaded crossbow has ATTACK base only");
                c.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOW));
                c.player.getInventory().setItem(9, new ItemStack(Items.ARROW));
                c.player.startUsingItem(InteractionHand.MAIN_HAND);
                var charging = ClassicCrosshairResolver.semanticState(c);
                check(charging.secondary().state() == ActionState.CHARGING, "real bow charging semantic retained");
                var quiet = PresentationResolver.resolve(charging, CrosshairTheme.CLASSIC_PLUS);
                check(quiet.base() == ClassicCrosshairType.DOT && quiet.modifier() == CrosshairPresentation.SecondaryModifier.NONE,
                        "real bow charging in air has DOT only");
                c.player.stopUsingItem();
                for (var scenario : scenarios()) {
                    var graphics = new RecordingGraphics(c);
                    CrosshairHudRenderer.drawPresentation(graphics, PresentationResolver.resolve(scenario.state(), CrosshairTheme.CLASSIC_PLUS), 20, 30, 1);
                    check(graphics.custom == 1, "gallery uses production base: " + scenario.label());
                }
            });
            int originalScale = context.computeOnClient(c -> c.options.guiScale().get());
            int originalWidth = context.computeOnClient(c -> c.getWindow().getWidth());
            int originalHeight = context.computeOnClient(c -> c.getWindow().getHeight());
            try {
                for (int scale : List.of(1, 2, 3)) for (boolean light : List.of(false, true)) {
                    context.runOnClient(c -> {
                        // Recalculate GUI dimensions before Screen initialization.
                        // Screenshot withSize alone enlarges the framebuffer but
                        // leaves the GUI at its earlier scale/dimensions.
                        if (scale == 3) { c.getWindow().setWidth(1280); c.getWindow().setHeight(960); }
                        c.options.guiScale().set(scale); c.resizeGui();
                        check(c.getWindow().getGuiScale() == scale, "gallery actual GUI scale " + scale);
                    });
                    context.setScreen(() -> new Gallery(light));
                    context.waitTicks(3);
                    // Fabric's isolated game directory is temporary. Save under
                    // the Gradle run working directory so evidence survives exit.
                    var options = net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions
                            .of("stage-5.1-gallery-scale-" + scale + (light ? "-light" : "-dark"))
                            .disableCounterPrefix().withDestinationDir(java.nio.file.Path.of("screenshots").toAbsolutePath());
                    if (scale == 3) options.withSize(1280, 960);
                    context.takeScreenshot(options);
                }
            } finally {
                context.runOnClient(c -> {
                    c.gui.setScreen(null);
                    c.getWindow().setWidth(originalWidth); c.getWindow().setHeight(originalHeight);
                    c.options.guiScale().set(originalScale); c.resizeGui();
                });
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
        if (glyph == null) return pixels;
        for (int row = 0; row < PixelGlyph.SIZE; row++) for (int col = 0; col < PixelGlyph.SIZE; col++)
            if (glyph.at(col, row)) pixels.add((x + col) + ":" + (y + row));
        return pixels;
    }
    private static void check(boolean value, String label) { if (!value) throw new AssertionError(label); checks++; }
    private static final class RecordingGraphics extends GuiGraphicsExtractor {
        int custom, vanilla, foreground, foregroundAlpha;
        int baseX, baseY;
        java.awt.image.BufferedImage baseMask;
        final Set<String> allDetailPixels = new HashSet<>();
        final Set<String> foregroundPixels = new HashSet<>();
        RecordingGraphics(Minecraft c) { super(c, new GuiRenderState(), 0, 0); }
        @Override public void blit(RenderPipeline pipeline, Identifier texture, int x, int y, float u, float v,
                int width, int height, int textureWidth, int textureHeight) {
            if (texture.getNamespace().equals(BuildupSituationalCrosshairClient.MOD_ID)) {
                custom++;
                baseX = x; baseY = y;
                try (var stream = cResource(texture)) {
                    baseMask = javax.imageio.ImageIO.read(stream);
                    check(baseMask.getWidth() == 15 && baseMask.getHeight() == 15, "actual PNG mask dimensions");
                } catch (java.io.IOException e) { throw new AssertionError("Classic mask unavailable", e); }
                check(width == 15 && height == 15 && textureWidth == 15 && textureHeight == 15, "base remains 15x15");
            }
        }
        private static java.io.InputStream cResource(Identifier texture) throws java.io.IOException {
            return Minecraft.getInstance().getResourceManager().getResourceOrThrow(texture).open();
        }
        @Override public void blitSprite(RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height) {
            if (sprite.equals(Identifier.withDefaultNamespace("hud/crosshair"))) vanilla++;
        }
        @Override public void fill(int x1, int y1, int x2, int y2, int color) {
            if ((color & 0xffffff) == 0xf4f4f4 || (color & 0xffffff) == 0x101010) {
                int dx = x1 - baseX, dy = y1 - baseY;
                if (x2 != x1 + 1 || y2 != y1 + 1) throw new AssertionError("micro pixels changed size");
                if (dx < 0 || dy < 0 || dx >= 15 || dy >= 15) throw new AssertionError("detail escaped 15x15 canvas");
                if ((baseMask.getRGB(dx, dy) >>> 24) != 0) throw new AssertionError("detail collided with actual Classic opaque mask");
                if (!allDetailPixels.add(x1 + ":" + y1)) throw new AssertionError("overlapping detail submissions change fade alpha");
            }
            if ((color & 0xffffff) == 0xf4f4f4) {
                foreground++; foregroundAlpha = color >>> 24; foregroundPixels.add(x1 + ":" + y1);
                if (x2 != x1 + 1 || y2 != y1 + 1) throw new AssertionError("detail pixels changed size");
            }
        }
    }

    private record Scenario(String label, CrosshairSemanticState state) { }
    private static CrosshairSemanticState scene(TargetType target, ActionKind primary, ActionState primaryState, ActionKind secondary, ActionState state) {
        var a = new ActionCandidate(ActionSlot.PRIMARY, primary, primaryState, CandidateSource.VANILLA_RUNTIME,
                Specificity.EXACT_CONTEXT, Confidence.EXACT, 0, "test:primary");
        var b = new ActionCandidate(ActionSlot.SECONDARY, secondary, state, CandidateSource.VANILLA_RUNTIME,
                Specificity.EXACT_CONTEXT, Confidence.EXACT, 0,
                state == ActionState.READY ? "buildup_situational_crosshair:crossbow/main" : "test:secondary");
        var hand = state == ActionState.READY ? new CrosshairContextSnapshot.HandState(CrosshairContextSnapshot.RangedItem.CROSSBOW, true, false)
                : CrosshairContextSnapshot.HandState.EMPTY;
        return new CrosshairSemanticState(target, Visibility.SHOW, CrosshairSemanticState.ResolvedAction.of(a),
                CrosshairSemanticState.ResolvedAction.of(b), List.of(a, b), hand, CrosshairContextSnapshot.HandState.EMPTY);
    }
    private static List<Scenario> scenarios() {
        return List.of(
                new Scenario("Air", scene(TargetType.MISS, ActionKind.NONE, ActionState.NORMAL, ActionKind.NONE, ActionState.NORMAL)),
                new Scenario("Block", scene(TargetType.BLOCK, ActionKind.MINE, ActionState.NORMAL, ActionKind.NONE, ActionState.NORMAL)),
                new Scenario("Bad tool", scene(TargetType.BLOCK, ActionKind.MINE, ActionState.INVALID, ActionKind.NONE, ActionState.NORMAL)),
                new Scenario("Entity", scene(TargetType.ENTITY, ActionKind.ATTACK, ActionState.NORMAL, ActionKind.NONE, ActionState.NORMAL)),
                new Scenario("Chest", scene(TargetType.BLOCK, ActionKind.MINE, ActionState.NORMAL, ActionKind.INTERACT, ActionState.NORMAL)),
                new Scenario("Place", scene(TargetType.BLOCK, ActionKind.MINE, ActionState.NORMAL, ActionKind.PLACE, ActionState.NORMAL)),
                new Scenario("Convert", scene(TargetType.BLOCK, ActionKind.MINE, ActionState.NORMAL, ActionKind.TRANSFORM, ActionState.NORMAL)),
                new Scenario("Crop use", scene(TargetType.BLOCK, ActionKind.MINE, ActionState.NORMAL, ActionKind.USE, ActionState.NORMAL)),
                new Scenario("Trade", scene(TargetType.ENTITY, ActionKind.ATTACK, ActionState.NORMAL, ActionKind.INTERACT, ActionState.NORMAL)),
                new Scenario("Bow draw", scene(TargetType.MISS, ActionKind.NONE, ActionState.NORMAL, ActionKind.USE, ActionState.CHARGING)),
                new Scenario("Xbow rdy", scene(TargetType.MISS, ActionKind.NONE, ActionState.NORMAL, ActionKind.USE, ActionState.READY)),
                new Scenario("Atk CD", scene(TargetType.ENTITY, ActionKind.ATTACK, ActionState.COOLDOWN, ActionKind.NONE, ActionState.NORMAL)),
                new Scenario("Blocked", scene(TargetType.BLOCK, ActionKind.MINE, ActionState.NORMAL, ActionKind.PLACE, ActionState.BLOCKED)),
                new Scenario("Invalid", scene(TargetType.BLOCK, ActionKind.MINE, ActionState.NORMAL, ActionKind.PLACE, ActionState.INVALID)),
                new Scenario("Special", scene(TargetType.BLOCK, ActionKind.MINE, ActionState.NORMAL, ActionKind.SPECIAL, ActionState.NORMAL)));
    }

    /** Typical semantic scenes pass through the production policy and renderer. */
    private static final class Gallery extends Screen {
        private final boolean light;
        private final List<Scenario> scenes = scenarios();
        Gallery(boolean light) { super(Component.literal("Classic+ micro visual acceptance")); this.light = light; }
        @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            int text = light ? 0xff15191d : 0xfff4f4f4;
            g.fill(0, 0, width, height, light ? 0xffd4d4cc : 0xff344c32);
            g.text(font, "Classic+ micro | GUI " + minecraft.options.guiScale().get() + (light ? " | light" : " | dark"), 10, 8, text);
            int cellWidth = (width - 20) / 5;
            for (int i = 0; i < scenes.size(); i++) {
                int x = 10 + i % 5 * cellWidth, y = 32 + i / 5 * 48;
                var scenario = scenes.get(i);
                g.text(font, scenario.label(), x, y, text);
                var p = PresentationResolver.resolve(scenario.state(), CrosshairTheme.CLASSIC_PLUS);
                CrosshairHudRenderer.drawPresentation(g, p, x + 15, y + 16, 1);
            }
            g.text(font, "One optional micro hint. No status icons.", 10, height - 18, text);
        }
        @Override public boolean isPauseScreen() { return false; }
    }
}
