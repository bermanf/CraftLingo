package dev.minehelper.language;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

class LocalizationTest {
    private static JsonObject locale(String name) throws Exception {
        try (var stream = LocalizationTest.class.getResourceAsStream("/assets/minehelper/lang/" + name + ".json")) {
            assertNotNull(stream);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
    @Test void englishFallbackAndRussianHaveSameKeysAndFormatParameters() throws Exception {
        JsonObject english = locale("en_us"), russian = locale("ru_ru");
        assertEquals(english.keySet(), russian.keySet());
        Pattern format = Pattern.compile("%s");
        for (String key : english.keySet()) {
            assertFalse(english.get(key).getAsString().isBlank(), key);
            assertFalse(russian.get(key).getAsString().isBlank(), key);
            assertEquals(format.matcher(english.get(key).getAsString()).results().count(),
                    format.matcher(russian.get(key).getAsString()).results().count(), key);
        }
        assertTrue(english.has("minehelper.settings.language"));
        assertTrue(english.has("minehelper.error.no_key"));
    }
}
