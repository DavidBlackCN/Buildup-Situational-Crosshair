package dev.buildup.situationalcrosshair.rules;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import dev.buildup.situationalcrosshair.semantic.*;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

/** Strict schema validation. A rejected field never silently becomes a default. */
public final class RuleParser {
    private RuleParser() { }
    public static Rule parse(String id, Reader input) throws IOException {
        identifier(id);
        var reader = new JsonReader(input);
        reader.setStrictness(Strictness.STRICT);
        var root = object(read(reader, 0), "rule");
        if (reader.peek() != JsonToken.END_DOCUMENT) throw bad("trailing JSON content");
        fields(root, "schema", "mode", "priority", "requires", "when", "emit");
        if (integer(required(root, "schema")) != 1) throw bad("schema must be 1");
        var mode = value(Rule.Mode.class, string(required(root, "mode")));
        int priority = root.has("priority") ? integer(root.get("priority")) : 0;
        if (priority < -100 || priority > 100) throw bad("priority outside [-100,100]");
        var requires = optionalObject(root, "requires");
        fields(requires, "mods");
        var mods = strings(requires, "mods", false);
        for (var mod : mods) if (!mod.matches("[a-z][a-z0-9_-]{1,63}")) throw bad("invalid mod ID: " + mod);
        var when = optionalObject(root, "when");
        fields(when, "target", "held", "player");
        var target = optionalObject(when, "target");
        fields(target, "type", "ids", "tags", "namespace", "properties");
        var type = target.has("type") ? value(TargetType.class, string(target.get("type"))) : null;
        var props = optionalObject(target, "properties");
        if (target.has("properties") && type != TargetType.BLOCK) throw bad("properties require target.type=block");
        var properties = new TreeMap<String, String>();
        for (var entry : props.entrySet()) {
            if (!entry.getKey().matches("[a-z0-9_]+")) throw bad("invalid block property name");
            properties.put(entry.getKey(), string(entry.getValue()));
        }
        if (type == TargetType.MISS && (target.has("ids") || target.has("tags") || target.has("namespace")))
            throw bad("miss target has no registry identity");
        var held = optionalObject(when, "held");
        fields(held, "hand", "ids", "tags", "namespace");
        var hand = held.has("hand") ? value(Rule.Hand.class, string(held.get("hand"))) : Rule.Hand.EITHER;
        var player = optionalObject(when, "player");
        fields(player, "sneaking", "creative", "using_item");
        var emit = required(root, "emit");
        if (!emit.isJsonArray() || emit.getAsJsonArray().isEmpty()) throw bad("emit must be a nonempty array");
        var emissions = new ArrayList<Rule.Emission>();
        for (var entry : emit.getAsJsonArray()) {
            var e = object(entry, "emit entry");
            fields(e, "slot", "action", "state");
            var slot = value(ActionSlot.class, string(required(e, "slot")));
            var actionName = string(required(e, "action"));
            var action = actionName.equals("*") ? null : value(ActionKind.class, actionName);
            if (action == null && mode != Rule.Mode.DENY) throw bad("wildcard action is deny-only");
            if (action != null && !action.supports(slot)) throw bad("illegal slot/action: " + slot + "/" + action);
            var state = e.has("state") ? value(ActionState.class, string(e.get("state")))
                    : mode == Rule.Mode.DENY ? null : ActionState.NORMAL;
            if (action == ActionKind.NONE && state != null && state != ActionState.NORMAL) throw bad("NONE requires NORMAL");
            emissions.add(new Rule.Emission(slot, action, state));
        }
        return new Rule(id, mode, priority, mods, selector(target), type, properties, selector(held), hand,
                bool(player, "sneaking"), bool(player, "creative"), bool(player, "using_item"), emissions);
    }

    private static Rule.Selector selector(JsonObject object) {
        String namespace = object.has("namespace") ? string(object.get("namespace")) : null;
        if (namespace != null && !namespace.matches("[a-z0-9_.-]+")) throw bad("invalid namespace");
        return new Rule.Selector(strings(object, "ids", true), strings(object, "tags", true), namespace);
    }
    private static Set<String> strings(JsonObject object, String field, boolean ids) {
        if (!object.has(field)) return Set.of();
        var array = object.get(field);
        if (!array.isJsonArray() || ids && array.getAsJsonArray().isEmpty())
            throw bad(field + " must be " + (ids ? "a nonempty array" : "an array"));
        var result = new TreeSet<String>();
        for (var e : array.getAsJsonArray()) { var s = string(e); if (ids) identifier(s); result.add(s); }
        return result;
    }
    private static void identifier(String id) {
        if (!id.matches("[a-z0-9_.-]+:[a-z0-9/._-]+")) throw bad("invalid namespaced ID: " + id);
    }
    private static Boolean bool(JsonObject object, String key) {
        if (!object.has(key)) return null;
        var e = object.get(key);
        if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isBoolean()) throw bad(key + " must be boolean");
        return e.getAsBoolean();
    }
    private static int integer(JsonElement e) {
        if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber() || !e.getAsString().matches("-?(0|[1-9][0-9]*)"))
            throw bad("expected integer");
        try { return Integer.parseInt(e.getAsString()); } catch (NumberFormatException ex) { throw bad("integer out of range"); }
    }
    private static String string(JsonElement e) {
        if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString()) throw bad("expected string");
        return e.getAsString();
    }
    private static <T extends Enum<T>> T value(Class<T> type, String s) {
        if (!s.equals(s.toLowerCase(Locale.ROOT))) throw bad("enum values must be lowercase: " + s);
        try { return Enum.valueOf(type, s.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) { throw bad("invalid " + type.getSimpleName() + ": " + s); }
    }
    private static JsonElement required(JsonObject object, String field) {
        if (!object.has(field)) throw bad("missing " + field);
        return object.get(field);
    }
    private static JsonObject optionalObject(JsonObject object, String field) {
        return object.has(field) ? object(object.get(field), field) : new JsonObject();
    }
    private static JsonObject object(JsonElement e, String name) {
        if (!e.isJsonObject()) throw bad(name + " must be an object");
        return e.getAsJsonObject();
    }
    private static void fields(JsonObject object, String... allowed) {
        var names = Set.of(allowed);
        for (var key : object.keySet()) if (!names.contains(key)) throw bad("unknown field: " + key);
    }
    private static IllegalArgumentException bad(String message) { return new IllegalArgumentException(message); }

    // JsonParser accepts duplicate object members by replacing the earlier value. Reject them instead.
    private static JsonElement read(JsonReader reader, int depth) throws IOException {
        if (depth > 16) throw bad("JSON nesting exceeds 16");
        return switch (reader.peek()) {
            case BEGIN_OBJECT -> {
                var object = new JsonObject(); reader.beginObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (object.has(name)) throw bad("duplicate field: " + name);
                    object.add(name, read(reader, depth + 1));
                }
                reader.endObject(); yield object;
            }
            case BEGIN_ARRAY -> {
                var array = new JsonArray(); reader.beginArray();
                while (reader.hasNext()) array.add(read(reader, depth + 1));
                reader.endArray(); yield array;
            }
            case STRING -> new JsonPrimitive(reader.nextString());
            case NUMBER -> new JsonPrimitive(new java.math.BigDecimal(reader.nextString()));
            case BOOLEAN -> new JsonPrimitive(reader.nextBoolean());
            case NULL -> { reader.nextNull(); yield JsonNull.INSTANCE; }
            default -> throw bad("unexpected JSON token");
        };
    }
}
