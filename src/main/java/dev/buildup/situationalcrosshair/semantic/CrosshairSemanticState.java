package dev.buildup.situationalcrosshair.semantic;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record CrosshairSemanticState(TargetType target, Visibility visibility,
        ResolvedAction primary, ResolvedAction secondary, List<ActionCandidate> candidates) {
    public record ResolvedAction(ActionKind action, ActionState state, Optional<ActionCandidate> evidence) {
        public ResolvedAction {
            Objects.requireNonNull(action);
            Objects.requireNonNull(state);
            Objects.requireNonNull(evidence);
            if (action == ActionKind.NONE && state != ActionState.NORMAL)
                throw new IllegalArgumentException("NONE cannot carry an active state");
            if (evidence.isPresent() && (evidence.get().action() != action || evidence.get().state() != state))
                throw new IllegalArgumentException("Evidence must describe the resolved action");
        }
        public static ResolvedAction none() {
            return new ResolvedAction(ActionKind.NONE, ActionState.NORMAL, Optional.empty());
        }
        public static ResolvedAction of(ActionCandidate candidate) {
            return new ResolvedAction(candidate.action(), candidate.state(), Optional.of(candidate));
        }
    }

    public CrosshairSemanticState {
        Objects.requireNonNull(target);
        Objects.requireNonNull(visibility);
        Objects.requireNonNull(primary);
        Objects.requireNonNull(secondary);
        candidates = List.copyOf(candidates);
        validate(primary, ActionSlot.PRIMARY, candidates);
        validate(secondary, ActionSlot.SECONDARY, candidates);
    }

    private static void validate(ResolvedAction action, ActionSlot slot, List<ActionCandidate> candidates) {
        if (!action.action().supports(slot)) throw new IllegalArgumentException("Illegal resolved slot/action");
        action.evidence().ifPresent(candidate -> {
            if (candidate.slot() != slot || !candidates.contains(candidate))
                throw new IllegalArgumentException("Evidence must belong to this slot and candidate pool");
        });
    }
}
