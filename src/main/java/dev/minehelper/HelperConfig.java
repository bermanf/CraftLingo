package dev.minehelper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;
import dev.minehelper.language.ExplanationLanguage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class HelperConfig {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    public String model = "gpt-4.1-mini";
    public int maxOutputTokens = 500;
    public boolean showPanel = true;
    public boolean cacheEnabled = true;
    public String explanationLanguage = "auto";

    public static Path directory() { return FMLPaths.CONFIGDIR.get().resolve("minehelper"); }
    public static HelperConfig load() {
        Path path = directory().resolve("settings.json");
        try {
            if (Files.isRegularFile(path)) {
                HelperConfig config = JSON.fromJson(Files.readString(path), HelperConfig.class);
                if (config != null) { config.validate(); return config; }
            }
        } catch (IOException | RuntimeException ignored) {
            // A broken user config must not prevent Minecraft from starting.
        }
        return new HelperConfig();
    }
    public void validate() {
        if (model == null || !model.matches("[A-Za-z0-9._:-]{1,120}")) model = "gpt-4.1-mini";
        maxOutputTokens = Math.clamp(maxOutputTokens, 128, 2000);
        explanationLanguage = ExplanationLanguage.fromCode(explanationLanguage).code();
    }
    public void save() throws IOException {
        validate();
        Files.createDirectories(directory());
        Files.writeString(directory().resolve("settings.json"), JSON.toJson(this), StandardCharsets.UTF_8);
    }
    public String apiKey() {
        String env = System.getenv("OPENAI_API_KEY");
        if (env != null && !env.isBlank()) return env.trim();
        try { return Files.readString(directory().resolve("openai.key")).trim(); }
        catch (IOException ignored) { return ""; }
    }
    public void saveKey(String key) throws IOException {
        Files.createDirectories(directory());
        if (key == null || key.isBlank()) Files.deleteIfExists(directory().resolve("openai.key"));
        else Files.writeString(directory().resolve("openai.key"), key.trim(), StandardCharsets.UTF_8);
    }
}
