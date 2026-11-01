package com.thinkfirst.coach;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 槽位是动态 JSON 对象，这里集中做类型安全的读取。 */
public final class CoachValues {

    public static final String EMPTY = "（待填写）";

    private CoachValues() {
    }

    public static String text(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value).replace("\r\n", "\n").replace('\r', '\n').trim();
    }

    public static String filled(Object value) {
        String text = text(value);
        return text.isEmpty() ? EMPTY : text;
    }

    public static String str(Map<String, Object> map, String key) {
        return text(map == null ? null : map.get(key));
    }

    public static List<String> strList(Map<String, Object> map, String key) {
        return asStringList(map == null ? null : map.get(key));
    }

    public static Map<String, Object> map(Map<String, Object> map, String key) {
        return asMap(map == null ? null : map.get(key));
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    public static List<String> asStringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> result = new ArrayList<>(list.size());
        for (Object item : list) {
            String text = text(item);
            if (!text.isEmpty()) {
                result.add(text);
            }
        }
        return result;
    }

    public static List<Map<String, Object>> asMapList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>(list.size());
        for (Object item : list) {
            result.add(asMap(item));
        }
        return result;
    }

    public static Map<String, Object> entryMap(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }

    public static String firstNonBlank(Object... values) {
        for (Object value : values) {
            String text = text(value);
            if (!text.isEmpty()) {
                return text;
            }
        }
        return "";
    }
}
