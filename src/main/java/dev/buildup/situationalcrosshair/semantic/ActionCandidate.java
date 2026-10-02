package dev.buildup.situationalcrosshair.semantic;

import java.util.Objects;

public record ActionCandidate(ActionSlot slot, ActionKind action, ActionState state,
        CandidateSource source, Specificity specificity, Confidence confidence,
        int priority, String origin) {
    public static final int MIN_PRIORITY = -100;
    public static final int MAX_PRIORITY = 100;

    public ActionCandidate {
        Objects.requireNonNull(slot);
        Objects.requireNonNull(action);
        Objects.requireNonNull(state);
        Objects.requireNonNull(source);
        Objects.requireNonNull(specificity);
        Objects.requireNonNull(confidence);
        Objects.requireNonNull(origin);
        if (!action.supports(slot)) throw new IllegalArgumentException("Illegal slot/action: " + slot + "/" + action);
        if (action == ActionKind.NONE && state != ActionState.NORMAL)
            throw new IllegalArgumentException("NONE must have NORMAL state");
        if (priority < MIN_PRIORITY || priority > MAX_PRIORITY) throw new IllegalArgumentException("Priority outside [-100,100]");
        if (!origin.matches("[a-z0-9_.-]+:[a-z0-9/._-]+")) throw new IllegalArgumentException("Invalid origin identifier: " + origin);
    }
}
