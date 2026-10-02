package dev.buildup.situationalcrosshair;

import dev.buildup.situationalcrosshair.crosshair.ClassicCrosshairResolver;
import dev.buildup.situationalcrosshair.crosshair.ClassicCrosshairType;
import dev.buildup.situationalcrosshair.semantic.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

public final class VanillaCapabilitiesTest implements FabricClientGameTest {
    private static int checks;

    @Override
    public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksDownload();
            context.waitFor(c -> c.gui.screen() == null);
            world.getServer().runCommand("gamemode survival @a");
            context.waitFor(c -> c.player.gameMode() == GameType.SURVIVAL);
            context.runOnClient(c -> {
                c.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                c.player.input.keyPresses = net.minecraft.world.entity.player.Input.EMPTY;
                hold(c, ItemStack.EMPTY, ItemStack.EMPTY);
                air(c);
                expect(c, ActionKind.NONE, ActionState.NORMAL, "air");
                var pos = c.player.blockPosition().offset(2, 0, 0);
                block(c, pos, Blocks.STONE.defaultBlockState());
                expect(c, ActionKind.NONE, ActionState.NORMAL, "stone empty hand");
                hold(c, new ItemStack(Items.COBBLESTONE), ItemStack.EMPTY);
                expect(c, ActionKind.PLACE, ActionState.NORMAL, "block placement");
                hold(c, new ItemStack(Items.TORCH), ItemStack.EMPTY);
                expect(c, ActionKind.PLACE, ActionState.NORMAL, "torch placement");
                hold(c, new ItemStack(Items.COBBLESTONE), ItemStack.EMPTY);
                c.level.setBlock(pos.above(), Blocks.STONE.defaultBlockState(), 3);
                expect(c, ActionKind.PLACE, ActionState.BLOCKED, "occupied placement destination");
                block(c, pos, Blocks.CHEST.defaultBlockState());
                hold(c, new ItemStack(Items.COBBLESTONE), ItemStack.EMPTY);
                expect(c, ActionKind.INTERACT, ActionState.NORMAL, "chest with block");
                c.player.getCooldowns().addCooldown(c.player.getMainHandItem(), 20);
                expect(c, ActionKind.INTERACT, ActionState.NORMAL, "chest interaction precedes held item cooldown");
                c.player.getCooldowns().removeCooldown(c.player.getCooldowns().getCooldownGroup(c.player.getMainHandItem()));
                c.player.input.keyPresses = new net.minecraft.world.entity.player.Input(false, false, false, false, false, true, false);
                expect(c, ActionKind.PLACE, ActionState.NORMAL, "sneak place on chest");
                c.player.input.keyPresses = net.minecraft.world.entity.player.Input.EMPTY;
                hold(c, ItemStack.EMPTY, ItemStack.EMPTY);
                block(c, pos, Blocks.OAK_DOOR.defaultBlockState());
                expect(c, ActionKind.INTERACT, ActionState.NORMAL, "wooden door");
                block(c, pos, Blocks.IRON_DOOR.defaultBlockState());
                expect(c, ActionKind.NONE, ActionState.NORMAL, "iron door no hand action");
                block(c, pos, Blocks.STONE_BUTTON.defaultBlockState());
                expect(c, ActionKind.INTERACT, ActionState.NORMAL, "button");

                block(c, pos, Blocks.OAK_LOG.defaultBlockState());
                hold(c, new ItemStack(Items.IRON_AXE), ItemStack.EMPTY);
                expect(c, ActionKind.TRANSFORM, ActionState.NORMAL, "axe log transform");
                c.player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
                expect(c, ActionKind.USE, ActionState.NORMAL, "shield intent suppresses main hand transformation");
                c.player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                var before = c.level.getBlockState(pos);
                int damage = c.player.getMainHandItem().getDamageValue();
                ClassicCrosshairResolver.semanticState(c);
                check(c.level.getBlockState(pos).equals(before) && damage == c.player.getMainHandItem().getDamageValue(),
                        "probing does not transform or damage item");
                // Same native component on an unrelated generic item: no axe-ID/class inference.
                var componentItem = new ItemStack(Items.STICK);
                componentItem.set(DataComponents.BLOCK_TRANSFORMER, c.player.getMainHandItem().get(DataComponents.BLOCK_TRANSFORMER));
                hold(c, componentItem, ItemStack.EMPTY);
                expect(c, ActionKind.TRANSFORM, ActionState.NORMAL, "component on stick");
                block(c, pos, Blocks.GRASS_BLOCK.defaultBlockState());
                hold(c, new ItemStack(Items.IRON_SHOVEL), ItemStack.EMPTY);
                expect(c, ActionKind.TRANSFORM, ActionState.NORMAL, "shovel transform");
                hold(c, new ItemStack(Items.IRON_HOE), ItemStack.EMPTY);
                expect(c, ActionKind.TRANSFORM, ActionState.NORMAL, "hoe transform");
                c.hitResult = new BlockHitResult(Vec3.atCenterOf(pos), Direction.DOWN, pos, false);
                expect(c, ActionKind.NONE, ActionState.NORMAL, "hoe disallowed face");
                block(c, pos, Blocks.GRASS_BLOCK.defaultBlockState());
                c.level.setBlock(pos.above(), Blocks.STONE.defaultBlockState(), 3);
                expect(c, ActionKind.NONE, ActionState.NORMAL, "hoe blocked above");

                c.level.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState(), 3);
                block(c, pos, Blocks.WHEAT.defaultBlockState());
                hold(c, new ItemStack(Items.BONE_MEAL), ItemStack.EMPTY);
                expect(c, ActionKind.USE, ActionState.NORMAL, "bonemeal crop");
                block(c, pos, Blocks.WHEAT.defaultBlockState().setValue(net.minecraft.world.level.block.CropBlock.AGE, 7));
                expect(c, ActionKind.NONE, ActionState.NORMAL, "mature crop");

                var villager = EntityTypes.VILLAGER.create(c.level, EntitySpawnReason.COMMAND);
                c.hitResult = new EntityHitResult(villager);
                hold(c, ItemStack.EMPTY, ItemStack.EMPTY);
                expect(c, ActionKind.INTERACT, ActionState.NORMAL, "villager");
                check(ClassicCrosshairResolver.semanticState(c).primary().action() == ActionKind.ATTACK, "villager primary preserved");
                c.hitResult = new EntityHitResult(EntityTypes.OAK_BOAT.create(c.level, EntitySpawnReason.COMMAND));
                expect(c, ActionKind.INTERACT, ActionState.NORMAL, "boat mounting");
                var sheep = EntityTypes.SHEEP.create(c.level, EntitySpawnReason.COMMAND);
                c.hitResult = new EntityHitResult(sheep);
                hold(c, new ItemStack(Items.SHEARS), ItemStack.EMPTY);
                expect(c, ActionKind.USE, ActionState.NORMAL, "shearing");
                var zombie = EntityTypes.ZOMBIE.create(c.level, EntitySpawnReason.COMMAND);
                c.hitResult = new EntityHitResult(zombie);
                hold(c, ItemStack.EMPTY, ItemStack.EMPTY);
                expect(c, ActionKind.NONE, ActionState.NORMAL, "hostile target");
                c.player.resetAttackStrengthTicker();
                check(ClassicCrosshairResolver.semanticState(c).primary().state() == ActionState.COOLDOWN,
                        "attack cooldown is not INVALID");

                air(c);
                hold(c, new ItemStack(Items.CROSSBOW), new ItemStack(Items.ARROW));
                c.player.startUsingItem(InteractionHand.MAIN_HAND);
                expect(c, ActionKind.USE, ActionState.CHARGING, "crossbow charging");
                c.player.stopUsingItem();
                var crossbow = new ItemStack(Items.CROSSBOW);
                crossbow.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.of(new ItemStackTemplate(Items.ARROW)));
                hold(c, crossbow, ItemStack.EMPTY);
                expect(c, ActionKind.USE, ActionState.READY, "loaded crossbow");
                c.player.getCooldowns().addCooldown(crossbow, 20);
                expect(c, ActionKind.USE, ActionState.COOLDOWN, "crossbow cooldown");
                hold(c, crossbow, new ItemStack(Items.SPYGLASS));
                expect(c, ActionKind.USE, ActionState.NORMAL, "cooldown allows offhand");
                check(ClassicCrosshairResolver.semanticState(c).secondary().evidence().orElseThrow().origin().endsWith("/off"),
                        "offhand provenance");
                c.player.getCooldowns().removeCooldown(c.player.getCooldowns().getCooldownGroup(crossbow));
                hold(c, new ItemStack(Items.BOW), new ItemStack(Items.ARROW));
                expect(c, ActionKind.USE, ActionState.NORMAL, "bow with ammo");
                c.player.startUsingItem(InteractionHand.MAIN_HAND);
                expect(c, ActionKind.USE, ActionState.CHARGING, "bow charging");
                check(ClassicCrosshairResolver.resolve(c) == ClassicCrosshairType.DOT, "charging preserves Classic DOT");
                c.player.stopUsingItem();
                hold(c, new ItemStack(Items.BOW), crossbow);
                expect(c, ActionKind.USE, ActionState.READY, "main bow no ammo reaches offhand crossbow");

