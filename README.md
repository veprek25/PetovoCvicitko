# PetovoCvicitko

Android appka (Kotlin + Jetpack Compose) pro zápis spánku a tréninků. Data jsou jen lokálně v telefonu (`data.json` v interním úložišti appky).

## Funkce

**Spánek**
- zápis hodin + minut k datu (datum = ráno, kdy ses probudil; šipkami ‹ › přepínáš den, klik na datum = dnes)
- graf 7 / 14 / 30 dní, čárkovaná čára = 8 h
- aktuální streak dní s ≥ 8 h, rekordní streak, průměr za 7 dní
- klik na záznam v historii ho načte k úpravě

**Gym**
- nahoře pole „Cvik“ – našeptává jen cviky, které už máš zapsané; smažeš-li všechny záznamy cviku, z nabídky zmizí (případně ho z nabídky odebereš křížkem)
- dole vlevo série × opakování (`3x15`, `2x10` …), vpravo kg
- u známého cviku ukáže, co jsi dal minule
- seznam předchozích tréninků

## Build & release

- **Push do `main`** → GitHub Actions sestaví podepsané APK a vydá ho jako Release `v1.0.<číslo běhu>`. Obtainium v telefonu si ho stáhne.
- **Pull request** → jen ověří, že se build sestaví.
- Podpisový klíč je jen v GitHub Secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

Lokálně: `./gradlew assembleDebug` (JDK 17 + Android SDK).
