package dev.buildup.situationalcrosshair.crosshair;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;

/** Read-only Classic policy. Null delegates the central sprite to Vanilla. */
public final class ClassicCrosshairResolver {
    private ClassicCrosshairResolver() { }

    public static ClassicCrosshairType resolve(Minecraft client) {
        if (client == null || client.player == null || client.level == null || client.hitResult == null)
            return null;
        if (client.gui.hud.isHidden() || !client.options.getCameraType().isFirstPerson()) return null;
        var player = client.player;
        var offHand = player.getOffhandItem();
        // Classic gives ranged off-hand items precedence, even when uncharged.
        var hand = offHand.is(Items.BOW) || offHand.is(Items.CROSSBOW)
                ? offHand : player.getMainHandItem();
        if (hand.is(Items.CROSSBOW) && CrossbowItem.isCharged(hand)) return ClassicCrosshairType.ATTACK;
        return switch (client.hitResult.getType()) {
            case MISS -> ClassicCrosshairType.DOT;
            case ENTITY -> ClassicCrosshairType.ATTACK;
            case BLOCK -> {
                if (player.isSpectator() || !(client.hitResult instanceof BlockHitResult block)) yield null;
                var pos = block.getBlockPos();
                var state = client.level.getBlockState(pos);
                boolean valid = player.isCreative() || state.getDestroySpeed(client.level, pos) >= 0
                        && player.hasCorrectToolForDrops(state);
                yield valid ? ClassicCrosshairType.BLOCK : ClassicCrosshairType.ERROR;
            }
        };
    }
}
