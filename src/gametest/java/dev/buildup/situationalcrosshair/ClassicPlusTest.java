package dev.buildup.situationalcrosshair;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import dev.buildup.situationalcrosshair.crosshair.ClassicCrosshairResolver;
import dev.buildup.situationalcrosshair.crosshair.ClassicCrosshairType;
import dev.buildup.situationalcrosshair.hud.CrosshairHudRenderer;
import dev.buildup.situationalcrosshair.hud.CrosshairDecorationRenderer;
import dev.buildup.situationalcrosshair.hud.SidecarSprite;
import dev.buildup.situationalcrosshair.hud.SidecarSprites;
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
                check(mapped.base() == ClassicCrosshairType.BLOCK && mapped.rightSidecar() == CrosshairPresentation.ActionSidecar.INTERACT,
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
                for (var base : ClassicCrosshairType.values()) for (var action : CrosshairPresentation.ActionSidecar.values()) {
                    for (var state : CrosshairPresentation.StateSidecar.values()) {
                        if (state != CrosshairPresentation.StateSidecar.NONE && action == CrosshairPresentation.ActionSidecar.NONE) continue;
                        var p = new CrosshairPresentation(Visibility.SHOW, base, state, action);
                        var transition = new TransitionController();
                        for (long elapsed : List.of(0L, 25_000_000L, 50_000_000L, 100_000_000L)) {
                            var frame = transition.update(p, TransitionMode.SUBTLE, elapsed);
                            var graphics = new RecordingGraphics(c);
                            CrosshairHudRenderer.drawPresentation(graphics, frame, 20, 30);
                            check(graphics.custom == 1 && graphics.vanilla == 0 && graphics.baseX == 20 && graphics.baseY == 30,
                                    "stable original base coordinates " + base + "/" + action);
                            var expected = expectedPixels(SidecarSprites.action(action), 20 + CrosshairDecorationRenderer.RIGHT_X + frame.rightOffset(), 32);
                            expected.addAll(expectedPixels(SidecarSprites.state(state), 20 + CrosshairDecorationRenderer.LEFT_X + frame.leftOffset(), 33));
                            check(graphics.foregroundPixels.equals(expected), "exact fixed-role anchor and bounded docking");
                            if (action != CrosshairPresentation.ActionSidecar.NONE)
                                check(graphics.foregroundAlpha == Math.round(255 * frame.rightOpacity() * 0.70f), "subordinate right alpha preserved");
                            if (state != CrosshairPresentation.StateSidecar.NONE)
                                check(graphics.leftAlpha == Math.round(255 * frame.leftOpacity() * 0.52f), "rarer left alpha preserved");
                        }
                    }
                }
                var attackIndicator = c.options.attackIndicator().get();
                c.options.attackIndicator().set(net.minecraft.client.AttackIndicatorStatus.CROSSHAIR);
                c.player.resetAttackStrengthTicker();
                var attackHud = record(c);
                check(attackHud.attackIndicator > 0 && attackHud.custom == 1 && attackHud.vanilla == 0,
                        "real Vanilla attack indicator survives central-sprite suppression");
                c.options.attackIndicator().set(attackIndicator);
                c.level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
                c.level.setBlock(pos.above(), Blocks.STONE.defaultBlockState(), 3);
                var blocked = PresentationResolver.resolve(ClassicCrosshairResolver.semanticState(c), CrosshairTheme.CLASSIC_PLUS);
                check(blocked.leftSidecar() == CrosshairPresentation.StateSidecar.BLOCKED && blocked.rightSidecar() == CrosshairPresentation.ActionSidecar.PLACE,
                        "real blocked placement has left state and right action");
                var blockedHud = record(c);
                check(blockedHud.leftAlpha > 0 && blockedHud.foreground > 0 && blockedHud.custom == 1, "real two-sidecar HUD composition");
                c.level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 3);
                c.level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
                var chestHit = c.hitResult;
                for (int i = 0; i < 10; i++) {
                    c.hitResult = i % 2 == 0 ? BlockHitResult.miss(Vec3.atCenterOf(pos), Direction.UP, pos) : chestHit;
                    var changedHud = record(c);
                    check(i % 2 == 0 ? changedHud.foreground == 0 : changedHud.foreground > 0 && changedHud.leftAlpha == 0,
                            "rapid actual target changes have no stale left/right sidecar");
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
                check(compact.base() == ClassicCrosshairType.ATTACK && compact.rightSidecar() == CrosshairPresentation.ActionSidecar.NONE,
                        "real loaded crossbow has ATTACK base only");
                c.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOW));
                c.player.getInventory().setItem(9, new ItemStack(Items.ARROW));
                c.player.startUsingItem(InteractionHand.MAIN_HAND);
                var charging = ClassicCrosshairResolver.semanticState(c);
                check(charging.secondary().state() == ActionState.CHARGING, "real bow charging semantic retained");
                var quiet = PresentationResolver.resolve(charging, CrosshairTheme.CLASSIC_PLUS);
                check(quiet.base() == ClassicCrosshairType.DOT && quiet.rightSidecar() == CrosshairPresentation.ActionSidecar.NONE,
                        "real bow charging in air has DOT only");
                c.player.stopUsingItem();
                for (var scenario : scenarios()) {
                    var graphics = new RecordingGraphics(c);
                    CrosshairHudRenderer.drawPresentation(graphics, TransitionController.Frame.immediate(PresentationResolver.resolve(scenario.state(), CrosshairTheme.CLASSIC_PLUS)), 20, 30);
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
                            .of("stage-5.3-gallery-scale-" + scale + (light ? "-light" : "-dark"))
                            .disableCounterPrefix().withDestinationDir(java.nio.file.Path.of("screenshots").toAbsolutePath());
                    if (scale == 3) options.withSize(1280, 960);
                    context.takeScreenshot(options);
                    if (scale == 2) {
                        context.setScreen(() -> new DockingGallery(light));
                        context.waitTicks(3);
                        context.takeScreenshot(net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions
                                .of("stage-5.3-docking-scale-2" + (light ? "-light" : "-dark"))
                                .disableCounterPrefix().withDestinationDir(java.nio.file.Path.of("screenshots").toAbsolutePath()));
                    }
                }
            } finally {
                context.runOnClient(c -> {
                    c.gui.setScreen(null);
                    c.getWindow().setWidth(originalWidth); c.getWindow().setHeight(originalHeight);
                    c.options.guiScale().set(originalScale); c.resizeGui();
                });
            }
            org.slf4j.LoggerFactory.getLogger("classic-plus-test").info("PASS: {} Classic+ HUD/command/sprite checks", checks);
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
    private static Set<String> expectedPixels(SidecarSprite sprite, int x, int y) {
        var pixels = new HashSet<String>();
        if (sprite == null) return pixels;
        try (var stream = RecordingGraphics.cResource(sprite.texture())) {
            var image = javax.imageio.ImageIO.read(stream);
            for (int row = 0; row < image.getHeight(); row++) for (int col = 0; col < image.getWidth(); col++)
                if ((image.getRGB(col, row) >>> 24) != 0) pixels.add((x + col) + ":" + (y + row));
        } catch (java.io.IOException e) { throw new AssertionError("Sprite asset missing", e); }
        return pixels;
    }
    private static void check(boolean value, String label) { if (!value) throw new AssertionError(label); checks++; }
    private static final class RecordingGraphics extends GuiGraphicsExtractor {
        int custom, vanilla, foreground, foregroundAlpha, leftAlpha, attackIndicator;
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
            if (sprite.getPath().contains("attack_indicator")) attackIndicator++;
        }
        @Override public void blit(RenderPipeline pipeline, Identifier texture, int x, int y, float u, float v,
                int width, int height, int textureWidth, int textureHeight, int color) {
            if (!texture.getNamespace().equals(BuildupSituationalCrosshairClient.MOD_ID) || !texture.getPath().startsWith("textures/gui/sidecar/")) return;
            boolean left = x < baseX;
            check(pipeline == net.minecraft.client.renderer.RenderPipelines.CROSSHAIR, "same inverse material as Classic");
            check(width == (left ? 8 : 10) && height == width && textureWidth == width && textureHeight == height,
                    "selected sprite canvas without scaling");
            check(u == 0 && v == 0, "whole sprite, no atlas cropping");
            int alpha = color >>> 24;
            check((color & 0xffffff) == (alpha << 16 | alpha << 8 | alpha), "RGB as well as alpha attenuates INVERT");
            var sprite = new SidecarSprite(texture, width);
            var pixels = expectedPixels(sprite, x, y);
            check(!pixels.isEmpty(), "actual sprite exists and has visible art");
            for (String pixel : pixels) {
                String[] coordinates = pixel.split(":");
                int dx = Integer.parseInt(coordinates[0]) - baseX, dy = Integer.parseInt(coordinates[1]) - baseY;
                if (dx < -11 || dx >= 28 || dy < 0 || dy >= 15) throw new AssertionError("sprite escaped 39x15 envelope");
                if (dx >= 0 && dx < 15) throw new AssertionError("sprite overlaps base canvas");
                if (!allDetailPixels.add(pixel)) throw new AssertionError("overlapping sprite submissions");
            }
            try (var input = cResource(texture)) {
                var image = javax.imageio.ImageIO.read(input);
                check(image.getWidth() == width && image.getHeight() == height, "actual PNG dimensions match metadata");
            } catch (java.io.IOException e) { throw new AssertionError(e); }
            try (var input = cResource(Identifier.fromNamespaceAndPath(texture.getNamespace(), texture.getPath() + ".mcmeta"))) {
                String metadata = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                check(metadata.contains("\"blur\":false"), "explicit nearest-neighbor asset metadata");
            } catch (java.io.IOException e) { throw new AssertionError(e); }
            foreground++;
            if (left) leftAlpha = alpha; else foregroundAlpha = alpha;
            foregroundPixels.addAll(pixels);
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
                new Scenario("Special", scene(TargetType.BLOCK, ActionKind.MINE, ActionState.NORMAL, ActionKind.SPECIAL, ActionState.NORMAL)),
                new Scenario("Locked UI", scene(TargetType.BLOCK, ActionKind.MINE, ActionState.NORMAL, ActionKind.INTERACT, ActionState.BLOCKED)));
    }

    /** Typical semantic scenes pass through the production policy and renderer. */
    private static final class Gallery extends Screen {
        private final boolean light;
        private final List<Scenario> scenes = scenarios();
        Gallery(boolean light) { super(Component.literal("Classic+ sidecar visual acceptance")); this.light = light; }
        @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            int text = light ? 0xff15191d : 0xfff4f4f4;
            g.fill(0, 0, width, height, light ? 0xffd4d4cc : 0xff344c32);
            g.text(font,  "Classic+ sidecars | GUI " + minecraft.options.guiScale().get() + (light ? " | light" : " | dark"), 10, 8, text);
            int cellWidth = (width - 20) / 5;
            for (int i = 0; i < scenes.size(); i++) {
                int x = 10 + i % 5 * cellWidth, y = 32 + i / 5 * 48;
                var scenario = scenes.get(i);
                g.text(font, scenario.label(), x, y, text);
                var p = PresentationResolver.resolve(scenario.state(), CrosshairTheme.CLASSIC_PLUS);
                CrosshairHudRenderer.drawPresentation(g, TransitionController.Frame.immediate(p), x + 15, y + 16);
            }
            g.text(font, "Right: action. Left: rare blocked state.", 10, height - 18, text);
        }
        @Override public boolean isPauseScreen() { return false; }
    }

    private static final class DockingGallery extends Screen {
        private final boolean light;
        private final List<List<TransitionController.Frame>> rows = new ArrayList<>();
        DockingGallery(boolean light) {
            super(Component.literal("Deterministic production docking samples")); this.light = light;
            for (var state : List.of(ActionState.NORMAL, ActionState.BLOCKED)) {
                var p = PresentationResolver.resolve(scene(TargetType.BLOCK, ActionKind.MINE, ActionState.NORMAL, ActionKind.PLACE, state), CrosshairTheme.CLASSIC_PLUS);
                var controller = new TransitionController();
                rows.add(List.of(controller.update(p, TransitionMode.SUBTLE, 0), controller.update(p, TransitionMode.SUBTLE, 25_000_000),
                        controller.update(p, TransitionMode.SUBTLE, 50_000_000), controller.update(p, TransitionMode.SUBTLE, 100_000_000)));
            }
        }
        @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
            g.fill(0, 0, width, height, light ? 0xffd4d4cc : 0xff344c32);
            int color = light ? 0xff15191d : 0xfff4f4f4;
            g.text(font, "Production docking | GUI 2 | 100ms ease-out", 10, 8, color);
            String[] times = {"0ms", "25ms", "50ms", "100ms"};
            for (int row = 0; row < 2; row++) for (int col = 0; col < 4; col++) {
                int x = 10 + col * (width - 20) / 4, y = 40 + row * 75;
                g.text(font, times[col], x, y, color);
                CrosshairHudRenderer.drawPresentation(g, rows.get(row).get(col), x + 20, y + 18);
            }
            g.text(font, "Same meaning immediately. No exit trail.", 10, height - 18, color);
        }
        @Override public boolean isPauseScreen() { return false; }
    }
}
