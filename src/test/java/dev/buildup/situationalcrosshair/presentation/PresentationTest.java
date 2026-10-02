package dev.buildup.situationalcrosshair.presentation;

import dev.buildup.situationalcrosshair.crosshair.ClassicCrosshairType;
import dev.buildup.situationalcrosshair.crosshair.ClassicPresentation;
import dev.buildup.situationalcrosshair.semantic.*;
import java.util.*;
import static dev.buildup.situationalcrosshair.semantic.CrosshairContextSnapshot.HandState;

/** Mapping and elapsed-time contracts without a game client or render dependency. */
public final class PresentationTest {
    private static int checks;
    public static void main(String[] args) {
        for (var target : TargetType.values()) for (var primaryState : ActionState.values()) {
            var primary = target == TargetType.BLOCK ? ActionKind.MINE : target == TargetType.ENTITY ? ActionKind.ATTACK : ActionKind.NONE;
            if (primary == ActionKind.NONE && primaryState != ActionState.NORMAL) continue;
            var state = semantic(target, primary, primaryState, ActionKind.NONE, ActionState.NORMAL);
            check(PresentationResolver.resolve(state, CrosshairTheme.CLASSIC).base() == ClassicPresentation.map(state), "exact Classic mapping");
            check(PresentationResolver.resolve(state, CrosshairTheme.CLASSIC).modifier() == CrosshairPresentation.SecondaryModifier.NONE,
                    "Classic has no secondary glyph");
        }
        var valid = semantic(TargetType.BLOCK, ActionKind.MINE, ActionState.NORMAL, ActionKind.TRANSFORM, ActionState.NORMAL);
        var blockTransform = PresentationResolver.resolve(valid, CrosshairTheme.CLASSIC_PLUS);
        check(blockTransform.base() == ClassicCrosshairType.BLOCK && blockTransform.modifier() == CrosshairPresentation.SecondaryModifier.TRANSFORM,
                "BLOCK + TRANSFORM retains block base");
        var attackInteract = PresentationResolver.resolve(semantic(TargetType.ENTITY, ActionKind.ATTACK, ActionState.COOLDOWN,
                ActionKind.INTERACT, ActionState.NORMAL), CrosshairTheme.CLASSIC_PLUS);
        check(attackInteract.base() == ClassicCrosshairType.ATTACK && attackInteract.modifier() == CrosshairPresentation.SecondaryModifier.INTERACT,
                "cooldown keeps ATTACK + INTERACT without a third element");
        check(PresentationResolver.resolve(semantic(TargetType.ENTITY, ActionKind.ATTACK, ActionState.COOLDOWN,
                ActionKind.NONE, ActionState.NORMAL), CrosshairTheme.CLASSIC_PLUS).modifier() == CrosshairPresentation.SecondaryModifier.NONE,
                "attack cooldown has no auxiliary glyph");
        check(PresentationResolver.resolve(semantic(TargetType.BLOCK, ActionKind.MINE, ActionState.INVALID,
                ActionKind.NONE, ActionState.NORMAL), CrosshairTheme.CLASSIC_PLUS).equals(
                new CrosshairPresentation(Visibility.SHOW, ClassicCrosshairType.ERROR, CrosshairPresentation.SecondaryModifier.NONE)),
                "invalid harvest is ERROR only");
        for (var target : TargetType.values()) {
            for (var action : List.of(ActionKind.INTERACT, ActionKind.USE, ActionKind.PLACE, ActionKind.TRANSFORM, ActionKind.SPECIAL)) {
                for (var status : ActionState.values()) {
                    var state = semantic(target, ActionKind.NONE, ActionState.NORMAL, action, status);
                    var mapped = PresentationResolver.resolve(state, CrosshairTheme.CLASSIC_PLUS);
                    boolean show = status == ActionState.NORMAL && action != ActionKind.SPECIAL
                            && (action != ActionKind.USE || target != TargetType.MISS);
                    check(show ? mapped.modifier().name().equals(action.name())
                            : mapped.modifier() == CrosshairPresentation.SecondaryModifier.NONE,
                            "selective visibility " + target + "/" + action + "/" + status);
                    check(state.secondary().action() == action && state.secondary().state() == status
                            && state.secondary().evidence().get().equals(state.candidates().get(1)),
                            "presentation preserves action/state/evidence");
                }
            }
        }
        var charged = new HandState(CrosshairContextSnapshot.RangedItem.CROSSBOW, true, false);
        for (String hand : List.of("main", "off")) {
            var rangedState = withEvidence(semantic(TargetType.MISS, ActionKind.NONE, ActionState.NORMAL, ActionKind.USE, ActionState.READY),
                    CandidateSource.VANILLA_RUNTIME, "buildup_situational_crosshair:crossbow/" + hand,
                    hand.equals("main") ? charged : HandState.EMPTY, hand.equals("off") ? charged : HandState.EMPTY);
            check(PresentationResolver.resolve(rangedState, CrosshairTheme.CLASSIC).base() == ClassicCrosshairType.ATTACK, "Classic charged appearance preserved");
            check(PresentationResolver.resolve(rangedState, CrosshairTheme.CLASSIC_PLUS).equals(
                    new CrosshairPresentation(Visibility.SHOW, ClassicCrosshairType.ATTACK, CrosshairPresentation.SecondaryModifier.NONE)),
                    "effective loaded crossbow READY reuses only ATTACK base");
            check(rangedState.secondary().action() == ActionKind.USE && rangedState.secondary().state() == ActionState.READY,
                    "READY remains secondary USE");
        }
        var ready = semantic(TargetType.MISS, ActionKind.NONE, ActionState.NORMAL, ActionKind.USE, ActionState.READY);
        for (var source : List.of(CandidateSource.VANILLA_RUNTIME, CandidateSource.PACK_RULE)) {
            var unrelated = withEvidence(ready, source, source == CandidateSource.PACK_RULE
                    ? "buildup_situational_crosshair:crossbow/off" : "buildup_situational_crosshair:bow/main", HandState.EMPTY, charged);
            check(PresentationResolver.resolve(unrelated, CrosshairTheme.CLASSIC_PLUS).base() == ClassicCrosshairType.DOT,
                    "unrelated charged hand/pack READY never fabricates ranged ready");
        }
        check(PresentationResolver.resolve(withEvidence(ready, CandidateSource.VANILLA_RUNTIME,
                "buildup_situational_crosshair:crossbow/main", HandState.EMPTY, charged), CrosshairTheme.CLASSIC_PLUS).base() == ClassicCrosshairType.DOT,
                "native provenance still requires charged effective hand");
        for (String origin : List.of("consumable", "blocking_item", "spyglass", "bow", "crossbow")) {
            var selfUse = withEvidence(semantic(TargetType.BLOCK, ActionKind.MINE, ActionState.NORMAL, ActionKind.USE, ActionState.NORMAL),
                    origin.equals("consumable") || origin.equals("blocking_item") ? CandidateSource.VANILLA_COMPONENT : CandidateSource.VANILLA_RUNTIME,
                    "buildup_situational_crosshair:" + origin + "/main", HandState.EMPTY, HandState.EMPTY);
            check(PresentationResolver.resolve(selfUse, CrosshairTheme.CLASSIC_PLUS).modifier() == CrosshairPresentation.SecondaryModifier.NONE,
                    "item-global use stays quiet even over a target");
        }
        for (var visibility : List.of(Visibility.HIDE, Visibility.VANILLA)) {
            var state = new CrosshairResolver(List.of()).resolve(CrosshairContextSnapshot.unavailable(visibility));
            for (var theme : CrosshairTheme.values()) {
                var mapped = PresentationResolver.resolve(state, theme);
                check(!mapped.customVisible() && mapped.visibility() == visibility, "visibility hard gate " + theme);
            }
        }
        var pool = new ArrayList<>(valid.candidates());
        pool.add(new ActionCandidate(ActionSlot.SECONDARY, ActionKind.PLACE, ActionState.NORMAL, CandidateSource.PACK_RULE,
                Specificity.GENERIC, Confidence.STRONG, -100, "test:unused"));
        var irrelevant = new CrosshairSemanticState(valid.target(), valid.visibility(), valid.primary(), valid.secondary(), pool, HandState.EMPTY, HandState.EMPTY);
        check(PresentationResolver.resolve(irrelevant, CrosshairTheme.CLASSIC_PLUS).equals(blockTransform), "presentation uses only resolved secondary");
        var unique = new HashSet<PixelGlyph>();
        for (var modifier : CrosshairPresentation.SecondaryModifier.values()) if (modifier != CrosshairPresentation.SecondaryModifier.NONE)
            check(unique.add(PixelGlyph.modifier(modifier)), "distinct action glyph " + modifier);
        check(PixelGlyph.modifier(CrosshairPresentation.SecondaryModifier.NONE) == null, "absence draws no detail");
        check(CrosshairPresentation.class.getRecordComponents().length == 3, "render model has no independent status channels");
        check(PixelGlyph.SIZE == 3, "micro foreground budget");
        check(ActionState.values().length == 6 && ActionKind.valueOf("SPECIAL") == ActionKind.SPECIAL, "semantic states/actions retained");

        var transition = new TransitionController();
        check(transition.update(blockTransform, TransitionMode.OFF, 0).detailOpacity() == 1, "OFF is immediate");
        check(transition.update(blockTransform, TransitionMode.SUBTLE, 1).detailOpacity() == 0.35f, "SUBTLE starts legible");
        float half = transition.update(blockTransform, TransitionMode.SUBTLE, 50_000_001).detailOpacity();
        check(Math.abs(half - 0.675f) < 0.0001f, "elapsed-time midpoint");
        check(transition.update(blockTransform, TransitionMode.SUBTLE, 100_000_001).detailOpacity() == 1, "100ms completes");
        var changed = transition.update(attackInteract, TransitionMode.SUBTLE, 101_000_001);
        check(changed.presentation().equals(attackInteract) && changed.detailOpacity() == 0.35f, "new effective action replaces old immediately");
        check(transition.update(attackInteract, TransitionMode.OFF, 101_000_002).detailOpacity() == 1, "disabling mid-transition snaps");
        var hidden = CrosshairPresentation.unavailable(Visibility.HIDE);
        check(!transition.update(hidden, TransitionMode.SUBTLE, 102_000_000).presentation().customVisible(), "hidden never fades out");
        check(transition.update(blockTransform, TransitionMode.SUBTLE, 103_000_000).detailOpacity() == 0.35f, "hidden resets history");
        check(transition.update(blockTransform, TransitionMode.SUBTLE, 0).detailOpacity() == 0.35f, "clock rollback bounded");
        System.out.println("PASS: " + checks + " presentation/transition checks");
    }
    public static CrosshairSemanticState semantic(TargetType target, ActionKind primary, ActionState primaryStatus,
            ActionKind secondary, ActionState secondaryStatus) {
        var a = candidate(ActionSlot.PRIMARY, primary, primaryStatus);
        var b = candidate(ActionSlot.SECONDARY, secondary, secondaryStatus);
        return new CrosshairSemanticState(target, Visibility.SHOW, CrosshairSemanticState.ResolvedAction.of(a),
                CrosshairSemanticState.ResolvedAction.of(b), List.of(a, b), HandState.EMPTY, HandState.EMPTY);
    }
    private static CrosshairSemanticState withEvidence(CrosshairSemanticState state, CandidateSource source, String origin,
            HandState main, HandState off) {
        var evidence = new ActionCandidate(ActionSlot.SECONDARY, state.secondary().action(), state.secondary().state(),
                source, Specificity.EXACT_CONTEXT, Confidence.EXACT, 0, origin);
        return new CrosshairSemanticState(state.target(), state.visibility(), state.primary(), CrosshairSemanticState.ResolvedAction.of(evidence),
                List.of(state.candidates().get(0), evidence), main, off);
    }
    private static ActionCandidate candidate(ActionSlot slot, ActionKind action, ActionState status) {
        return new ActionCandidate(slot, action, status, CandidateSource.VANILLA_RUNTIME, Specificity.EXACT_CONTEXT, Confidence.EXACT, 0,
                "test:" + slot.name().toLowerCase(Locale.ROOT));
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); checks++; }
}
