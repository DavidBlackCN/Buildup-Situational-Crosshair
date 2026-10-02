package dev.buildup.situationalcrosshair.semantic;

import java.util.Objects;

/** Value-only facts, captured once; no live mutable Minecraft references. */
public record CrosshairContextSnapshot(TargetType target, Visibility visibility, boolean creative,
        Capability breakability, Capability harvestability, HandState mainHand, HandState offHand,
        java.util.List<UseAttempt> useAttempts, boolean attackCooling, boolean spectator, boolean nativeCaptured) {
    public CrosshairContextSnapshot(TargetType target, Visibility visibility, boolean creative,
            Capability breakability, Capability harvestability, HandState mainHand, HandState offHand) {
        this(target, visibility, creative, breakability, harvestability, mainHand, offHand,
                java.util.List.of(), false, false, false);
    }
    public enum Capability { UNKNOWN, YES, NO }
    public enum RangedItem { NONE, BOW, CROSSBOW }
    public enum Hand { MAIN, OFF, NONE }

    public record HandState(RangedItem rangedItem, boolean charged, boolean using) {
        public static final HandState EMPTY = new HandState(RangedItem.NONE, false, false);
        public HandState {
            Objects.requireNonNull(rangedItem);
            if (charged && rangedItem != RangedItem.CROSSBOW)
                throw new IllegalArgumentException("Only crossbows store charged projectiles");
        }
    }

    public CrosshairContextSnapshot {
        Objects.requireNonNull(target);
        Objects.requireNonNull(visibility);
        Objects.requireNonNull(breakability);
        Objects.requireNonNull(harvestability);
        Objects.requireNonNull(mainHand);
        Objects.requireNonNull(offHand);
        useAttempts = java.util.List.copyOf(useAttempts);
    }

    public static CrosshairContextSnapshot unavailable(Visibility visibility) {
        return new CrosshairContextSnapshot(TargetType.MISS, visibility, false, Capability.UNKNOWN,
                Capability.UNKNOWN, HandState.EMPTY, HandState.EMPTY);
    }
}
