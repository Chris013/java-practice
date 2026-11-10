package io.github.chris013.practice.task30;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Turns plain Java objects into a {@link JsonValue} tree, so that {@link Json} can print them.
 *
 * <p>The order of the checks in {@link #convert} matters. The first case that matches wins:
 * {@code JsonValue} before everything, enums before {@code CharSequence}, records before {@code Iterable} and
 * {@code Map}. A record that happens to implement {@code Iterable} is therefore written as an object of its
 * components, which is what a reader of the record expects.
 *
 * <p>Reflection is used only for records and arrays. Records give us their components through
 * {@link Class#getRecordComponents()}, which keeps declaration order and needs no naming convention.
 */
public final class JsonMapper {

    private JsonMapper() {}

    /**
     * Maps {@code value} to JSON.
     *
     * <p>A top-level {@code null} or {@link Optional#empty()} becomes {@link JsonNull}. Inside a record, an
     * {@code Optional.empty()} component is left out of the object, so it can be absent from the JSON.
     * Inside a list or array, the same value becomes {@code null}, because a list element cannot be left out.
     *
     * @throws IllegalArgumentException for NaN or infinite numbers, for maps with non-String keys, for
     *     unsupported types (the message names the class), and for a container that contains itself
     *     (identity, not equality, decides this: two equal lists are not a cycle)
     * @throws IllegalStateException if a record accessor throws; the exception is the cause
     */
    public static JsonValue toJson(Object value) {
        return convert(value, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    /**
     * @param path the containers on the current path from the root. It is an identity set, and an entry is
     *     removed again when its conversion finishes, so that the same list may appear twice side by side.
     */
    private static JsonValue convert(Object value, Set<Object> path) {
        return switch (value) {
            case null -> JsonNull.INSTANCE;
            case JsonValue json -> json;
            case Boolean bool -> bool ? JsonBoolean.TRUE : JsonBoolean.FALSE;
            case Integer integer -> new JsonNumber(BigDecimal.valueOf(integer));
            case Long lng -> new JsonNumber(BigDecimal.valueOf(lng));
            case Short shrt -> new JsonNumber(BigDecimal.valueOf(shrt));
            case Byte byt -> new JsonNumber(BigDecimal.valueOf(byt));
            case BigInteger big -> new JsonNumber(new BigDecimal(big));
            case BigDecimal decimal -> new JsonNumber(decimal);
            case Double dbl -> {
                checkFinite(dbl);
                yield new JsonNumber(BigDecimal.valueOf(dbl));
            }
            case Float flt -> {
                checkFinite(flt);
                // Float.toString gives the shortest text that reads back as this float (0.1f -> "0.1").
                // Widening to double first would print 0.10000000149011612.
                yield new JsonNumber(new BigDecimal(flt.toString()));
            }
            case Character character -> new JsonString(String.valueOf(character));
            case Enum<?> constant -> new JsonString(constant.name());
            case CharSequence text -> new JsonString(text.toString());
            case Optional<?> optional -> optional.isPresent() ? convert(optional.get(), path) : JsonNull.INSTANCE;
            case TemporalAccessor time when isJavaTime(time) -> new JsonString(time.toString());
            case Record record -> fromRecord(record, path);
            case Map<?, ?> map -> fromMap(map, path);
            case Iterable<?> items -> fromIterable(items, path);
            default -> fromArrayOrReject(value, path);
        };
    }

    private static void checkFinite(double number) {
        if (Double.isNaN(number) || Double.isInfinite(number)) {
            throw new IllegalArgumentException("JSON has no number for " + number);
        }
    }

    private static boolean isJavaTime(TemporalAccessor value) {
        String packageName = value.getClass().getPackageName();
        return packageName.equals("java.time") || packageName.startsWith("java.time.");
    }

    private static JsonValue fromRecord(Record record, Set<Object> path) {
        enter(record, path);
        try {
            var members = new LinkedHashMap<String, JsonValue>();
            for (RecordComponent component : record.getClass().getRecordComponents()) {
                Object componentValue = readComponent(record, component);
                if (componentValue instanceof Optional<?> optional && optional.isEmpty()) {
                    continue;
                }
                String name = jsonName(component);
                if (members.put(name, convert(componentValue, path)) != null) {
                    throw new IllegalArgumentException("duplicate JSON name: " + name);
                }
            }
            return new JsonObject(members);
        } finally {
            path.remove(record);
        }
    }

    /**
     * Reads a component through its public accessor. A failing accessor is reported as an
     * {@link IllegalStateException} whose cause is the exception the accessor threw, so the original stack
     * trace is kept.
     */
    private static Object readComponent(Record record, RecordComponent component) {
        try {
            return component.getAccessor().invoke(record);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("cannot read " + component.getName() + " of " + typeName(record), e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(
                    "accessor " + component.getName() + " of " + typeName(record) + " threw", e.getCause());
        }
    }

    private static String jsonName(RecordComponent component) {
        JsonName annotation = component.getAnnotation(JsonName.class);
        return annotation == null ? component.getName() : annotation.value();
    }

    private static JsonValue fromMap(Map<?, ?> map, Set<Object> path) {
        enter(map, path);
        try {
            var members = new LinkedHashMap<String, JsonValue>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!(entry.getKey() instanceof String key)) {
                    throw new IllegalArgumentException("map keys must be String, not " + typeName(entry.getKey()));
                }
                members.put(key, convert(entry.getValue(), path));
            }
            return new JsonObject(members);
        } finally {
            path.remove(map);
        }
    }

    private static JsonValue fromIterable(Iterable<?> items, Set<Object> path) {
        enter(items, path);
        try {
            var elements = new ArrayList<JsonValue>();
            for (Object item : items) {
                elements.add(convert(item, path));
            }
            return new JsonArray(elements);
        } finally {
            path.remove(items);
        }
    }

    private static JsonValue fromArrayOrReject(Object value, Set<Object> path) {
        if (!value.getClass().isArray()) {
            throw new IllegalArgumentException("unsupported type: " + value.getClass().getName());
        }
        // java.lang.reflect.Array works for int[] and friends as well as for Object[]; it boxes primitives.
        enter(value, path);
        try {
            int length = Array.getLength(value);
            var elements = new ArrayList<JsonValue>(length);
            for (int i = 0; i < length; i++) {
                elements.add(convert(Array.get(value, i), path));
            }
            return new JsonArray(elements);
        } finally {
            path.remove(value);
        }
    }

    private static void enter(Object container, Set<Object> path) {
        if (!path.add(container)) {
            throw new IllegalArgumentException("cycle detected");
        }
    }

    private static String typeName(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }
}
