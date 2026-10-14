# Zdraví

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

## Build

### GitHub Actions (bez Android Studia)
1. Vytvoř na GitHubu prázdný repozitář a pushni tam tuhle složku.
2. Actions → workflow **Build APK** se spustí sám (nebo ručně přes *Run workflow*).
3. Po doběhnutí stáhni artifact `zdravi-apk` (zip s APK) a nainstaluj na telefon.

### Lokálně
```
./gradlew assembleDebug
```
Potřebuje JDK 17 a Android SDK (`ANDROID_HOME` nebo `local.properties`). APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Pozn.
- APK je podepsané pevným klíčem `app/debug.keystore`, takže nové verze jdou instalovat přes starou bez ztráty dat. Klíč neměň, jinak musíš appku odinstalovat (= smazat data).
- Zálohu dat řeší Android Auto Backup (pokud ho máš zapnutý).
