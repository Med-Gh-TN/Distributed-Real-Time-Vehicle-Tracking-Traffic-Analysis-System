package com.traffic.gateway;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FIXED JSON UTILS (Java 8 Compatible, No External Libraries)
 * Fixes "package does not exist" and "cannot find symbol" errors.
 */
public class JsonUtils {

    public static Map<String, Object> parseJson(String json) {
        Map<String, Object> map = new HashMap<>();
        if (json == null || json.isEmpty()) return map;

        String clean = json.trim().replace("{", "").replace("}", "").replace("\"", "").replace("\n", "");
        String[] pairs = clean.split(",");

        for (String pair : pairs) {
            String[] parts = pair.split(":");
            if (parts.length == 2) {
                map.put(parts[0].trim(), parts[1].trim());
            }
        }
        return map;
    }

    public static String toJson(Object object) {
        if (object == null) return "{}";
        if (object instanceof List) {
            List<?> list = (List<?>) object;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                sb.append(toJson(list.get(i)));
                if (i < list.size() - 1) sb.append(",");
            }
            sb.append("]");
            return sb.toString();
        }
        if (object instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) object;
            StringBuilder sb = new StringBuilder("{");
            int i = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                sb.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
                if (i++ < map.size() - 1) sb.append(",");
            }
            sb.append("}");
            return sb.toString();
        }
        if (object instanceof String) return "\"" + object + "\"";
        return objectToJsonReflection(object);
    }

    private static String objectToJsonReflection(Object obj) {
        try {
            StringBuilder json = new StringBuilder("{");
            Field[] fields = obj.getClass().getDeclaredFields();
            int count = 0;
            for (Field field : fields) {
                field.setAccessible(true);
                if (field.getName().equals("serialVersionUID")) continue;
                if (count++ > 0) json.append(",");
                json.append("\"").append(field.getName()).append("\":");
                Object val = field.get(obj);
                if (val instanceof Number || val instanceof Boolean) json.append(val);
                else json.append("\"").append(val).append("\"");
            }
            json.append("}");
            return json.toString();
        } catch (Exception e) { return "{}"; }
    }
}