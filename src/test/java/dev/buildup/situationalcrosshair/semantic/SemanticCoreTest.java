package dev.buildup.situationalcrosshair.semantic;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import dev.buildup.situationalcrosshair.crosshair.ClassicPresentation;
import dev.buildup.situationalcrosshair.crosshair.ClassicCrosshairType;
import static dev.buildup.situationalcrosshair.semantic.CrosshairContextSnapshot.*;

/** Dependency-free contract suite. Explicit checks run regardless of JVM assertion flags. */
public final class SemanticCoreTest {
    private static int checks;
    private static final CrosshairResolver CLASSIC = new CrosshairResolver(List.of(new BaseTargetProvider(),
            new HarvestProvider(), new EntityAttackProvider(), new ClassicCrossbowProvider()));

    public static void main(String[] args) {
        var miss = snapshot(TargetType.MISS, Capability.UNKNOWN, Capability.UNKNOWN, HandState.EMPTY, HandState.EMPTY);
        var valid = snapshot(TargetType.BLOCK, Capability.YES, Capability.YES, HandState.EMPTY, HandState.EMPTY);
        var invalid = snapshot(TargetType.BLOCK, Capability.YES, Capability.NO, HandState.EMPTY, HandState.EMPTY);
        var entity = snapshot(TargetType.ENTITY, Capability.UNKNOWN, Capability.UNKNOWN, HandState.EMPTY, HandState.EMPTY);
        check(ClassicPresentation.map(CLASSIC.resolve(miss)) == ClassicCrosshairType.DOT, "MISS/NONE => DOT");
        check(ClassicPresentation.map(CLASSIC.resolve(valid)) == ClassicCrosshairType.BLOCK, "MINE/NORMAL => BLOCK");
        check(ClassicPresentation.map(CLASSIC.resolve(invalid)) == ClassicCrosshairType.ERROR, "MINE/INVALID => ERROR");
        check(ClassicPresentation.map(CLASSIC.resolve(entity)) == ClassicCrosshairType.ATTACK, "ATTACK => ATTACK");
        check(CLASSIC.resolve(invalid).primary().action() == ActionKind.MINE, "invalid harvest remains MINE");
        check(CLASSIC.resolve(valid).secondary().action() == ActionKind.NONE, "secondary normally NONE");
        check(ClassicPresentation.map(CLASSIC.resolve(snapshot(TargetType.BLOCK, Capability.UNKNOWN,
                Capability.UNKNOWN, HandState.EMPTY, HandState.EMPTY))) == null, "unknown harvest fallback");

        var charged = new HandState(RangedItem.CROSSBOW, true, false);
        var ranged = CLASSIC.resolve(snapshot(TargetType.BLOCK, Capability.YES, Capability.NO, charged, HandState.EMPTY));
        check(ranged.primary().action() == ActionKind.MINE && ranged.primary().state() == ActionState.INVALID,
                "secondary does not erase mining");
        check(ranged.secondary().action() == ActionKind.USE && ranged.secondary().state() == ActionState.READY,
                "charged crossbow uses secondary");
        check(ClassicPresentation.map(ranged) == ClassicCrosshairType.ATTACK, "Classic charged override");
        check(ClassicPresentation.map(CLASSIC.resolve(snapshot(TargetType.MISS, Capability.UNKNOWN,
                Capability.UNKNOWN, charged, new HandState(RangedItem.BOW, false, true)))) == ClassicCrosshairType.DOT,
                "offhand bow parity, no new charging visualization");

        for (var visibility : List.of(Visibility.HIDE, Visibility.VANILLA)) {
            var gate = new CrosshairResolver(List.of((context, collector) -> { throw new AssertionError("gate did not short-circuit"); }));
            var state = gate.resolve(CrosshairContextSnapshot.unavailable(visibility));
            check(state.visibility() == visibility && state.candidates().isEmpty(), "visibility hard gate");
            check(ClassicPresentation.map(state) == null, "visibility precedes presentation");
        }

        for (var slot : ActionSlot.values()) for (var action : ActionKind.values()) {
            if (action.supports(slot)) {
                check(candidate(slot, action, Specificity.EXACT_TARGET, CandidateSource.VANILLA_RUNTIME,
                        Confidence.EXACT, 0, "test:legal").slot() == slot, "legal combination");
            } else expectIllegal(() -> candidate(slot, action, Specificity.EXACT_TARGET,
                    CandidateSource.VANILLA_RUNTIME, Confidence.EXACT, 0, "test:illegal"), "illegal combination");
        }
        expectIllegal(() -> candidate(ActionSlot.PRIMARY, ActionKind.MINE, Specificity.GENERIC,
                CandidateSource.PACK_RULE, Confidence.EXACT, 101, "test:priority"), "priority bound");
        expectIllegal(() -> candidate(ActionSlot.PRIMARY, ActionKind.MINE, Specificity.GENERIC,
                CandidateSource.PACK_RULE, Confidence.EXACT, -101, "test:priority"), "negative bound");
        expectIllegal(() -> candidate(ActionSlot.PRIMARY, ActionKind.MINE, Specificity.GENERIC,
                CandidateSource.PACK_RULE, Confidence.EXACT, 0, "not an id"), "origin validation");
        expectIllegal(() -> new ActionCandidate(ActionSlot.PRIMARY, ActionKind.NONE, ActionState.INVALID,
                CandidateSource.UNKNOWN, Specificity.GENERIC, Confidence.UNKNOWN, 0, "test:none"), "NONE state validation");

        var exact = candidate(ActionSlot.PRIMARY, ActionKind.ATTACK, Specificity.EXACT_CONTEXT,
                CandidateSource.VANILLA_RUNTIME, Confidence.EXACT, -100, "test:runtime");
        var generic = candidate(ActionSlot.PRIMARY, ActionKind.SPECIAL, Specificity.GENERIC,
                CandidateSource.PACK_RULE, Confidence.STRONG, 100, "test:broad");
        var secondary = candidate(ActionSlot.SECONDARY, ActionKind.INTERACT, Specificity.EXACT_TARGET,
                CandidateSource.VANILLA_RUNTIME, Confidence.EXACT, 0, "test:interact");
        var weak = candidate(ActionSlot.PRIMARY, ActionKind.SPECIAL, Specificity.EXACT_CONTEXT,
                CandidateSource.VANILLA_RUNTIME, Confidence.INFERRED, 100, "test:weak");
        var baseline = resolve(entity, List.of(exact, generic, secondary, weak));
        check(baseline.primary().evidence().orElseThrow().equals(exact), "exact runtime beats broad priority and weak guess");
        check(baseline.secondary().action() == ActionKind.INTERACT, "slots resolve independently");
        var pool = new ArrayList<>(List.of(exact, generic, secondary, weak));
        for (int seed = 0; seed < 32; seed++) {
            Collections.shuffle(pool, new Random(seed));
            check(resolve(entity, pool).equals(baseline), "order-independent resolution " + seed);
        }
        var authority = candidate(ActionSlot.PRIMARY, ActionKind.SPECIAL, Specificity.EXACT_CONTEXT,
                CandidateSource.PACK_RULE, Confidence.AUTHORITATIVE, 100, "test:pack");
        check(resolve(entity, List.of(authority, exact)).primary().action() == ActionKind.ATTACK,
                "source authority precedes local priority/confidence");
        var tieA = candidate(ActionSlot.PRIMARY, ActionKind.ATTACK, Specificity.EXACT_TARGET,
                CandidateSource.VANILLA_RUNTIME, Confidence.EXACT, 0, "test:a");
        var tieB = candidate(ActionSlot.PRIMARY, ActionKind.SPECIAL, Specificity.EXACT_TARGET,
                CandidateSource.VANILLA_RUNTIME, Confidence.EXACT, 0, "test:b");
        check(resolve(entity, List.of(tieB, tieA)).primary().evidence().orElseThrow().equals(tieA), "stable origin tie-break");
        check(resolve(entity, List.of(weak)).primary().action() == ActionKind.NONE, "weak confidence falls back");
        var collector = new CandidateCollector();
        collector.add(exact);
        var frozen = collector.candidates();
        collector.add(secondary);
        collector.add(exact);
        check(frozen.size() == 1 && collector.candidates().size() == 2, "immutable snapshots and deduplication");
        try { frozen.clear(); throw new AssertionError("mutable pool"); }
        catch (UnsupportedOperationException expected) { checks++; }
        var shared = new CrosshairResolver(List.of((context, sink) -> check(context == valid, "first shared snapshot"),
                (context, sink) -> check(context == valid, "second shared snapshot")));
        shared.resolve(valid);
        System.out.println("PASS: " + checks + " semantic/presentation contract checks");
    }

    private static CrosshairSemanticState resolve(CrosshairContextSnapshot context, List<ActionCandidate> candidates) {
        return new CrosshairResolver(List.of((snapshot, sink) -> candidates.forEach(sink::add))).resolve(context);
    }
    private static CrosshairContextSnapshot snapshot(TargetType target, Capability breaking, Capability harvest,
                                                     HandState main, HandState off) {
        return new CrosshairContextSnapshot(target, Visibility.SHOW, false, breaking, harvest, main, off);
    }
    private static ActionCandidate candidate(ActionSlot slot, ActionKind action, Specificity specificity,
            CandidateSource source, Confidence confidence, int priority, String origin) {
        return new ActionCandidate(slot, action, ActionState.NORMAL, source, specificity, confidence, priority, origin);
    }
    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        checks++;
    }
    private static void expectIllegal(Runnable action, String label) {
        try { action.run(); } catch (IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError(label);
    }
}
