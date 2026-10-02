package dev.buildup.situationalcrosshair.rules;

import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.LoggerFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;

/** Reload work stays off the render path; apply publishes a complete immutable generation. */
public final class RuleManager extends SimplePreparableReloadListener<RuleManager.Loaded> {
    public record Loaded(CompiledRuleSet rules, int rejected, int inactive) { }
    private static final RuleManager INSTANCE = new RuleManager();
    private static final String DIRECTORY = "crosshair_rules/";
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger("buildup_situational_crosshair/rules");
    private volatile Loaded loaded = new Loaded(CompiledRuleSet.EMPTY, 0, 0);
    private volatile long generation;
    private RuleManager() { }
    public static void register() {
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                Identifier.fromNamespaceAndPath("buildup_situational_crosshair", "rules"), INSTANCE);
    }
    public static CompiledRuleSet current() { return INSTANCE.loaded.rules(); }
    public static Loaded status() { return INSTANCE.loaded; }
    public static long generation() { return INSTANCE.generation; }

    @Override protected Loaded prepare(ResourceManager resources, ProfilerFiller profiler) {
        var rules = new ArrayList<Rule>();
        int rejected = 0;
        int inactive = 0;
        // listResources selects the highest-priority pack for each resource path.
        var entries = resources.listResources("crosshair_rules", id -> id.getPath().endsWith(".json"))
                .entrySet().stream().sorted(Comparator.comparing(e -> e.getKey().toString())).toList();
        for (var entry : entries) {
            var path = entry.getKey();
            String id = path.getNamespace() + ":" + path.getPath().substring(DIRECTORY.length(), path.getPath().length() - 5);
            try (var reader = entry.getValue().openAsReader()) {
                // Bound one malformed/oversized file without aborting other pack rules.
                var text = new StringBuilder();
                var buffer = new char[4096];
                int count;
                while ((count = reader.read(buffer)) != -1) {
                    if (text.length() + count > 1_048_576) throw new IllegalArgumentException("rule exceeds 1 Mi characters");
                    text.append(buffer, 0, count);
                }
                var rule = RuleParser.parse(id, new StringReader(text.toString()));
                if (!rule.requiredMods().stream().allMatch(FabricLoader.getInstance()::isModLoaded)) { inactive++; continue; }
                rules.add(rule);
            } catch (Exception ex) {
                rejected++;
                LOG.warn("Rejected crosshair rule {} (pack {}): {}", id, entry.getValue().sourcePackId(), ex.getMessage());
            }
        }
        return new Loaded(new CompiledRuleSet(rules, FabricLoader.getInstance()::isModLoaded), rejected, inactive);
    }
    @Override protected void apply(Loaded prepared, ResourceManager resources, ProfilerFiller profiler) {
        loaded = prepared;
        generation++;
        LOG.info("Loaded {} crosshair rules ({} inactive, {} rejected), generation {}",
                prepared.rules().size(), prepared.inactive(), prepared.rejected(), generation);
    }
}
