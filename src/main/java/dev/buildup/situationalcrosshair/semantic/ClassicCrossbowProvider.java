package dev.buildup.situationalcrosshair.semantic;

import static dev.buildup.situationalcrosshair.semantic.CrosshairContextSnapshot.RangedItem;

/** Only the Stage 1 loaded-crossbow exception; full ranged capability is Stage 3. */
public final class ClassicCrossbowProvider implements CandidateProvider {
    @Override
    public void collect(CrosshairContextSnapshot context, CandidateCollector collector) {
        if (context.nativeCaptured()) return; // Real runtime input precedence is handled by VanillaUseProvider.
        // Preserve existing Classic offhand selection, including an offhand bow.
        var hand = context.offHand().rangedItem() != RangedItem.NONE ? context.offHand() : context.mainHand();
        if (!hand.charged()) return;
        collector.add(new ActionCandidate(ActionSlot.SECONDARY, ActionKind.USE, ActionState.READY,
                CandidateSource.VANILLA_COMPONENT, Specificity.EXACT_ITEM, Confidence.EXACT, 0,
                "buildup_situational_crosshair:classic_charged_crossbow"));
    }
}
