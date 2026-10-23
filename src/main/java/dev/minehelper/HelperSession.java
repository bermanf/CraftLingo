package dev.minehelper;

import dev.minehelper.gpt.AnswerCache;
import dev.minehelper.gpt.GptClient;
import dev.minehelper.gpt.LearningPrompt;
import dev.minehelper.language.ExplanationLanguage;
import static dev.minehelper.ui.UiText.text;
import dev.minehelper.text.TextSelection;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class HelperSession {
    public static final String EXPLAIN = LearningPrompt.EXPLAIN;
    public static final String TRANSLATE = LearningPrompt.TRANSLATE;
    public TextSelection.Selection selection;
    public String answer = "";
    public String status = "";
    public boolean busy;
    public int scroll;
    private final HelperConfig config;
    private final GptClient client = new GptClient();
    private final AnswerCache cache;
    private final List<GptClient.Turn> history = new ArrayList<>();
    private CompletableFuture<GptClient.Reply> request;
    private long generation;
    private String discussionLanguage = "", discussionModel = "";

    public HelperSession(HelperConfig config) {
        this.config = config;
        cache = new AnswerCache(HelperConfig.directory().resolve("answers.json"));
    }
    public void select(TextSelection.Selection selected) {
        if (selected == null || selected.text().isBlank()) return;
        generation++;
        if (request != null) request.cancel(true);
        busy = false;
        selection = selected;
        history.clear();
        scroll = 0;
        config.showPanel = true;
        ask(EXPLAIN, true);
    }
    public void ask(String question, boolean fresh) {
        if (selection == null || question.isBlank()) return;
        ExplanationLanguage language = language();
        String model = config.model;
        if (!discussionLanguage.equals(language.code()) || !discussionModel.equals(model)) {
            invalidateDiscussion();
            discussionLanguage = language.code(); discussionModel = model;
        }
        if (busy) return;
        if (fresh) history.clear();
        scroll = 0;
        String q = TextSelection.limit(question.strip(), 1000);
        String cacheKey = AnswerCache.key(model, language.code(), selection.text(), selection.context(), q);
        String cached = config.cacheEnabled && history.isEmpty() ? cache.get(cacheKey) : null;
        if (cached != null) {
            answer = cached;
            status = text("status.cached");
            history.add(new GptClient.Turn("user", q));
            history.add(new GptClient.Turn("assistant", cached));
            return;
        }
        boolean cacheable = history.isEmpty();
        long token = ++generation;
        busy = true;
        answer = text("status.thinking");
        status = text("status.waiting");
        request = client.ask(config.apiKey(), model, config.maxOutputTokens, language,
                selection.text(), selection.context(), List.copyOf(history), q);
        request.whenComplete((reply, error) -> Minecraft.getInstance().execute(() -> {
            if (token != generation) return;
            if (language != language() || !model.equals(config.model)) { preferencesChanged(); return; }
            busy = false;
            scroll = 0;
            if (error != null) {
                Throwable cause = error;
                while (cause.getCause() != null) cause = cause.getCause();
                answer = cause instanceof GptClient.UserError userError ? text(userError.getMessage(), userError.arguments()) : text("error.request");
                status = text("status.failed");
                return;
            }
            answer = reply.text();
            status = reply.truncated() ? text("status.truncated") : text("status.tokens", reply.inputTokens(), reply.outputTokens());
            history.add(new GptClient.Turn("user", q));
            history.add(new GptClient.Turn("assistant", answer));
            List<GptClient.Turn> bounded = GptClient.boundedHistory(history);
            history.clear(); history.addAll(bounded);
            if (config.cacheEnabled && cacheable && !reply.truncated()) cache.put(cacheKey, answer);
        }));
    }
    public ExplanationLanguage language() {
        return ExplanationLanguage.resolve(config.explanationLanguage, Minecraft.getInstance().options.languageCode);
    }
    private void invalidateDiscussion() {
        generation++;
        if (request != null) request.cancel(true);
        busy = false; history.clear(); scroll = 0;
    }
    /** Invalidate in-flight replies without making an automatic paid request. */
    public void preferencesChanged() {
        invalidateDiscussion(); discussionLanguage = ""; discussionModel = "";
        answer = selection == null ? "" : text("status.preferences_changed");
        status = "";
    }
    public void clear() {
        invalidateDiscussion(); selection = null; discussionLanguage = ""; discussionModel = "";
        answer = ""; status = "";
    }
}
