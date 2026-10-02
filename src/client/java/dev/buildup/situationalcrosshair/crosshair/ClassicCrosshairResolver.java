package dev.buildup.situationalcrosshair.crosshair;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;

/** Local 1.20.1 behavior, evaluated against the current rendered target. */
public final class ClassicCrosshairResolver {
    private ClassicCrosshairResolver() { }

    /** Null means leave the vanilla crosshair untouched. */
    public static ClassicCrosshairType resolve(Minecraft client) {
        var player = client.player;
        var target = client.hitResult;
        if (player == null || client.level == null || target == null) {
            return null;
        }

        // Preserve the original offhand precedence, including an offhand bow
        // preventing a charged main-hand crossbow from overriding the target.
        ItemStack ranged = player.getMainHandItem();
        if (isClassicRanged(player.getOffhandItem())) {
            ranged = player.getOffhandItem();
        }
        if (ranged.is(Items.CROSSBOW) && CrossbowItem.isCharged(ranged)) {
            return ClassicCrosshairType.ATTACK;
        }

        return switch (target.getType()) {
            case MISS -> ClassicCrosshairType.DOT;
            case ENTITY -> ClassicCrosshairType.ATTACK;
            case BLOCK -> {
                var pos = ((BlockHitResult) target).getBlockPos();
                var state = client.level.getBlockState(pos);
                boolean harvestable = player.isCreative()
                        || (state.getDestroySpeed(client.level, pos) >= 0
                        && player.hasCorrectToolForDrops(state));
                yield harvestable ? ClassicCrosshairType.BLOCK : ClassicCrosshairType.ERROR;
            }
        };
    }

    private static boolean isClassicRanged(ItemStack stack) {
        return stack.is(Items.BOW) || stack.is(Items.CROSSBOW);
    }
}
