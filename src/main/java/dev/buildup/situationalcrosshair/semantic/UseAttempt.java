package dev.buildup.situationalcrosshair.semantic;

import java.util.Objects;
import dev.buildup.situationalcrosshair.semantic.CrosshairContextSnapshot.Hand;

/** A read-only probe result in vanilla execution order, not an executed interaction. */
public record UseAttempt(Disposition disposition, ActionKind action, ActionState state,
        Hand hand, CandidateSource source, Confidence confidence, String origin) {
    public enum Disposition { PASS, ACTION, UNKNOWN }
    public UseAttempt {
        Objects.requireNonNull(disposition);
        Objects.requireNonNull(action);
        Objects.requireNonNull(state);
        Objects.requireNonNull(hand);
        Objects.requireNonNull(source);
        Objects.requireNonNull(confidence);
        Objects.requireNonNull(origin);
        if (!action.supports(ActionSlot.SECONDARY)) throw new IllegalArgumentException("Invalid use action");
    }
}
