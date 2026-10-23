package dev.minehelper.gpt;

import com.google.gson.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AnswerCache {
    private final LinkedHashMap<String, String> entries = new LinkedHashMap<>();
    private final Path path;
    private static final int MAX_ENTRIES = 300;
    public AnswerCache(Path path) {
        this.path = path;
        try {
            if (Files.isRegularFile(path) && Files.size(path) < 4_000_000) {
                JsonObject data = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                for (Map.Entry<String, JsonElement> e : data.entrySet()) {
                    if (entries.size() >= MAX_ENTRIES) break;
                    if (e.getKey().matches("[0-9a-f]{64}") && e.getValue().isJsonPrimitive())
                        entries.put(e.getKey(), e.getValue().getAsString());
                }
            }
        } catch (IOException | RuntimeException ignored) {}
    }
    public static String key(String model, String languageCode, String selected, String context, String question) {
        try {
            JsonArray parts = new JsonArray();
            parts.add(LearningPrompt.VERSION); parts.add(model); parts.add(languageCode); parts.add(selected); parts.add(context); parts.add(question);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(parts.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    public String get(String key) { return entries.get(key); }
    public void put(String key, String text) {
        entries.put(key, text);
        while (entries.size() > MAX_ENTRIES) entries.pollFirstEntry();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, new Gson().toJson(entries), StandardCharsets.UTF_8);
        } catch (IOException ignored) { /* An unwritable cache must not discard a successful reply. */ }
    }
}
