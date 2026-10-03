package dev.buildup.situationalcrosshair.presentation;

import dev.buildup.situationalcrosshair.crosshair.ClassicCrosshairType;
import dev.buildup.situationalcrosshair.crosshair.ClassicPresentation;
import dev.buildup.situationalcrosshair.semantic.*;
import static dev.buildup.situationalcrosshair.presentation.CrosshairPresentation.ActionSidecar;
import static dev.buildup.situationalcrosshair.presentation.CrosshairPresentation.StateSidecar;

/** Presentation policy sees resolved actions, never re-ranks the candidate pool. */
public final class PresentationResolver {
    private PresentationResolver() { }
    public static CrosshairPresentation resolve(CrosshairSemanticState state, CrosshairTheme theme) {
        java.util.Objects.requireNonNull(theme);
        if (state.visibility() != Visibility.SHOW) return CrosshairPresentation.unavailable(state.visibility());
        if (theme == CrosshairTheme.CLASSIC) {
            var base = ClassicPresentation.map(state);
            return base == null ? CrosshairPresentation.unavailable(Visibility.VANILLA)
                    : CrosshairPresentation.baseOnly(base);
        }
        var primary = state.primary();
        var secondary = state.secondary();
        // Semantic information need not occupy the center of the screen.
        var base = switch (state.target()) {
            case MISS -> ClassicCrosshairType.DOT;
            case ENTITY -> primary.action() == ActionKind.ATTACK ? ClassicCrosshairType.ATTACK : ClassicCrosshairType.DOT;
            case BLOCK -> primary.action() == ActionKind.MINE
                    ? primary.state() == ActionState.INVALID ? ClassicCrosshairType.ERROR : ClassicCrosshairType.BLOCK
                    : ClassicCrosshairType.DOT;
        };
        // Only the effective native crossbow's provenance qualifies. An unrelated
        // charged offhand or a pack rule's generic READY must not become ATTACK.
        if (loadedCrossbowReady(state)) return CrosshairPresentation.baseOnly(ClassicCrosshairType.ATTACK);
        // Air, uncertain primary meaning and invalid mining keep a clean Classic
        // base. This also prevents a full sidecar overwhelming the single DOT.
        if (base == ClassicCrosshairType.DOT || base == ClassicCrosshairType.ERROR)
            return CrosshairPresentation.baseOnly(base);
        boolean blocked = secondary.state() == ActionState.BLOCKED
                && secondary.evidence().map(e -> e.confidence() == Confidence.AUTHORITATIVE
                    || e.confidence() == Confidence.EXACT || e.confidence() == Confidence.STRONG).orElse(false);
        if (secondary.state() != ActionState.NORMAL && !blocked) return CrosshairPresentation.baseOnly(base);
        var right = switch (secondary.action()) {
            case INTERACT -> ActionSidecar.INTERACT;
            case USE -> state.target() != TargetType.MISS && !selfUse(secondary) ? ActionSidecar.USE : ActionSidecar.NONE;
            case PLACE -> ActionSidecar.PLACE;
            case TRANSFORM -> ActionSidecar.TRANSFORM;
            default -> ActionSidecar.NONE;
        };
        var left = blocked && right != ActionSidecar.NONE ? StateSidecar.BLOCKED : StateSidecar.NONE;
        return new CrosshairPresentation(Visibility.SHOW, base, left, right);
    }

    private static boolean loadedCrossbowReady(CrosshairSemanticState state) {
        var secondary = state.secondary();
        if (secondary.action() != ActionKind.USE || secondary.state() != ActionState.READY) return false;
        return secondary.evidence().filter(e -> e.source() == CandidateSource.VANILLA_RUNTIME).map(e -> {
            var hand = switch (e.origin()) {
                case "buildup_situational_crosshair:crossbow/main" -> state.mainHand();
                case "buildup_situational_crosshair:crossbow/off" -> state.offHand();
                default -> CrosshairContextSnapshot.HandState.EMPTY;
            };
            return hand.rangedItem() == CrosshairContextSnapshot.RangedItem.CROSSBOW && hand.charged();
        }).orElse(false);
    }

    private static boolean selfUse(CrosshairSemanticState.ResolvedAction action) {
        // These existing native origins describe item-global feedback, even when
        // the ray hits a block/entity. Pack actions retain their own target meaning.
        return action.evidence().filter(e -> e.source() == CandidateSource.VANILLA_RUNTIME
                || e.source() == CandidateSource.VANILLA_COMPONENT).map(e -> switch (e.origin()) {
            case "buildup_situational_crosshair:consumable/main", "buildup_situational_crosshair:consumable/off",
                    "buildup_situational_crosshair:blocking_item/main", "buildup_situational_crosshair:blocking_item/off",
                    "buildup_situational_crosshair:spyglass/main", "buildup_situational_crosshair:spyglass/off",
                    "buildup_situational_crosshair:bow/main", "buildup_situational_crosshair:bow/off",
                    "buildup_situational_crosshair:crossbow/main", "buildup_situational_crosshair:crossbow/off" -> true;
            default -> false;
        }).orElse(false);
    }
}