                // Bucket uses its own fluid-aware raycast, independent of the HUD hit result.
                hold(c, new ItemStack(Items.BUCKET), ItemStack.EMPTY);
                c.player.setXRot(90);
                c.player.setYRot(0);
                var below = c.player.blockPosition().below();
                c.level.setBlock(c.player.blockPosition(), Blocks.AIR.defaultBlockState(), 3);
                c.level.setBlock(below, Blocks.WATER.defaultBlockState(), 3);
                air(c);
                expect(c, ActionKind.USE, ActionState.NORMAL, "bucket source pickup");
                hold(c, new ItemStack(Items.WATER_BUCKET), ItemStack.EMPTY);
                c.level.setBlock(below, Blocks.STONE.defaultBlockState(), 3);
                expect(c, ActionKind.USE, ActionState.NORMAL, "bucket emptying");
                c.player.setXRot(0);
                air(c);
                hold(c, new ItemStack(Items.APPLE), ItemStack.EMPTY);
                c.player.getFoodData().setFoodLevel(10);
                expect(c, ActionKind.USE, ActionState.NORMAL, "consumable component hungry");
                c.player.getFoodData().setFoodLevel(20);
                expect(c, ActionKind.NONE, ActionState.NORMAL, "consumable component full");
            });
            world.getServer().runCommand("item replace entity @a weapon.mainhand with minecraft:bow");
            world.getServer().runCommand("item replace entity @a weapon.offhand with minecraft:arrow");
            context.waitFor(c -> c.player.getMainHandItem().is(Items.BOW) && c.player.getOffhandItem().is(Items.ARROW));
            context.runOnClient(c -> {
                c.options.keyUse.setDown(true);
                c.gameMode.useItem(c.player, InteractionHand.MAIN_HAND);
            });
            context.waitFor(c -> c.player.isUsingItem() && c.player.getTicksUsingItem() >= 20);
            context.runOnClient(c -> {
                air(c);
                expect(c, ActionKind.USE, ActionState.READY, "bow fully drawn over real ticks");
                check(ClassicCrosshairResolver.resolve(c) == ClassicCrosshairType.DOT, "ready bow remains Classic DOT");
                c.options.keyUse.setDown(false);
                c.gameMode.releaseUsingItem(c.player);
            });
            world.getServer().runCommand("gamemode spectator @a");
            context.waitFor(c -> c.player.isSpectator());
            context.runOnClient(c -> {
                block(c, c.player.blockPosition().offset(2, 0, 0), Blocks.CHEST.defaultBlockState());
                expect(c, ActionKind.NONE, ActionState.NORMAL, "spectator gameplay use suppressed");
                check(ClassicCrosshairResolver.semanticState(c).primary().state() == ActionState.BLOCKED,
                        "spectator cannot mine");
            });
            context.takeScreenshot("stage-3-smoke");
            org.slf4j.LoggerFactory.getLogger("vanilla-capabilities-test").info("PASS: {} vanilla capability checks", checks);
        }
    }

    private static void hold(Minecraft c, ItemStack main, ItemStack off) {
        c.player.setItemInHand(InteractionHand.MAIN_HAND, main);
        c.player.setItemInHand(InteractionHand.OFF_HAND, off);
    }
    private static void air(Minecraft c) { c.hitResult = BlockHitResult.miss(Vec3.ZERO, Direction.UP, BlockPos.ZERO); }
    private static void block(Minecraft c, BlockPos pos, BlockState state) {
        c.level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
        c.level.setBlock(pos, state, 3);
        c.hitResult = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
    }
    private static void expect(Minecraft c, ActionKind action, ActionState state, String label) {
        var result = ClassicCrosshairResolver.semanticState(c);
        check(result.secondary().action() == action && result.secondary().state() == state,
                label + ": expected " + action + "/" + state + " got " + result.secondary() + " probes="
                        + dev.buildup.situationalcrosshair.crosshair.ContextCapture.capture(c).useAttempts());
    }
    private static void check(boolean value, String label) {
        if (!value) throw new AssertionError(label);
        checks++;
    }
}
