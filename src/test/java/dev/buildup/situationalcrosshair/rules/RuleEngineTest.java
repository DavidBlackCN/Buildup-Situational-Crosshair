package dev.buildup.situationalcrosshair.rules;

import dev.buildup.situationalcrosshair.semantic.*;
import java.io.StringReader;
import java.util.*;
import static dev.buildup.situationalcrosshair.semantic.CrosshairContextSnapshot.*;

public final class RuleEngineTest {
    private static int checks;
    private static final String EMIT = "\"emit\":[{\"slot\":\"secondary\",\"action\":\"special\"}]";
    public static void main(String[] args) throws Exception {
        var context = context("exampletech:crusher", Set.of("c:machines"), "exampletech:wrench", Set.of("c:tools/wrench"));
        var fallback = fixture("generic_wrench");
        var override = fixture("exact_override");
        var deny = fixture("deny");
        check(resolve(context, List.of(fallback), List.of()).secondary().action() == ActionKind.SPECIAL, "generic wrench fallback");
        var nativeUse = nativeCandidate(ActionSlot.SECONDARY, ActionKind.INTERACT, Specificity.EXACT_CONTEXT);
        var nativeMine = nativeCandidate(ActionSlot.PRIMARY, ActionKind.MINE, Specificity.EXACT_CONTEXT);
        check(resolve(context, List.of(fallback), List.of(nativeUse)).secondary().action() == ActionKind.INTERACT, "fallback cannot replace native action");
        var forced = resolve(context, List.of(override), List.of(nativeMine, nativeUse));
        check(forced.primary().action() == ActionKind.MINE && forced.secondary().action() == ActionKind.TRANSFORM,
                "override is per slot");
        var protectedTarget = context("exampletech:protected_machine", Set.of(), "exampletech:wrench", Set.of("c:tools/wrench"));
        check(resolve(protectedTarget, List.of(deny), List.of(nativeCandidate(ActionSlot.SECONDARY, ActionKind.TRANSFORM,
                Specificity.EXACT_CONTEXT))).secondary().action() == ActionKind.NONE, "deny fixture filters native action");
        var wildcard = parse("test:deny_all", "{\"schema\":1,\"mode\":\"deny\",\"emit\":[{\"slot\":\"secondary\",\"action\":\"*\"}]}");
        var denied = resolve(context, List.of(override, fallback, wildcard), List.of(nativeMine, nativeUse));
        check(denied.secondary().action() == ActionKind.NONE && denied.primary().action() == ActionKind.MINE, "deny precedes override and fallback; other slot survives");
        var broad = parse("test:broad", "{\"schema\":1,\"mode\":\"augment\",\"priority\":100," + EMIT + "}");
        check(resolve(context, List.of(broad), List.of(nativeUse)).secondary().action() == ActionKind.INTERACT, "broad high priority cannot defeat exact runtime");
        check(resolve(context, List.of(broad), List.of()).secondary().action() == ActionKind.SPECIAL, "generic augment beats absence marker");
        var broadFallback = parse("test:generic", "{\"schema\":1,\"mode\":\"fallback\"," + EMIT + "}");
        check(resolve(context, List.of(broadFallback), List.of()).secondary().action() == ActionKind.SPECIAL, "generic fallback beats absence marker");
        var none = parse("test:none", "{\"schema\":1,\"mode\":\"override\",\"emit\":[{\"slot\":\"secondary\",\"action\":\"none\"}]}");
        check(resolve(context, List.of(none, fallback), List.of(nativeUse)).secondary().action() == ActionKind.NONE, "explicit NONE override");
        var exact = parse("test:exact", "{\"schema\":1,\"mode\":\"augment\",\"priority\":-100,\"when\":{\"target\":{\"ids\":[\"exampletech:crusher\"]}},\"emit\":[{\"slot\":\"secondary\",\"action\":\"transform\"}]}");
        check(resolve(context, List.of(broad, exact), List.of()).secondary().action() == ActionKind.TRANSFORM, "exact beats broad regardless of priority");

        var selectors = parse("test:selectors", """
                {"schema":1,"mode":"augment","when":{
                  "target":{"type":"block","ids":["other:x"],"tags":["c:machines"],"namespace":"exampletech","properties":{"active":"true"}},
                  "held":{"hand":"either","ids":["other:y"],"tags":["c:tools/wrench"]},
                  "player":{"sneaking":true,"creative":false,"using_item":false}},
                  "emit":[{"slot":"secondary","action":"transform"}]}
                """);
        check(selectors.matches(context), "ids OR tags; selectors AND; properties and player");
        check(selectors.specificity(context) == Specificity.TAG_PAIR, "unmatched exact branches do not inflate specificity");
        check(!selectors.matches(context("other:crusher", Set.of("c:machines"), "exampletech:wrench", Set.of("c:tools/wrench"))), "namespace also AND");
        var eitherFacts = new RuleFacts(context.ruleFacts().target(), RuleFacts.Identity.EMPTY, context.ruleFacts().main(), Map.of("active", "true"), true, false);
        var offContext = withFacts(context, eitherFacts);
        check(selectors.matches(offContext), "either matches offhand");
        var mainOnly = parse("test:main", json("augment", "\"held\":{\"hand\":\"main\",\"tags\":[\"c:tools/wrench\"]}"));
        check(!mainOnly.matches(offContext), "main selector does not match offhand");
        var offOnly = parse("test:off", json("augment", "\"held\":{\"hand\":\"off\",\"tags\":[\"c:tools/wrench\"]}"));
        check(offOnly.matches(offContext), "off selector");
        var propertyMismatch = withFacts(context, new RuleFacts(context.ruleFacts().target(), context.ruleFacts().main(), RuleFacts.Identity.EMPTY, Map.of("active", "false"), true, false));
        check(!selectors.matches(propertyMismatch), "property mismatch");
        var missingMod = parse("test:optional", "{\"schema\":1,\"mode\":\"augment\",\"requires\":{\"mods\":[\"exampletech\",\"another\"]}," + EMIT + "}");
        check(new CompiledRuleSet(List.of(missingMod), mod -> mod.equals("exampletech")).size() == 0, "requires all mods");
        check(new CompiledRuleSet(List.of(missingMod), mod -> true).size() == 1, "required mods present");
        check(new CompiledRuleSet(List.of(selectors), mod -> true).eligible(context).size() == 1, "tag OR branch indexed");
        var namespace = parse("test:namespace", json("augment", "\"target\":{\"namespace\":\"exampletech\"}"));
        check(new CompiledRuleSet(List.of(namespace), mod -> true).eligible(context).size() == 1, "namespace index matches");
        check(new CompiledRuleSet(List.of(namespace), mod -> true).eligible(protectedTarget).size() == 1, "namespace covers distinct IDs");
        check(new CompiledRuleSet(List.of(namespace), mod -> true).eligible(context("other:block", Set.of(), "other:item", Set.of())).isEmpty(), "unrelated namespace excluded");
        var emptyMods = parse("test:empty_mods", "{\"schema\":1,\"mode\":\"augment\",\"requires\":{\"mods\":[]}," + EMIT + "}");
        check(new CompiledRuleSet(List.of(emptyMods), mod -> false).size() == 1, "empty optional-mod requirements");
        var multiple = parse("test:multiple", "{\"schema\":1,\"mode\":\"override\",\"emit\":[{\"slot\":\"secondary\",\"action\":\"special\"},{\"slot\":\"secondary\",\"action\":\"use\"}]}");
        check(resolve(context, List.of(multiple), List.of()).secondary().action() == ActionKind.USE, "multiple emissions use stable semantic tie-break");
        var denyReady = parse("test:deny_ready", "{\"schema\":1,\"mode\":\"deny\",\"emit\":[{\"slot\":\"secondary\",\"action\":\"use\",\"state\":\"ready\"}]}");
        var ready = new ActionCandidate(ActionSlot.SECONDARY, ActionKind.USE, ActionState.READY, CandidateSource.VANILLA_RUNTIME,
                Specificity.EXACT_CONTEXT, Confidence.EXACT, 0, "test:ready");
        check(resolve(context, List.of(denyReady), List.of(ready)).secondary().action() == ActionKind.NONE, "state-specific deny matches");
        check(resolve(context, List.of(denyReady), List.of(nativeCandidate(ActionSlot.SECONDARY, ActionKind.USE,
                Specificity.EXACT_CONTEXT))).secondary().action() == ActionKind.USE, "state-specific deny preserves other states");
        var aOverride = parse("test:a", "{\"schema\":1,\"mode\":\"override\",\"emit\":[{\"slot\":\"secondary\",\"action\":\"place\"}]}");
        var zOverride = parse("test:z", "{\"schema\":1,\"mode\":\"override\"," + EMIT + "}");
        check(resolve(context, List.of(zOverride, aOverride), List.of(nativeUse)).secondary().action() == ActionKind.PLACE,
                "conflicting equal overrides use stable origin tie-break");
        check(resolve(context, List.of(broadFallback), List.of(new ActionCandidate(ActionSlot.SECONDARY, ActionKind.INTERACT,
                ActionState.NORMAL, CandidateSource.GENERIC_INFERENCE, Specificity.EXACT_CONTEXT, Confidence.INFERRED, 100,
                "test:weak"))).secondary().action() == ActionKind.SPECIAL, "weak candidate does not prevent fallback");

        for (var invalid : List.of(
                "{\"schema\":2,\"mode\":\"augment\"," + EMIT + "}",
                "{\"schema\":1,\"mode\":\"invalid\"," + EMIT + "}",
                "{\"schema\":1,\"mode\":\"augment\",\"priority\":101," + EMIT + "}",
                "{\"schema\":1,\"mode\":\"augment\",\"priority\":-101," + EMIT + "}",
                "{\"schema\":1,\"mode\":\"augment\",\"priority\":0.5," + EMIT + "}",
                "{\"schema\":1,\"mode\":\"augment\",\"priority\":\"1\"," + EMIT + "}",
                json("augment", "\"target\":{\"type\":\"entity\",\"properties\":{\"x\":\"true\"}}"),
                json("augment", "\"target\":{\"properties\":{}}"),
                json("augment", "\"target\":{\"type\":\"miss\",\"ids\":[\"a:b\"]}"),
                json("augment", "\"target\":{\"ids\":[]}"),
                json("augment", "\"held\":{\"tags\":[\"#c:wrench\"]}"),
                json("augment", "\"held\":{\"hand\":\"left\"}"),
                json("augment", "\"player\":{\"sneaking\":\"true\"}"),
                json("augment", "\"player\":{\"expression\":\"true\"}"),
                "{\"schema\":1,\"mode\":\"augment\",\"emit\":[{\"slot\":\"primary\",\"action\":\"place\"}]}",
                "{\"schema\":1,\"mode\":\"augment\",\"emit\":[{\"slot\":\"secondary\",\"action\":\"*\"}]}",
                "{\"schema\":1,\"mode\":\"override\",\"emit\":[{\"slot\":\"primary\",\"action\":\"none\",\"state\":\"invalid\"}]}",
                "{\"schema\":1,\"mode\":\"augment\",\"emit\":[]}",
                "{\"schema\":1,\"schema\":1,\"mode\":\"augment\"," + EMIT + "}",
                "{\"schema\":1,\"mode\":\"augment\",\"texture\":\"a:b\"," + EMIT + "}",
                "{\"schema\":1,\"mode\":\"augment\",\"when\":null," + EMIT + "}",
                "{\"schema\":1,\"mode\":\"augment\",\"requires\":{\"mods\":[\"Bad ID\"]}," + EMIT + "}",
                "{\"schema\":1,\"mode\":\"augment\",\"emit\":[{\"slot\":\"secondary\",\"action\":\"special\",\"action\":\"use\"}]}",
                "{/*comment*/\"schema\":1,\"mode\":\"augment\"," + EMIT + "}",
                "{\"schema\":1,\"mode\":\"augment\"," + EMIT + "} {}")) {
            try { parse("test:bad", invalid); throw new AssertionError("accepted: " + invalid); }
            catch (IllegalArgumentException | java.io.IOException expected) { checks++; }
        }

        var many = new ArrayList<Rule>();
        for (int i = 0; i < 2000; i++) many.add(parse("pack:r" + i, json("augment", "\"target\":{\"ids\":[\"pack:block" + i + "\"]}")));
        many.add(exact);
        var indexed = new CompiledRuleSet(many, mod -> true);
        check(indexed.size() == 2001 && indexed.eligible(context).size() == 1, "2000 unrelated rules excluded before full match");
        var expected = resolve(context, List.of(exact, broad, fallback), List.of(nativeMine));
        var shuffled = new ArrayList<>(List.of(exact, broad, fallback));
        for (int i = 0; i < 32; i++) { Collections.shuffle(shuffled, new Random(i)); check(resolve(context, shuffled, List.of(nativeMine)).equals(expected), "deterministic " + i); }
        var hidden = new CrosshairResolver(List.of(), () -> { throw new AssertionError("hidden queried rules"); });
        check(hidden.resolve(CrosshairContextSnapshot.unavailable(Visibility.HIDE)).candidates().isEmpty(), "visibility gate precedes rules");
        System.out.println("PASS: " + checks + " rule engine checks; 2001-rule index selected 1 rule");
    }
    private static String json(String mode, String when) { return "{\"schema\":1,\"mode\":\"" + mode + "\",\"when\":{" + when + "}," + EMIT + "}"; }
    private static Rule fixture(String name) throws Exception {
        try (var reader = new java.io.InputStreamReader(Objects.requireNonNull(RuleEngineTest.class.getResourceAsStream("/rules/" + name + ".json")), java.nio.charset.StandardCharsets.UTF_8)) {
            return RuleParser.parse("fixtures:" + name, reader);
        }
    }
    private static Rule parse(String id, String json) throws java.io.IOException { return RuleParser.parse(id, new StringReader(json)); }
    private static ActionCandidate nativeCandidate(ActionSlot slot, ActionKind action, Specificity specificity) {
        return new ActionCandidate(slot, action, ActionState.NORMAL, CandidateSource.VANILLA_RUNTIME, specificity, Confidence.EXACT, 0, "test:native");
    }
    private static CrosshairSemanticState resolve(CrosshairContextSnapshot context, List<Rule> rules, List<ActionCandidate> nativeCandidates) {
        var compiled = new CompiledRuleSet(rules, mod -> true);
        return new CrosshairResolver(List.of(new BaseTargetProvider(), (snapshot, sink) -> nativeCandidates.forEach(sink::add)), () -> compiled).resolve(context);
    }
    private static CrosshairContextSnapshot context(String target, Set<String> targetTags, String item, Set<String> itemTags) {
        var facts = new RuleFacts(new RuleFacts.Identity(target, targetTags), new RuleFacts.Identity(item, itemTags),
                RuleFacts.Identity.EMPTY, Map.of("active", "true"), true, false);
        return new CrosshairContextSnapshot(TargetType.BLOCK, Visibility.SHOW, false, Capability.YES, Capability.YES,
                HandState.EMPTY, HandState.EMPTY, List.of(), false, false, true, facts);
    }
    private static CrosshairContextSnapshot withFacts(CrosshairContextSnapshot c, RuleFacts facts) {
        return new CrosshairContextSnapshot(c.target(), c.visibility(), c.creative(), c.breakability(), c.harvestability(),
                c.mainHand(), c.offHand(), c.useAttempts(), c.attackCooling(), c.spectator(), c.nativeCaptured(), facts);
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); checks++; }
}
