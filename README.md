# Mine Helper

Learn languages while playing Minecraft. Hold a key, select a word or phrase in the live game interface, and ask GPT to explain it in your preferred language. The helper does not pause the world or replace the screen you are inspecting.

**Prototype 0.2.0 · Minecraft Java 1.21.1 · NeoForge 21.1.262 · Client only · MIT**

[Русская инструкция](README.ru.md) · [Validation](VALIDATION.md) · [Changes](CHANGELOG.md)

## Install

1. Close Minecraft and put `minehelper-neoforge-1.21.1-0.2.0.jar` in your profile's `mods` folder. Keep only one version of Mine Helper installed. No additional mod libraries are required.
2. Start Minecraft, enter a world, and press **F9**.
3. Enter your own [OpenAI API key](https://platform.openai.com/api-keys), choose a model and an explanation language, then **Save**. The default model is `gpt-4.1-mini`; you may choose another Responses-compatible model available to your account.
4. Hold **left Alt** and click a word, or drag across a phrase. Releasing the mouse button requests an explanation.
5. Release Alt to return mouse control to the game. The response panel remains visible.

This uses the OpenAI API directly. There is no embedded shared key, author-operated backend, or ChatGPT account login. API requests use your own account. Do not put your key in source code or share it in an issue.

## Choose your explanation language

In **F9 → Explanation language**, click to cycle forward or Shift-click to cycle backward, then Save:

| Code | Language | Code | Language |
|---|---|---|---|
| EN | English | RU | Русский |
| FR | Français | DE | Deutsch |
| ES | Español | PT | Português |
| IT | Italiano | UK | Українська |
| PL | Polski | TR | Türkçe |
| ZH | 简体中文 | JA | 日本語 |
| KO | 한국어 | AR | العربية |
| HI | हिन्दी | ID | Bahasa Indonesia |

**Auto** follows the Minecraft language, falling back to English for unsupported languages. An explicit selection overrides it. The default is Auto, including when reading an older configuration that has no language field.

The choice controls **explanations, translations, and follow-up answers**. GPT detects the language of the selected source text; you do not need a separate source-language setting. For example, play in English, choose FR, and receive explanations in French. If you want Russian explanations with an English game interface, explicitly choose RU.

Saving settings clears the current discussion and prevents a pending old-language response from being displayed. It does not automatically send a paid request: press Explain when ready. Cache entries include the resolved response language, model, context, and prompt version.

The **mod's UI language is separate**: its labels follow Minecraft, with English and Russian translations and English fallback for other locales. The 16-language selector controls GPT responses, not a complete 16-language UI localization.

## Controls

| Action | Default |
|---|---|
| Select text / interact with helper | Hold left Alt |
| Explain a word | Alt + click |
| Explain a phrase | Alt + drag |
| Show / hide panel | F8 |
| GPT settings | F9, or Mods → Mine Helper → Config |
| Scroll the response | Alt + wheel over panel |
| Follow up | Alt + click the question field, type, Enter |
| Cancel text input | Esc |

Bindings can be changed in Options → Controls → Key Binds → Mine Helper. During question input, the keyboard types into the helper while the world keeps ticking. The mod does not change whether an existing Minecraft screen itself pauses the game.

Item tooltips are pinned when you enter selection mode, so you can move the pointer from an item onto its tooltip text. Only that tooltip is held in place; the world and other interface elements remain live.

## Requests and local data

Uses the [OpenAI Responses API](https://developers.openai.com/api/docs/guides/text), `https://api.openai.com/v1/responses`, with `store: false`. Only the selected text, nearby textual context, and bounded discussion history are sent. Screenshots and world saves are not sent. Requests run asynchronously; there are no per-frame model calls. This version displays the response when generation completes rather than streaming it token by token.

Limits: selection 1200 characters, context 1600, question 1000; discussion history up to three complete question/answer pairs and 5000 characters. The default generation limit is 500 tokens; reasoning models may need a higher `maxOutputTokens` setting.

Files under your Minecraft profile:

```text
config/minehelper/settings.json   model, explanationLanguage, limits, cache preference
config/minehelper/openai.key      your local API key (plain text)
config/minehelper/answers.json    up to 300 cached answers
```

Example preference: `"explanationLanguage": "fr"`. Valid values are `auto` and the lower-case codes above. A process environment variable `OPENAI_API_KEY` takes priority over the local key file. Leaving the key field blank preserves an existing key. To remove the file key or clear the cache, close Minecraft and remove the corresponding file.

The panel reports token usage supplied by the API. Follow-up requests with history are not cached. Incomplete responses are not cached. Replacing or cancelling a local request cannot guarantee that the provider will stop or waive charges for a generation already submitted.

## Current limits

- Targets **NeoForge 1.21.1**. This JAR is not for Fabric, Forge, or another Minecraft version.
- Supports ordinary 2D text rendering used by chat, menus, books, tooltips, and compatible mod interfaces. It does not yet capture text embedded in images, rotated/custom renderers, or 3D world text such as signs. No OCR.
- Overlapping custom interfaces and multi-line grouping may require dedicated integrations. Selection of bidirectional / right-to-left source text is not yet validated. Selecting an Arabic response language does not imply full RTL source-selection support.
- The full IC2 modpack and every shader/resource-pack combination have not been tested. See the validation report for the actual scope.
- Prompt language selection is tested for all 16 choices, but response quality in each language has not been evaluated against the live OpenAI service without a user-provided key.
- No vocabulary trainer, pronunciation, or spaced repetition yet.

## Build and test

Requires **JDK 21** and network access for the initial dependency download:

```powershell
./gradlew.bat build
./gradlew.bat test
./gradlew.bat runClient
```

The release is written to `build/libs/minehelper-neoforge-1.21.1-0.2.0.jar`. Tests use a local HTTP server and do not spend OpenAI API tokens. The source and JAR do not include an API key.

## Contributing

The code is MIT licensed. No repository has been published automatically.

- Add a response language in `language/ExplanationLanguage.java`: a stable code, English prompt name, and native display name. Do not accept arbitrary free-form text as a language instruction.
- Change the general learning instructions in `gpt/LearningPrompt.java`. Built-in actions must refer to the configured response language rather than force a specific language. Bump the prompt version when behavior changes so old cached answers are not reused.
- Add UI translations in `src/main/resources/assets/minehelper/lang/`. English is the fallback and Russian is included. Keep translation keys and formatting placeholders consistent.
- Keep network work off the game thread and retain the live selection behavior. Add regression tests when changing language resolution, history, or caching.

Main modules: `text/` captures and selects text; `ui/` provides the overlay and settings; `HelperSession` manages requests/history; `gpt/` implements Responses API and caching. Based on the official [NeoForge 1.21.1 MDK](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle).
