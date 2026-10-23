package dev.minehelper.gpt;

import com.google.gson.*;
import dev.minehelper.text.TextSelection;
import dev.minehelper.language.ExplanationLanguage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** No Minecraft calls on HTTP threads; caller marshals completion to the game thread. */
public final class GptClient {
    public static final URI OPENAI = URI.create("https://api.openai.com/v1/responses");
    private volatile HttpClient http;
    private final URI endpoint;
    public record Turn(String role, String text) {}
    public record Reply(String text, int inputTokens, int outputTokens, boolean truncated) {}

    public GptClient() { this(OPENAI); }
    // Package-private endpoint override is for a local protocol test, never exposed in user config.
    GptClient(URI endpoint) {
        this.endpoint = endpoint;
    }
    private synchronized HttpClient http() {
        if (http == null) http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(12))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        return http;
    }
    public CompletableFuture<Reply> ask(String key, String model, int maxTokens,
                                       ExplanationLanguage language, String selected, String context, List<Turn> history, String question) {
        if (key == null || key.isBlank()) return CompletableFuture.failedFuture(
                new UserError("error.no_key"));
        JsonObject body = payload(model, maxTokens, language, selected, context, history, question);
        return CompletableFuture.supplyAsync(() -> {
            HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(55))
                    .header("Authorization", "Bearer " + key.trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
            return http().sendAsync(request, HttpResponse.BodyHandlers.ofString());
        }).thenCompose(future -> future).handle((response, error) -> {
            if (error != null) throw new UserError("error.network");
            if (response.statusCode() != 200) throw new UserError(safeError(response.statusCode()), response.statusCode());
            return parse(response.body());
        });
    }
    static JsonObject payload(String model, int maxTokens, ExplanationLanguage language, String selected, String context,
                              List<Turn> history, String question) {
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("store", false);
        body.addProperty("max_output_tokens", maxTokens);
        body.addProperty("instructions", LearningPrompt.instructions(language));
        JsonArray input = new JsonArray();
        JsonObject quote = new JsonObject();
        quote.addProperty("selected", TextSelection.limit(selected, 1200));
        quote.addProperty("context", TextSelection.limit(context, 1600));
        input.add(message("user", "Analyze this quoted game text: " + quote));
        List<Turn> bounded = boundedHistory(history);
        for (Turn turn : bounded) input.add(message(turn.role, turn.text));
        input.add(message("user", TextSelection.limit(question, 1000)));
        body.add("input", input);
        return body;
    }
    public static List<Turn> boundedHistory(List<Turn> history) {
        List<Turn> bounded = new ArrayList<>();
        int chars = 0;
        // Keep whole question/answer pairs, newest first, then restore chronological order.
        for (int i = history.size() - 2; i >= 0 && bounded.size() < 6; i -= 2) {
            Turn user = history.get(i), assistant = history.get(i + 1);
            if (!user.role.equals("user") || !assistant.role.equals("assistant")) continue;
            if (chars + user.text.length() + assistant.text.length() > 5000) break;
            bounded.addFirst(assistant);
            bounded.addFirst(user);
            chars += user.text.length() + assistant.text.length();
        }
        return List.copyOf(bounded);
    }
    private static JsonObject message(String role, String text) {
        JsonObject m = new JsonObject(); m.addProperty("role", role); m.addProperty("content", text); return m;
    }
    static Reply parse(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            StringBuilder text = new StringBuilder();
            if (root.has("output")) for (JsonElement item : root.getAsJsonArray("output")) {
                JsonObject out = item.getAsJsonObject();
                if (!"message".equals(string(out, "type")) || !out.has("content")) continue;
                for (JsonElement part : out.getAsJsonArray("content")) {
                    JsonObject p = part.getAsJsonObject();
                    if ("output_text".equals(string(p, "type"))) text.append(string(p, "text"));
                    if ("refusal".equals(string(p, "type"))) text.append(string(p, "refusal"));
                }
            }
            if (text.isEmpty()) {
                if ("incomplete".equals(string(root, "status")))
                    throw new UserError("error.token_limit");
                throw new UserError("error.empty_response");
            }
            JsonObject usage = root.has("usage") && root.get("usage").isJsonObject()
                    ? root.getAsJsonObject("usage") : new JsonObject();
            return new Reply(TextSelection.limit(text.toString(), 10000), integer(usage, "input_tokens"), integer(usage, "output_tokens"),
                    "incomplete".equals(string(root, "status")));
        } catch (UserError e) { throw e; }
        catch (RuntimeException e) { throw new UserError("error.invalid_response"); }
    }
    private static String string(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : ""; }
    private static int integer(JsonObject o, String k) { return o.has(k) ? o.get(k).getAsInt() : 0; }
    static String safeError(int code) {
        return switch(code) {
            case 401 -> "error.unauthorized";
            case 403 -> "error.forbidden";
            case 404 -> "error.model_unavailable";
            case 429 -> "error.rate_limit";
            case 400 -> "error.parameters";
            default -> "error.http";
        };
    }
    public static final class UserError extends RuntimeException {
        private final Object[] arguments;
        public UserError(String key, Object... arguments) { super(key); this.arguments = arguments.clone(); }
        public Object[] arguments() { return arguments.clone(); }
    }
}
