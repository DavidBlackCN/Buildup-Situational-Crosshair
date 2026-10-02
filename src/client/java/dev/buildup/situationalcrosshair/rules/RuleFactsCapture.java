package dev.buildup.situationalcrosshair.rules;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import java.util.HashMap;
import java.util.stream.Collectors;

/** Capture current synced registry tags, so joining another server never retains old tag matches. */
public final class RuleFactsCapture {
    private RuleFactsCapture() { }
    public static RuleFacts capture(Minecraft c) {
        var target = RuleFacts.Identity.EMPTY;
        var properties = new HashMap<String, String>();
        if (c.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            var state = c.level.getBlockState(hit.getBlockPos());
            target = new RuleFacts.Identity(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(),
                    state.typeHolder().tags().map(t -> t.location().toString()).collect(Collectors.toSet()));
            for (var property : state.getProperties()) properties.put(property.getName(), propertyValue(state, property));
        } else if (c.hitResult instanceof EntityHitResult hit) {
            var type = hit.getEntity().getType();
            target = new RuleFacts.Identity(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString(),
                    BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(type).tags().map(t -> t.location().toString()).collect(Collectors.toSet()));
        }
        return new RuleFacts(target, item(c.player.getMainHandItem()), item(c.player.getOffhandItem()), properties,
                c.player.isSecondaryUseActive(), c.player.isUsingItem());
    }
    private static RuleFacts.Identity item(ItemStack stack) {
        if (stack.isEmpty()) return RuleFacts.Identity.EMPTY;
        return new RuleFacts.Identity(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                stack.typeHolder().tags().map(t -> t.location().toString()).collect(Collectors.toSet()));
    }
    private static <T extends Comparable<T>> String propertyValue(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }
}
