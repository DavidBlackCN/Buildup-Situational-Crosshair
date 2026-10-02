package dev.buildup.situationalcrosshair.crosshair;

import dev.buildup.situationalcrosshair.mixin.BlockItemAccessor;
import dev.buildup.situationalcrosshair.semantic.*;
import dev.buildup.situationalcrosshair.semantic.CrosshairContextSnapshot.Hand;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.stateproviders.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;

/** Read-only native probes. Unknown earlier handlers prevent claiming a later effective action. */
public final class VanillaCapabilityCapture {
    private record InteractionMethods(boolean item, boolean empty) { }
    private static final ClassValue<InteractionMethods> BASE_METHODS = new ClassValue<>() {
        @Override protected InteractionMethods computeValue(Class<?> type) {
            return new InteractionMethods(baseMethod(type, "useItemOn"), baseMethod(type, "useWithoutItem"));
        }
    };
    private static final ClassValue<Boolean> BASE_ITEM_USE_ON = new ClassValue<>() {
        @Override protected Boolean computeValue(Class<?> type) {
            for (var current = type; current != null; current = current.getSuperclass()) {
                for (var method : current.getDeclaredMethods()) {
                    if (method.getName().equals("useOn")) return current == Item.class;
                }
            }
            return false;
        }
    };
    private VanillaCapabilityCapture() { }

    public static List<UseAttempt> capture(Minecraft client) {
        if (client.player.isSpectator()) return List.of();
        if (client.player.isUsingItem()) {
            var hand = client.player.getUsedItemHand();
            return List.of(itemUse(client, hand, client.player.getItemInHand(hand), true));
        }
        var attempts = new ArrayList<UseAttempt>();
        for (var hand : InteractionHand.values()) {
            var stack = client.player.getItemInHand(hand);
            if (client.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
                boolean bypass = client.player.isSecondaryUseActive()
                        && (!client.player.getMainHandItem().isEmpty() || !client.player.getOffhandItem().isEmpty());
                if (!bypass) attempts.add(blockInteraction(client, hand, hit));
                if (!stack.isEmpty()) {
                    if (client.player.getCooldowns().isOnCooldown(stack)) {
                        attempts.add(action(hand, ActionKind.USE, ActionState.COOLDOWN, "item_cooldown"));
                        continue;
                    }
                    attempts.add(itemOnBlock(client, hand, stack, hit));
                }
            } else if (client.hitResult instanceof EntityHitResult hit) {
                attempts.add(entityInteraction(client, hand, stack, hit));
            }
            attempts.add(itemUse(client, hand, stack, false));
        }
        return List.copyOf(attempts);
    }

    private static UseAttempt blockInteraction(Minecraft c, InteractionHand hand, BlockHitResult hit) {
        var state = c.level.getBlockState(hit.getBlockPos());
        var block = state.getBlock();
        // Special target behavior is recognized only for vanilla registrations. An exact
        // inherited base PASS is safe for any namespace; unknown overrides remain unknown.
        if (BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals("minecraft")
                && hand == InteractionHand.MAIN_HAND) {
            if (block instanceof DoorBlock door)
                return door.type().canOpenByHand() ? action(hand, ActionKind.INTERACT, ActionState.NORMAL, "door") : pass(hand);
            if (block instanceof ButtonBlock || block instanceof LeverBlock || block instanceof FenceGateBlock)
                return action(hand, ActionKind.INTERACT, ActionState.NORMAL, "block_interaction");
            if (BASE_METHODS.get(block.getClass()).item() && state.getMenuProvider(c.level, hit.getBlockPos()) != null)
                return action(hand, ActionKind.INTERACT, ActionState.NORMAL, "block_menu");
        }
        // Exact inherited base implementation is known to pass. Custom overrides are unknown.
        if (!BASE_METHODS.get(block.getClass()).item()) return unknown(hand);
        if (hand == InteractionHand.MAIN_HAND && !BASE_METHODS.get(block.getClass()).empty()) return unknown(hand);
        return pass(hand);
    }

    private static boolean baseMethod(Class<?> type, String name) {
        for (var current = type; current != null; current = current.getSuperclass()) {
            for (var method : current.getDeclaredMethods()) {
                if (method.getName().equals(name)) return current == BlockBehaviour.class || current == Block.class;
            }
        }
        return false;
    }

