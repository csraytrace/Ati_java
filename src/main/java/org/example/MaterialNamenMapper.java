package org.example;

import java.util.HashMap;
import java.util.Map;

public class MaterialNamenMapper {
    private static final Map<String, String> materialMap = new HashMap<>();
    static {
        // alles lowercase!
        materialMap.put("air",  "78 N 0.001225 + 20.94 O 0.001225 + 0.93 Ar 0.001225");
        materialMap.put("luft", "78 N 0.001225 + 20.94 O 0.001225 + 0.93 Ar 0.001225");
        // beliebig weitere Namen und Rezepte ergänzen...
        // materialMap.put("wasser", "2 H 1.0 + 1 O 1.0");
    }

    public static String mapName(String name) {
        if (name == null) return null;
        String lower = name.trim().toLowerCase();
        if (materialMap.containsKey(lower)) {
            return materialMap.get(lower);
        }
        // Sonst gib einfach den Namen zurück (damit Standard-parseVerbindung klappt)
        return name;
    }
}
