# Changelog

## 0.2.0

- Add an explanation-language selector in F9: 16 explicit languages plus Auto, saved in `explanationLanguage`.
- Generalize the GPT prompt to language learning; detect the source language and use the chosen response language for explanation, translation, grammar, and follow-ups.
- Include response language and prompt version in cache keys. Old 0.1.0 answers are not reused under the new prompt.
- Reset discussion history and invalidate pending responses when saving settings. No automatic request is made on settings changes.
- Move UI labels and error messages to English/Russian locale resources with English fallback.
- Keep candidate language and unsaved input through screen resize. Back cancels unsaved selection.
- Add an English README, retain a Russian guide, and document how to add languages and translations.
- Expand automatic coverage to 37 tests and verify language selection, saving, cancellation, and small-screen layout in Minecraft.

Migration: an old configuration without `explanationLanguage` uses **Auto**, which follows the Minecraft language with English fallback. Choose RU explicitly if you previously expected Russian responses while playing with English UI. Keep only one version of the mod JAR installed.

## 0.1.0

- Initial live text-selection overlay for NeoForge 1.21.1.
- GPT explanations, translations, grammar, and follow-ups in Russian.
- Local answer cache, asynchronous API access, and tooltip pinning.
