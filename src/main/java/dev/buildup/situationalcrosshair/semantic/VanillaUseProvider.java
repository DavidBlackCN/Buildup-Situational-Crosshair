package dev.buildup.situationalcrosshair.semantic;

/** The first consuming/blocked action wins; an unknown earlier stage prevents guessing later ones. */
public final class VanillaUseProvider implements CandidateProvider {
    @Override
    public void collect(CrosshairContextSnapshot context, CandidateCollector collector) {
        if (!context.nativeCaptured() || context.spectator()) return;
        UseAttempt cooling = null;
        for (var attempt : context.useAttempts()) {
            if (attempt.disposition() == UseAttempt.Disposition.PASS) continue;
            if (attempt.disposition() == UseAttempt.Disposition.UNKNOWN) return;
            if (attempt.state() == ActionState.COOLDOWN) {
                if (cooling == null) cooling = attempt;
                continue; // Vanilla skips this item; the other hand may still succeed.
            }
            emit(attempt, collector);
            return;
        }
        if (cooling != null) emit(cooling, collector);
    }

    private static void emit(UseAttempt attempt, CandidateCollector collector) {
        collector.add(new ActionCandidate(ActionSlot.SECONDARY, attempt.action(), attempt.state(),
                attempt.source(), Specificity.EXACT_CONTEXT, attempt.confidence(), 0,
                attempt.origin() + "/" + attempt.hand().name().toLowerCase(java.util.Locale.ROOT)));
    }
}
