package dev.buildup.situationalcrosshair;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.buildup.situationalcrosshair.crosshair.ClassicCrosshairResolver;
import dev.buildup.situationalcrosshair.rules.RuleManager;
import dev.buildup.situationalcrosshair.semantic.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.util.InclusiveRange;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import java.nio.file.*;
import java.util.*;

/** Actual pack repository/reload lifecycle, not a fake RuleManager replacement. */
public final class RuleReloadTest implements FabricClientGameTest {
    private static int checks;
    private static final String RULE = "assets/testpack/crosshair_rules/nested/reload.json";
    @Override public void runTest(ClientGameTestContext context) {
        var original = context.computeOnClient(c -> List.copyOf(c.getResourcePackRepository().getSelectedIds()));
        var directory = context.computeOnClient(Minecraft::getResourcePackDirectory);
        try {
            Files.createDirectories(directory);
            var low = Files.createTempDirectory(directory, "bsc-stage4-low-");
            var high = Files.createTempDirectory(directory, "bsc-stage4-high-");
            metadata(low); metadata(high);
            write(low, RULE, rule("transform"));
            write(high, RULE, rule("use"));
            write(high, "assets/testpack/crosshair_rules/bad.json", "{\"schema\":99}");
            write(high, "assets/testpack/crosshair_rules/absent.json", """
                    {"schema":1,"mode":"override","requires":{"mods":["bsc_test_absent"]},
                    "emit":[{"slot":"primary","action":"special"}]}
                    """);
            write(high, "assets/testpack/crosshair_rules/tags.json", """
                    {"schema":1,"mode":"override","when":{
                    "target":{"type":"block","tags":["minecraft:logs"],"namespace":"minecraft","properties":{"axis":"y"}},
                    "held":{"hand":"off","tags":["minecraft:planks"]},
                    "player":{"sneaking":true,"creative":false,"using_item":false}},
                    "emit":[{"slot":"secondary","action":"special"}]}
                    """);
            write(high, "assets/testpack/crosshair_rules/entity.json", """
                    {"schema":1,"mode":"override","when":{"target":{"type":"entity","ids":["minecraft:zombie"]}},
                    "emit":[{"slot":"primary","action":"special"}]}
                    """);
            var selected = new ArrayList<>(original);
            selected.add("file/" + low.getFileName()); selected.add("file/" + high.getFileName());
            context.runOnClient(c -> { c.getResourcePackRepository().reload(); c.getResourcePackRepository().setSelected(selected); });
            reload(context);
            check(RuleManager.status().rules().size() == 3, "pack override deduplicates same resource ID");
            check(RuleManager.status().rejected() == 1 && RuleManager.status().inactive() == 1, "invalid isolated; missing optional mod inactive");
            try (var world = context.worldBuilder().create()) {
                world.getConnection().waitForChunksDownload();
                context.waitFor(c -> c.gui.screen() == null);
                world.getServer().runCommand("gamemode survival @a");
                context.waitFor(c -> c.player.gameMode() == GameType.SURVIVAL);
                context.runOnClient(c -> {
                    prepareStone(c);
                    var state = ClassicCrosshairResolver.semanticState(c);
                    check(state.secondary().action() == ActionKind.USE, "higher pack wins");
                    check(state.secondary().evidence().orElseThrow().origin().equals("testpack:nested/reload"), "stable path-derived ID");
                    check(state.primary().action() == ActionKind.MINE, "override leaves primary intact");
                    var pos = c.player.blockPosition().offset(2, 0, 0);
                    c.level.setBlock(pos, Blocks.OAK_LOG.defaultBlockState(), 3);
                    c.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    c.player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.OAK_PLANKS));
                    c.player.input.keyPresses = new Input(false, false, false, false, false, true, false);
                    check(ClassicCrosshairResolver.semanticState(c).secondary().action() == ActionKind.SPECIAL,
                            "real block/item tags, properties, player flags and offhand");
                    c.player.input.keyPresses = Input.EMPTY;
                    check(ClassicCrosshairResolver.semanticState(c).secondary().action() != ActionKind.SPECIAL, "player condition updates without reload");
                    c.hitResult = new EntityHitResult(EntityTypes.ZOMBIE.create(c.level, EntitySpawnReason.COMMAND));
                    check(ClassicCrosshairResolver.semanticState(c).primary().action() == ActionKind.SPECIAL, "real entity registry identity");
                });
                write(high, RULE, "{broken");
                reload(context);
                context.runOnClient(c -> { prepareStone(c); check(ClassicCrosshairResolver.semanticState(c).secondary().action() == ActionKind.NONE,
                        "invalid higher resource does not silently revive shadowed lower rule"); });
                Files.delete(high.resolve(RULE));
                reload(context);
                context.runOnClient(c -> { prepareStone(c); check(ClassicCrosshairResolver.semanticState(c).secondary().action() == ActionKind.TRANSFORM,
                        "removing high resource exposes lower pack"); });
                write(low, RULE, "{broken");
                reload(context);
                context.runOnClient(c -> { prepareStone(c); check(ClassicCrosshairResolver.semanticState(c).secondary().action() == ActionKind.NONE,
                        "invalid replacement removes previous generation rule"); });
                check(RuleManager.status().rejected() == 2, "malformed file counted and isolated");
                context.runOnClient(c -> c.getResourcePackRepository().setSelected(original));
                reload(context);
                check(RuleManager.current().size() == 0, "pack removal clears all rules");
                context.runOnClient(c -> { prepareStone(c); check(ClassicCrosshairResolver.semanticState(c).secondary().action() == ActionKind.NONE,
                        "normal pipeline restored after pack removal"); });
            }
            // Leave the temporary fixture directories for test evidence; they are never selected after this test.
            org.slf4j.LoggerFactory.getLogger("rule-reload-test").info("PASS: {} real rule resource/reload checks", checks);
        } catch (Exception ex) { throw new AssertionError("Rule reload integration failed", ex); }
        finally { context.runOnClient(c -> c.getResourcePackRepository().setSelected(original)); }
    }
    private static void prepareStone(Minecraft c) {
        c.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
        c.player.input.keyPresses = Input.EMPTY;
        c.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        c.player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        var pos = c.player.blockPosition().offset(2, 0, 0);
        c.level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
        c.hitResult = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
    }
    private static void reload(ClientGameTestContext context) {
        long generation = RuleManager.generation();
        var future = context.computeOnClient(Minecraft::reloadResourcePacks);
        context.waitFor(c -> future.isDone(), 1200);
        future.join();
        check(RuleManager.generation() > generation, "reload listener applied a fresh generation");
    }
    private static String rule(String action) {
        return "{\"schema\":1,\"mode\":\"override\",\"when\":{\"target\":{\"type\":\"block\",\"ids\":[\"minecraft:stone\"]},"
                + "\"held\":{\"ids\":[\"minecraft:stick\"]}},\"emit\":[{\"slot\":\"secondary\",\"action\":\"" + action + "\"}]}";
    }
    private static void metadata(Path directory) throws Exception {
        var version = SharedConstants.getCurrentVersion().packVersion(PackType.CLIENT_RESOURCES);
        var metadata = new PackMetadataSection(Component.literal("Stage 4 test fixture"), new InclusiveRange<>(version, version));
        var root = new JsonObject();
        root.add(PackMetadataSection.CLIENT_TYPE.name(), PackMetadataSection.CLIENT_TYPE.codec().encodeStart(JsonOps.INSTANCE, metadata).getOrThrow());
        write(directory, "pack.mcmeta", root.toString());
    }
    private static void write(Path directory, String relative, String text) throws Exception {
        var path = directory.resolve(relative); Files.createDirectories(path.getParent()); Files.writeString(path, text);
    }
    private static void check(boolean value, String label) { if (!value) throw new AssertionError(label); checks++; }
}
