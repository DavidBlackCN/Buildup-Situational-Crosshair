package dev.buildup.situationalcrosshair;

import dev.buildup.situationalcrosshair.crosshair.ClassicCrosshairResolver;
import dev.buildup.situationalcrosshair.crosshair.ClassicCrosshairType;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.LoggerFactory;

/** Runs against real client player, world, item components and harvest rules. */
public final class ClassicParityTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksDownload();
            context.waitFor(client -> client.gui.screen() == null);
            world.getServer().runCommand("gamemode survival @a");
            context.waitFor(client -> client.player.gameMode() == net.minecraft.world.level.GameType.SURVIVAL);
            context.runOnClient(client -> {
                var player = client.player;
                client.gameMode.setLocalMode(net.minecraft.world.level.GameType.SURVIVAL);
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                var miss = BlockHitResult.miss(Vec3.ZERO, Direction.UP, BlockPos.ZERO);
                client.hitResult = miss;
                expect(client, ClassicCrosshairType.DOT, "air");
                client.hitResult = new EntityHitResult(player);
                expect(client, ClassicCrosshairType.ATTACK, "entity");

                var pos = player.blockPosition().offset(2, 0, 0);
                client.hitResult = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
                client.level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
                expect(client, ClassicCrosshairType.BLOCK, "dirt empty hand");
                client.level.setBlock(pos, Blocks.DIAMOND_ORE.defaultBlockState(), 3);
                expect(client, ClassicCrosshairType.ERROR, "ore empty hand");
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WOODEN_PICKAXE));
                expect(client, ClassicCrosshairType.ERROR, "insufficient tier");
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
                expect(client, ClassicCrosshairType.BLOCK, "correct tier");
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SHOVEL));
                expect(client, ClassicCrosshairType.ERROR, "wrong tool");
                client.level.setBlock(pos, Blocks.BEDROCK.defaultBlockState(), 3);
                expect(client, ClassicCrosshairType.ERROR, "survival bedrock");

                client.hitResult = miss;
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOW));
                expect(client, ClassicCrosshairType.DOT, "bow");
                player.startUsingItem(InteractionHand.MAIN_HAND);
                expect(client, ClassicCrosshairType.DOT, "using bow");
                player.stopUsingItem();
                var crossbow = new ItemStack(Items.CROSSBOW);
                player.setItemInHand(InteractionHand.MAIN_HAND, crossbow);
                expect(client, ClassicCrosshairType.DOT, "uncharged crossbow");
                crossbow.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.of(new ItemStackTemplate(Items.ARROW)));
                expect(client, ClassicCrosshairType.ATTACK, "charged crossbow");
                player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.BOW));
                expect(client, ClassicCrosshairType.DOT, "offhand bow precedence");
                player.setItemInHand(InteractionHand.OFF_HAND, crossbow.copy());
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                expect(client, ClassicCrosshairType.ATTACK, "offhand charged crossbow");
                client.hitResult = null;
                expect(client, null, "unknown target fallback");
                client.hitResult = miss;
                player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                HudParityChecks.run(client);
            });
            world.getServer().runCommand("gamemode creative @a");
            context.waitFor(client -> client.player.isCreative());
            context.runOnClient(client -> {
                var pos = client.player.blockPosition().offset(2, 0, 0);
                client.level.setBlock(pos, Blocks.BEDROCK.defaultBlockState(), 3);
                client.hitResult = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
                expect(client, ClassicCrosshairType.BLOCK, "creative bedrock");
            });
            context.takeScreenshot("classic-gameplay-smoke");
            LoggerFactory.getLogger("classic-parity-test").info("PASS: 16 Classic resolver integration cases");
        }
    }

    private static void expect(net.minecraft.client.Minecraft client, ClassicCrosshairType expected, String label) {
        var actual = ClassicCrosshairResolver.resolve(client);
        if (actual != expected) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
        HudParityChecks.check(client, expected == null ? 0 : 1, expected == null ? 1 : 0, label);
    }
}
