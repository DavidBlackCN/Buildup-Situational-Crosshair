package dev.buildup.situationalcrosshair.crosshair;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import dev.buildup.situationalcrosshair.semantic.CrosshairContextSnapshot;
import dev.buildup.situationalcrosshair.semantic.TargetType;
import dev.buildup.situationalcrosshair.semantic.Visibility;
import static dev.buildup.situationalcrosshair.semantic.CrosshairContextSnapshot.*;

/** Captures live state through the native adapters, then publishes one value-only snapshot. */
public final class ContextCapture {
    private ContextCapture() { }

    public static CrosshairContextSnapshot capture(Minecraft client) {
        var player = client.player;
        var target = client.hitResult;
        if (player == null || client.level == null || target == null)
            return CrosshairContextSnapshot.unavailable(Visibility.VANILLA);
        if (client.gui.hud.isHidden() || !client.options.getCameraType().isFirstPerson())
            return CrosshairContextSnapshot.unavailable(Visibility.HIDE);
        var kind = switch (target.getType()) {
            case MISS -> TargetType.MISS;
            case BLOCK -> TargetType.BLOCK;
            case ENTITY -> TargetType.ENTITY;
        };
        var breakability = Capability.UNKNOWN;
        var harvestability = Capability.UNKNOWN;
        if (target instanceof BlockHitResult block && kind == TargetType.BLOCK) {
            var pos = block.getBlockPos();
            var state = client.level.getBlockState(pos);
            breakability = player.isCreative() || state.getDestroySpeed(client.level, pos) >= 0
                    ? Capability.YES : Capability.NO;
            harvestability = player.hasCorrectToolForDrops(state) ? Capability.YES : Capability.NO;
        }
        return new CrosshairContextSnapshot(kind, Visibility.SHOW, player.isCreative(), breakability, harvestability,
                hand(player.getMainHandItem(), player.isUsingItem() && player.getUsedItemHand() == InteractionHand.MAIN_HAND),
                hand(player.getOffhandItem(), player.isUsingItem() && player.getUsedItemHand() == InteractionHand.OFF_HAND),
                VanillaCapabilityCapture.capture(client), player.getAttackStrengthScale(0) < 1,
                player.isSpectator(), true);
    }

    private static HandState hand(ItemStack stack, boolean using) {
        var item = stack.is(Items.BOW) ? RangedItem.BOW
                : stack.is(Items.CROSSBOW) ? RangedItem.CROSSBOW : RangedItem.NONE;
        return new HandState(item, item == RangedItem.CROSSBOW && CrossbowItem.isCharged(stack), using);
    }
}