    private static UseAttempt itemOnBlock(Minecraft c, InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        var pos = hit.getBlockPos();
        if (!c.level.mayInteract(c.player, pos) || !c.level.getWorldBorder().isWithinBounds(pos))
            return action(hand, ActionKind.USE, ActionState.BLOCKED, "world_permission");
        var transformer = stack.get(DataComponents.BLOCK_TRANSFORMER);
        if (transformer != null) {
            boolean shieldIntent = hand == InteractionHand.MAIN_HAND
                    && c.player.getOffhandItem().has(DataComponents.BLOCKS_ATTACKS) && !c.player.isSecondaryUseActive();
            if (!shieldIntent) {
                if (!c.player.mayUseItemAt(pos, hit.getDirection(), stack))
                    return action(hand, ActionKind.TRANSFORM, ActionState.BLOCKED, "transform_permission");
                for (var transform : transformer.value().transforms()) {
                    if (transform.disallowedFaces().contains(hit.getDirection())) continue;
                    var provider = transform.blockStateProvider().value();
                    if (!deterministic(provider, 0)) return unknown(hand);
                    // Private random stream: never consume the world's RNG.
                    if (provider.getOptionalState(c.level, RandomSource.create(0), pos) != null)
                        return componentAction(hand, ActionKind.TRANSFORM, ActionState.NORMAL, "block_transformer");
                }
            }
        }
        if (stack.getItem() instanceof BlockItem item) {
            if (!vanilla(stack) && item.getClass() != BlockItem.class) return unknown(hand);
            var context = new BlockPlaceContext(c.player, hand, stack, hit);
            var updated = item.updatePlacementContext(context);
            boolean allowed = updated != null && c.player.mayUseItemAt(updated.getClickedPos(), hit.getDirection(), stack)
                    && c.level.getWorldBorder().isWithinBounds(updated.getClickedPos())
                    && updated.canPlace() && ((BlockItemAccessor) item).buildup$getPlacementState(updated) != null;
            return action(hand, ActionKind.PLACE, allowed ? ActionState.NORMAL : ActionState.BLOCKED, "placement");
        }
        if (stack.is(Items.BONE_MEAL)) {
            var state = c.level.getBlockState(pos);
            if (state.getBlock() instanceof BonemealableBlock growable
                    && growable.isValidBonemealTarget(c.level, pos, state, BonemealSource.INTERACTION))
                return action(hand, ActionKind.USE, ActionState.NORMAL, "bonemeal");
            return pass(hand);
        }
        // The component was checked above; inherited Item.useOn has no further target behavior.
        // This also permits shield/food use after a block interaction passes, without
        // treating arbitrary item subclasses as if they had no target-specific override.
        if (BASE_ITEM_USE_ON.get(stack.getItem().getClass())) return pass(hand);
        return unknown(hand);
    }

    private static boolean deterministic(BlockStateProvider provider, int depth) {
        if (depth > 16) return false;
        if (provider instanceof SimpleStateProvider) return true;
        if (provider instanceof CopyPropertiesProvider copy) return deterministic(copy.source().value(), depth + 1);
        if (provider instanceof RuleBasedStateProvider rules) {
            if (rules.fallback() != null && !deterministic(rules.fallback().value(), depth + 1)) return false;
            return rules.rules().stream().allMatch(rule -> deterministic(rule.then().value(), depth + 1));
        }
        return false;
    }

    private static UseAttempt entityInteraction(Minecraft c, InteractionHand hand, ItemStack stack, EntityHitResult hit) {
        var entity = hit.getEntity();
        if (!BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace().equals("minecraft")) return unknown(hand);
        if (stack.is(Items.SHEARS) && entity instanceof Shearable shearable && shearable.readyForShearing())
            return action(hand, ActionKind.USE, ActionState.NORMAL, "shearing");
        if (entity instanceof Villager villager && villager.isAlive() && !villager.isSleeping()
                && !villager.isTrading() && !c.player.isSecondaryUseActive() && !(stack.getItem() instanceof SpawnEggItem))
            return action(hand, ActionKind.INTERACT, ActionState.NORMAL, "villager");
        if (entity instanceof Boat boat && !c.player.isSecondaryUseActive() && boat.getPassengers().size() < 2
                && !c.player.isPassenger() && !boat.isUnderWater())
            return action(hand, ActionKind.INTERACT, ActionState.NORMAL, "boat");
        // Hostile targets have no generic right-click interaction; naming/tag guesses are not used.
        if (entity instanceof net.minecraft.world.entity.monster.Monster) return pass(hand);
        return unknown(hand);
    }

