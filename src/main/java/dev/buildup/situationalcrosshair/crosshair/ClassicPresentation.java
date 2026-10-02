package dev.buildup.situationalcrosshair.crosshair;

import dev.buildup.situationalcrosshair.semantic.*;

/** Pure Classic theme policy; texture selection remains in the renderer. */
public final class ClassicPresentation {
    private ClassicPresentation() { }

    public static ClassicCrosshairType map(CrosshairSemanticState state) {
        if (state.visibility() != Visibility.SHOW) return null;
        // Existing charged-crossbow appearance, without calling it a melee ATTACK action.
        var classicHand = state.offHand().rangedItem() != CrosshairContextSnapshot.RangedItem.NONE
                ? state.offHand() : state.mainHand();
        if (classicHand.charged())
            return ClassicCrosshairType.ATTACK;
        return switch (state.target()) {
            case MISS -> ClassicCrosshairType.DOT;
            case ENTITY -> state.primary().action() == ActionKind.ATTACK ? ClassicCrosshairType.ATTACK : null;
            case BLOCK -> {
                if (state.primary().action() != ActionKind.MINE) yield null;
                yield switch (state.primary().state()) {
                    case NORMAL -> ClassicCrosshairType.BLOCK;
                    case INVALID -> ClassicCrosshairType.ERROR;
                    default -> null;
                };
            }
        };
    }
}
