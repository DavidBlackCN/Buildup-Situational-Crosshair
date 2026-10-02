package dev.buildup.situationalcrosshair.semantic;

import static dev.buildup.situationalcrosshair.semantic.CrosshairContextSnapshot.Capability;

public final class HarvestProvider implements CandidateProvider {
    @Override
    public void collect(CrosshairContextSnapshot context, CandidateCollector collector) {
        if (context.target() != TargetType.BLOCK) return;
        if (!context.creative() && (context.breakability() == Capability.UNKNOWN
                || context.harvestability() == Capability.UNKNOWN)) return;
        boolean valid = context.creative() || context.breakability() == Capability.YES
                && context.harvestability() == Capability.YES;
        collector.add(new ActionCandidate(ActionSlot.PRIMARY, ActionKind.MINE,
                context.spectator() ? ActionState.BLOCKED : valid ? ActionState.NORMAL : ActionState.INVALID, CandidateSource.VANILLA_RUNTIME,
                Specificity.EXACT_CONTEXT, Confidence.EXACT, 0, "buildup_situational_crosshair:harvest"));
    }
}