    private static UseAttempt itemUse(Minecraft c, InteractionHand hand, ItemStack stack, boolean using) {
        if (stack.isEmpty()) return pass(hand);
        if (c.player.getCooldowns().isOnCooldown(stack)) return action(hand, ActionKind.USE, ActionState.COOLDOWN, "item_cooldown");
        if (stack.is(Items.BOW) || stack.is(Items.CROSSBOW)) {
            boolean crossbow = stack.is(Items.CROSSBOW);
            if (crossbow && CrossbowItem.isCharged(stack)) return action(hand, ActionKind.USE, ActionState.READY, "crossbow");
            boolean ammo = !c.player.getProjectile(stack).isEmpty() || c.player.isCreative();
            if (!ammo && !using) return pass(hand);
            var state = using ? (!crossbow && c.player.getTicksUsingItem() >= 20 ? ActionState.READY : ActionState.CHARGING)
                    : ActionState.NORMAL;
            return action(hand, ActionKind.USE, state, crossbow ? "crossbow" : "bow");
        }
        if (stack.is(Items.BUCKET) || stack.is(Items.WATER_BUCKET) || stack.is(Items.LAVA_BUCKET)) return bucket(c, hand, stack);
        var consumable = stack.get(DataComponents.CONSUMABLE);
        if (consumable != null) return consumable.canConsume(c.player, stack)
                ? componentAction(hand, ActionKind.USE, using ? ActionState.CHARGING : ActionState.NORMAL, "consumable") : pass(hand);
        if (stack.has(DataComponents.BLOCKS_ATTACKS)) return componentAction(hand, ActionKind.USE, ActionState.NORMAL, "blocking_item");
        if (stack.is(Items.SPYGLASS)) return action(hand, ActionKind.USE, ActionState.NORMAL, "spyglass");
        if (stack.getItem() instanceof BlockItem || stack.has(DataComponents.BLOCK_TRANSFORMER)
                || stack.getItem().getClass() == Item.class || stack.is(Items.SHEARS)) return pass(hand);
        return unknown(hand);
    }

    private static UseAttempt bucket(Minecraft c, InteractionHand hand, ItemStack stack) {
        boolean empty = stack.is(Items.BUCKET);
        var eye = c.player.getEyePosition();
        var end = eye.add(c.player.getViewVector(1).scale(c.player.blockInteractionRange()));
        var hit = c.level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE,
                empty ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE, c.player));
        if (hit.getType() != HitResult.Type.BLOCK) return pass(hand);
        var pos = hit.getBlockPos();
        var state = c.level.getBlockState(pos);
        if (!c.level.mayInteract(c.player, pos) || !c.player.mayUseItemAt(pos.relative(hit.getDirection()), hit.getDirection(), stack))
            return action(hand, ActionKind.USE, ActionState.BLOCKED, "bucket_permission");
        if (empty) {
            if (state.getBlock() instanceof LiquidBlock && state.getFluidState().isSource())
                return action(hand, ActionKind.USE, ActionState.NORMAL, "bucket_pickup");
            return unknown(hand); // waterlogged and specialized pickup contracts are not executed
        }
        var fluid = stack.is(Items.WATER_BUCKET) ? Fluids.WATER : Fluids.LAVA;
        if (state.getBlock() instanceof LiquidBlockContainer container && container.canPlaceLiquid(c.player, c.level, pos, state, fluid))
            return action(hand, ActionKind.USE, ActionState.NORMAL, "bucket_container");
        var destination = c.level.getBlockState(pos.relative(hit.getDirection()));
        return destination.isAir() || destination.canBeReplaced(fluid)
                ? action(hand, ActionKind.USE, ActionState.NORMAL, "bucket_empty") : pass(hand);
    }

    private static boolean vanilla(ItemStack stack) { return BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("minecraft"); }
    private static Hand semanticHand(InteractionHand hand) { return hand == InteractionHand.MAIN_HAND ? Hand.MAIN : Hand.OFF; }
    private static UseAttempt action(InteractionHand hand, ActionKind kind, ActionState state, String origin) {
        return new UseAttempt(UseAttempt.Disposition.ACTION, kind, state, semanticHand(hand),
                CandidateSource.VANILLA_RUNTIME, Confidence.STRONG, "buildup_situational_crosshair:" + origin);
    }
    private static UseAttempt componentAction(InteractionHand hand, ActionKind kind, ActionState state, String origin) {
        return new UseAttempt(UseAttempt.Disposition.ACTION, kind, state, semanticHand(hand),
                CandidateSource.VANILLA_COMPONENT, Confidence.EXACT, "buildup_situational_crosshair:" + origin);
    }
    private static UseAttempt pass(InteractionHand hand) {
        return new UseAttempt(UseAttempt.Disposition.PASS, ActionKind.NONE, ActionState.NORMAL, semanticHand(hand),
                CandidateSource.VANILLA_RUNTIME, Confidence.EXACT, "buildup_situational_crosshair:pass");
    }
    private static UseAttempt unknown(InteractionHand hand) {
        return new UseAttempt(UseAttempt.Disposition.UNKNOWN, ActionKind.NONE, ActionState.NORMAL, semanticHand(hand),
                CandidateSource.UNKNOWN, Confidence.UNKNOWN, "buildup_situational_crosshair:unknown");
    }
}
