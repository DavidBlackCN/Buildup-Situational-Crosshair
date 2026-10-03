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
            check(PresentationResolver.resolve(state, CrosshairTheme.CLASSIC).rightSidecar() == CrosshairPresentation.ActionSidecar.NONE,
                    "Classic has no secondary glyph");
        }
        var valid = semantic(TargetType.BLOCK, ActionKind.MINE, ActionState.NORMAL, ActionKind.TRANSFORM, ActionState.NORMAL);
        var blockTransform = PresentationResolver.resolve(valid, CrosshairTheme.CLASSIC_PLUS);
        check(blockTransform.base() == ClassicCrosshairType.BLOCK && blockTransform.rightSidecar() == CrosshairPresentation.ActionSidecar.TRANSFORM,
                "BLOCK + TRANSFORM retains block base");
        var attackInteract = PresentationResolver.resolve(semantic(TargetType.ENTITY, ActionKind.ATTACK, ActionState.COOLDOWN,
                ActionKind.INTERACT, ActionState.NORMAL), CrosshairTheme.CLASSIC_PLUS);
        check(attackInteract.base() == ClassicCrosshairType.ATTACK && attackInteract.rightSidecar() == CrosshairPresentation.ActionSidecar.INTERACT,
                "cooldown keeps ATTACK + INTERACT without a third element");
        check(PresentationResolver.resolve(semantic(TargetType.ENTITY, ActionKind.ATTACK, ActionState.COOLDOWN,
                ActionKind.NONE, ActionState.NORMAL), CrosshairTheme.CLASSIC_PLUS).rightSidecar() == CrosshairPresentation.ActionSidecar.NONE,
                "attack cooldown has no auxiliary glyph");
        check(PresentationResolver.resolve(semantic(TargetType.BLOCK, ActionKind.MINE, ActionState.INVALID,
                ActionKind.NONE, ActionState.NORMAL), CrosshairTheme.CLASSIC_PLUS).equals(
                CrosshairPresentation.baseOnly(ClassicCrosshairType.ERROR)),
                "invalid harvest is ERROR only");
        for (var target : TargetType.values()) {
            for (var action : List.of(ActionKind.INTERACT, ActionKind.USE, ActionKind.PLACE, ActionKind.TRANSFORM, ActionKind.SPECIAL)) {
                for (var status : ActionState.values()) {
                    var state = semantic(target, target == TargetType.BLOCK ? ActionKind.MINE : target == TargetType.ENTITY ? ActionKind.ATTACK : ActionKind.NONE,
                            ActionState.NORMAL, action, status);
                    var mapped = PresentationResolver.resolve(state, CrosshairTheme.CLASSIC_PLUS);
                    boolean show = target != TargetType.MISS && action != ActionKind.SPECIAL
                            && (status == ActionState.NORMAL || status == ActionState.BLOCKED);
                    check(show ? mapped.rightSidecar().name().equals(action.name())
                            : mapped.rightSidecar() == CrosshairPresentation.ActionSidecar.NONE,
                            "selective visibility " + target + "/" + action + "/" + status);
                    check(mapped.sidecarCount() <= 2 && mapped.leftSidecar() ==
                            (show && status == ActionState.BLOCKED ? CrosshairPresentation.StateSidecar.BLOCKED : CrosshairPresentation.StateSidecar.NONE),
                            "left is exceptional state only, count bounded");
                    var classic = PresentationResolver.resolve(state, CrosshairTheme.CLASSIC);
                    check(classic.sidecarCount() == 0 && classic.base() == ClassicPresentation.map(state), "Classic always exact and undecorated");
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
                    CrosshairPresentation.baseOnly(ClassicCrosshairType.ATTACK)),
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
            check(PresentationResolver.resolve(selfUse, CrosshairTheme.CLASSIC_PLUS).rightSidecar() == CrosshairPresentation.ActionSidecar.NONE,
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
        for (var modifier : CrosshairPresentation.ActionSidecar.values()) if (modifier != CrosshairPresentation.ActionSidecar.NONE)
            check(unique.add(PixelGlyph.action(modifier)), "distinct action glyph " + modifier);
        check(PixelGlyph.action(CrosshairPresentation.ActionSidecar.NONE) == null, "absence draws no detail");
        check(PixelGlyph.state(CrosshairPresentation.StateSidecar.NONE) == null, "no left glyph for absence");
        check(PixelGlyph.state(CrosshairPresentation.StateSidecar.BLOCKED) != null, "explicit left blocked design");
        check(CrosshairPresentation.class.getRecordComponents().length == 4, "render model has fixed left/right roles");
        check(PixelGlyph.SIZE == 7, "selected sidecar design budget");
        var blocked = PresentationResolver.resolve(semantic(TargetType.BLOCK, ActionKind.MINE, ActionState.NORMAL, ActionKind.PLACE, ActionState.BLOCKED), CrosshairTheme.CLASSIC_PLUS);
        check(blocked.leftSidecar() == CrosshairPresentation.StateSidecar.BLOCKED && blocked.rightSidecar() == CrosshairPresentation.ActionSidecar.PLACE,
                "known blocked action shows fixed-role two-sidecar composition");
        var uncertainEvidence = new ActionCandidate(ActionSlot.SECONDARY, ActionKind.PLACE, ActionState.BLOCKED, CandidateSource.GENERIC_INFERENCE,
                Specificity.GENERIC, Confidence.INFERRED, 0, "test:uncertain");
        var uncertain = new CrosshairSemanticState(TargetType.BLOCK, Visibility.SHOW, valid.primary(),
                CrosshairSemanticState.ResolvedAction.of(uncertainEvidence), List.of(valid.candidates().get(0), uncertainEvidence), HandState.EMPTY, HandState.EMPTY);
        check(PresentationResolver.resolve(uncertain, CrosshairTheme.CLASSIC_PLUS).sidecarCount() == 0, "uncertain blocked action does not invent an exceptional cue");
        var unsupported = semantic(TargetType.BLOCK, ActionKind.NONE, ActionState.NORMAL, ActionKind.INTERACT, ActionState.NORMAL);
        check(PresentationResolver.resolve(unsupported, CrosshairTheme.CLASSIC_PLUS).sidecarCount() == 0, "uncertain primary DOT cannot be overwhelmed by sidecars");
        var badToolInteract = semantic(TargetType.BLOCK, ActionKind.MINE, ActionState.INVALID, ActionKind.INTERACT, ActionState.BLOCKED);
        check(PresentationResolver.resolve(badToolInteract, CrosshairTheme.CLASSIC_PLUS).equals(CrosshairPresentation.baseOnly(ClassicCrosshairType.ERROR)),
                "invalid mining is ERROR only even with a blocked secondary");
        reject(() -> new CrosshairPresentation(Visibility.HIDE, ClassicCrosshairType.BLOCK, CrosshairPresentation.StateSidecar.NONE,
                CrosshairPresentation.ActionSidecar.PLACE), "hidden cannot carry sidecars");
        reject(() -> new CrosshairPresentation(Visibility.VANILLA, null, CrosshairPresentation.StateSidecar.BLOCKED,
                CrosshairPresentation.ActionSidecar.PLACE), "vanilla cannot carry sidecars");
        reject(() -> new CrosshairPresentation(Visibility.SHOW, ClassicCrosshairType.BLOCK, CrosshairPresentation.StateSidecar.BLOCKED,
                CrosshairPresentation.ActionSidecar.NONE), "state requires qualifying action");
        check(ActionState.values().length == 6 && ActionKind.valueOf("SPECIAL") == ActionKind.SPECIAL, "semantic states/actions retained");

        var transition = new TransitionController();
        var off = transition.update(blocked, TransitionMode.OFF, 0);
        check(off.leftOpacity() == 1 && off.rightOpacity() == 1 && off.leftOffset() == 0 && off.rightOffset() == 0, "OFF immediate");
        var first = transition.update(blocked, TransitionMode.SUBTLE, 1);
        check(first.presentation().equals(blocked) && first.leftOffset() == 2 && first.rightOffset() == -2
                && first.leftOpacity() == 0.45f && first.rightOpacity() == 0.45f, "meaning immediate; entry within 2px");
        float previousOpacity = 0;
        for (int millis = 0; millis <= 100; millis += 5) {
            var frame = transition.update(blocked, TransitionMode.SUBTLE, 1 + millis * 1_000_000L);
            check(frame.rightOpacity() >= previousOpacity && frame.rightOpacity() <= 1 && frame.leftOpacity() == frame.rightOpacity(),
                    "monotonic bounded ease-out");
            check(frame.leftOffset() >= 0 && frame.leftOffset() <= 2 && frame.rightOffset() >= -2 && frame.rightOffset() <= 0,
                    "pixel-aligned bounded docking");
            previousOpacity = frame.rightOpacity();
        }
        var settled = transition.update(blocked, TransitionMode.SUBTLE, 100_000_001);
        check(settled.leftOffset() == 0 && settled.rightOffset() == 0 && settled.rightOpacity() == 1, "100ms settles without pulse");
        var unblocked = new CrosshairPresentation(Visibility.SHOW, blocked.base(), CrosshairPresentation.StateSidecar.NONE, blocked.rightSidecar());
        var cleared = transition.update(unblocked, TransitionMode.SUBTLE, 101_000_001);
        check(cleared.presentation().leftSidecar() == CrosshairPresentation.StateSidecar.NONE && cleared.leftOffset() == 0
                && cleared.rightOpacity() == 1, "removing state leaves stable right cue, no ghost");
        var newlyBlocked = transition.update(blocked, TransitionMode.SUBTLE, 102_000_001);
        check(newlyBlocked.leftOpacity() == 0.45f && newlyBlocked.rightOpacity() == 1, "rare state docks without replaying right action");
        var newBase = new CrosshairPresentation(Visibility.SHOW, ClassicCrosshairType.ATTACK, blocked.leftSidecar(), blocked.rightSidecar());
        check(transition.update(newBase, TransitionMode.SUBTLE, 202_000_001).rightOpacity() == 1, "base changes do not restart same action");
        for (int i = 0; i < 20; i++) {
            var next = i % 2 == 0 ? blockTransform : attackInteract;
            var frame = transition.update(next, TransitionMode.SUBTLE, 203_000_001 + i);
            check(frame.presentation().equals(next) && frame.presentation().leftSidecar() == CrosshairPresentation.StateSidecar.NONE,
                    "rapid switch replaces meaning immediately without old state");
        }
        check(transition.update(blockTransform, TransitionMode.OFF, 204_000_000).rightOffset() == 0, "OFF mid-entry snaps");
        for (var visibility : List.of(Visibility.HIDE, Visibility.VANILLA)) {
            var hidden = CrosshairPresentation.unavailable(visibility);
            check(transition.update(hidden, TransitionMode.SUBTLE, 205_000_000).presentation().sidecarCount() == 0, "hidden/fallback clears frame");
            check(transition.update(blockTransform, TransitionMode.SUBTLE, 206_000_000).rightOpacity() == 0.45f, "visibility reset restarts entry");
        }
        check(transition.update(blockTransform, TransitionMode.SUBTLE, 0).rightOpacity() == 0.45f, "clock rollback bounded");
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
    private static void reject(Runnable action, String label) {
        try { action.run(); } catch (IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError(label);
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); checks++; }
}
