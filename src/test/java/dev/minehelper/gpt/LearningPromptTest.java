package dev.minehelper.gpt;

import dev.minehelper.language.ExplanationLanguage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class LearningPromptTest {
    @ParameterizedTest
    @EnumSource(value = ExplanationLanguage.class, names = "AUTO", mode = EnumSource.Mode.EXCLUDE)
    void selectedLanguageControlsEveryRequest(ExplanationLanguage language) {
        var payload = GptClient.payload("gpt-4.1-mini", 500, language, "se acabaron",
                "Se acabaron los suministros.",
                List.of(new GptClient.Turn("user", "Explain"), new GptClient.Turn("assistant", "Previous reply in another language")),
                "Pourquoi ?");
        String instructions = payload.get("instructions").getAsString();
        assertTrue(instructions.contains("Response language: " + language.englishName() + " (" + language.code() + ")"));
        assertTrue(instructions.contains("even if the question or previous replies use another language"));
        assertTrue(instructions.contains("Detect the language of the selected text"));
        assertFalse(instructions.contains("Russian-speaking"));
        assertFalse(instructions.contains("learn English"));
        assertTrue(payload.getAsJsonArray("input").get(0).toString().contains("Se acabaron"));
    }
    @Test void allBuiltInActionsUseConfiguredLanguageRatherThanRussian() {
        assertTrue(LearningPrompt.TRANSLATE.contains("configured response language"));
        for (String action : List.of(LearningPrompt.EXPLAIN, LearningPrompt.TRANSLATE, LearningPrompt.GRAMMAR)) {
            assertFalse(action.contains("Russian"));
            assertFalse(action.contains("русск"));
        }
    }
    @Test void autoMustBeResolvedBeforeSendingToGpt() {
        assertThrows(IllegalArgumentException.class, () -> LearningPrompt.instructions(ExplanationLanguage.AUTO));
    }
}
