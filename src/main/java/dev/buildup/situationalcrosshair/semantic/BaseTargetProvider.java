package dev.buildup.situationalcrosshair.semantic;

/** Conservative defaults for both input channels, for every known target. */
public final class BaseTargetProvider implements CandidateProvider {
    @Override
    public void collect(CrosshairContextSnapshot context, CandidateCollector collector) {
        for (var slot : ActionSlot.values()) {
            collector.add(new ActionCandidate(slot, ActionKind.NONE, ActionState.NORMAL,
                    CandidateSource.VANILLA_RUNTIME, Specificity.GENERIC, Confidence.EXACT, -100,
                    "buildup_situational_crosshair:base/" + slot.name().toLowerCase(java.util.Locale.ROOT)));
        }
    }
}
