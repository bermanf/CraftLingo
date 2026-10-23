package dev.minehelper.gpt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class AnswerCacheTest {
    @TempDir Path temp;
    @Test void cacheUsesContextAndModelAndSurvivesRestart() {
        String key = AnswerCache.key("m", "ru", "run", "run out of", "explain");
        assertNotEquals(key, AnswerCache.key("m", "ru", "run", "run home", "explain"));
        assertNotEquals(key, AnswerCache.key("m2", "ru", "run", "run out of", "explain"));
        assertNotEquals(key, AnswerCache.key("m", "fr", "run", "run out of", "explain"));
        var cache = new AnswerCache(temp.resolve("answers.json"));
        cache.put(key, "заканчиваться");
        assertEquals("заканчиваться", new AnswerCache(temp.resolve("answers.json")).get(key));
    }
    @Test void corruptedCacheDoesNotCrash() throws Exception {
        Path path = temp.resolve("answers.json"); Files.writeString(path, "broken");
        assertNull(new AnswerCache(path).get("missing"));
    }
}
