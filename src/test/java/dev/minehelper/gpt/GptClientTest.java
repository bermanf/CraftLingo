package dev.minehelper.gpt;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.minehelper.language.ExplanationLanguage;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class GptClientTest {
    private static final String REPLY = """
            {"status":"completed","output":[{"type":"reasoning","summary":[]},
            {"type":"message","content":[{"type":"output_text","text":"Кирка — инструмент."}]}],
            "usage":{"input_tokens":190,"output_tokens":15}}
            """;
    @Test void realHttpProtocolSendsOnlyBoundedTextAndParsesResponse() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<JsonObject> sent = new AtomicReference<>();
        AtomicReference<String> auth = new AtomicReference<>();
        server.createContext("/v1/responses", exchange -> {
            sent.set(JsonParser.parseString(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject());
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] reply = REPLY.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, reply.length); exchange.getResponseBody().write(reply); exchange.close();
        });
        server.start();
        try {
            var client = new GptClient(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/responses"));
            var reply = client.ask("test-only", "gpt-4.1-mini", 500, ExplanationLanguage.FR, "Pickaxe", "Diamond Pickaxe", List.of(), LearningPrompt.EXPLAIN)
                    .get(5, TimeUnit.SECONDS);
            assertEquals("Кирка — инструмент.", reply.text());
            assertEquals(190, reply.inputTokens()); assertEquals(15, reply.outputTokens());
            assertFalse(sent.get().get("store").getAsBoolean());
            assertEquals("Bearer test-only", auth.get());
            assertTrue(sent.get().getAsJsonArray("input").toString().contains("Diamond Pickaxe"));
            assertFalse(sent.get().toString().contains("input_image"));
            assertFalse(sent.get().toString().contains("test-only"));
            assertTrue(sent.get().get("instructions").getAsString().contains("Response language: French (fr)"));
        } finally { server.stop(0); }
    }
    @Test void noKeyProducesUsefulErrorWithoutNetwork() {
        var client = new GptClient(URI.create("http://127.0.0.1:1"));
        var error = assertThrows(Exception.class, () -> client.ask("", "gpt-4.1-mini", 500, ExplanationLanguage.EN, "a", "a", List.of(), "explain").join());
        assertEquals("error.no_key", error.getCause().getMessage());
    }
    @Test void responseParserHandlesRefusalMalformedAndTruncation() {
        assertEquals("Нет ответа", GptClient.parse("{\"output\":[{\"type\":\"message\",\"content\":[{\"type\":\"refusal\",\"refusal\":\"Нет ответа\"}]}]}").text());
        assertThrows(GptClient.UserError.class, () -> GptClient.parse("not json"));
        assertThrows(GptClient.UserError.class, () -> GptClient.parse("{\"status\":\"incomplete\",\"output\":[]}"));
        assertTrue(GptClient.parse(REPLY.replace("completed", "incomplete")).truncated());
        assertFalse(GptClient.parse(REPLY).truncated());
    }
    @Test void historyIsLimitedToThreeWholePairs() {
        List<GptClient.Turn> history = new ArrayList<>();
        for (int i = 0; i < 20; i++) { history.add(new GptClient.Turn("user", "q" + i)); history.add(new GptClient.Turn("assistant", "a" + i)); }
        var bounded = GptClient.boundedHistory(history);
        assertEquals(6, bounded.size()); assertEquals("q17", bounded.getFirst().text());
        assertEquals("a19", bounded.getLast().text());
    }
    @Test void payloadLimitsSelectedTextAndContext() {
        var body = GptClient.payload("gpt-4.1-mini", 500, ExplanationLanguage.EN, "a".repeat(3000), "b".repeat(5000), List.of(), "q".repeat(3000));
        assertTrue(body.toString().length() < 5000);
        assertEquals(1000, body.getAsJsonArray("input").get(1).getAsJsonObject().get("content").getAsString().length());
    }
    @Test void errorsDoNotExposeServicePayloadsOrSecrets() {
        assertEquals("error.unauthorized", GptClient.safeError(401));
        assertEquals("error.rate_limit", GptClient.safeError(429));
    }
}
