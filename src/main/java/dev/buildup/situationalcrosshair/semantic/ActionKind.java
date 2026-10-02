package dev.buildup.situationalcrosshair.semantic;

public enum ActionKind {
    NONE, MINE, ATTACK, INTERACT, USE, PLACE, TRANSFORM, SPECIAL;

    public boolean supports(ActionSlot slot) {
        return switch (this) {
            case NONE, SPECIAL -> true;
            case MINE, ATTACK -> slot == ActionSlot.PRIMARY;
            case INTERACT, USE, PLACE, TRANSFORM -> slot == ActionSlot.SECONDARY;
        };
    }
}
