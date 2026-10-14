package cz.veprek.zdravi

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

const val SLEEP_GOAL_MIN = 8 * 60

data class SleepEntry(val date: LocalDate, val minutes: Int)

data class GymEntry(
    val id: Long,
    val date: LocalDate,
    val exercise: String,
    val setsReps: String,
    val kg: String,
)

/** Rozparsovaná data – používá se při načtení ze souboru i při importu. */
private class Parsed(
    val sleep: List<SleepEntry>,
    val gym: List<GymEntry>,
    val hidden: List<String>,
    val split: Map<Int, String>,
)

/** Veškerá data appky, uložená lokálně jako JSON v interním úložišti. */
object Store {
    private var file: File? = null

    val sleep = mutableStateListOf<SleepEntry>()
    val gym = mutableStateListOf<GymEntry>()

    /** Split: den v týdnu (1 = pondělí … 7 = neděle) -> text. */
    val split = mutableStateMapOf<Int, String>()

    /** Názvy cviků (lowercase), které uživatel ručně skryl z našeptávání. */
    private val hidden = mutableStateListOf<String>()

    fun init(context: Context) {
        if (file != null) return
        val f = File(context.filesDir, "data.json")
        file = f
        if (!f.exists()) return
        try {
            applyParsed(parse(JSONObject(f.readText())))
        } catch (e: Exception) {
            // Poškozený soubor – uschovat kopii, ať jde data ručně zachránit
            f.copyTo(File(context.filesDir, "data.corrupt.json"), overwrite = true)
        }
    }

    // ---------- (De)serializace ----------

    private fun parse(root: JSONObject): Parsed {
        val s = mutableListOf<SleepEntry>()
        root.optJSONArray("sleep")?.let { a ->
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                s.add(SleepEntry(LocalDate.parse(o.getString("date")), o.getInt("min")))
            }
        }
        val g = mutableListOf<GymEntry>()
        root.optJSONArray("gym")?.let { a ->
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                g.add(
                    GymEntry(
                        id = o.getLong("id"),
                        date = LocalDate.parse(o.getString("date")),
                        exercise = o.getString("ex"),
                        setsReps = o.optString("sets", ""),
                        kg = o.optString("kg", ""),
                    )
                )
            }
        }
        val h = mutableListOf<String>()
        root.optJSONArray("hidden")?.let { a ->
            for (i in 0 until a.length()) h.add(a.getString(i))
        }
        val sp = mutableMapOf<Int, String>()
        root.optJSONObject("split")?.let { o ->
            for (day in 1..7) {
                val t = o.optString(day.toString(), "")
                if (t.isNotBlank()) sp[day] = t
            }
        }
        return Parsed(s, g, h, sp)
    }

    private fun applyParsed(p: Parsed) {
        sleep.clear()
        sleep.addAll(p.sleep.sortedByDescending { it.date })
        gym.clear()
        gym.addAll(p.gym)
        hidden.clear()
        hidden.addAll(p.hidden)
        split.clear()
        split.putAll(p.split)
    }

    private fun toJson(): JSONObject {
        val s = JSONArray()
        sleep.forEach { s.put(JSONObject().put("date", it.date.toString()).put("min", it.minutes)) }
        val g = JSONArray()
        gym.forEach {
            g.put(
                JSONObject()
                    .put("id", it.id)
                    .put("date", it.date.toString())
                    .put("ex", it.exercise)
                    .put("sets", it.setsReps)
                    .put("kg", it.kg)
            )
        }
        val h = JSONArray()
        hidden.forEach { h.put(it) }
        val sp = JSONObject()
        split.forEach { (day, text) -> sp.put(day.toString(), text) }
        return JSONObject()
            .put("version", 1)
            .put("sleep", s)
            .put("gym", g)
            .put("hidden", h)
            .put("split", sp)
    }

    private fun save() {
        val f = file ?: return
        // atomický zápis: tmp soubor + přejmenování
        val tmp = File(f.parentFile, "data.json.tmp")
        tmp.writeText(toJson().toString())
        tmp.renameTo(f)
    }

    // ---------- Export / import ----------

    fun exportJson(): String = toJson().toString(2)

    /** Nahradí všechna data obsahem zálohy. Vrací false, pokud soubor není platná záloha. */
    fun importJson(text: String): Boolean {
        val parsed = try {
            val root = JSONObject(text)
            if (!root.has("sleep") && !root.has("gym") && !root.has("split")) return false
            parse(root)
        } catch (e: Exception) {
            return false
        }
        applyParsed(parsed)
        save()
        return true
    }

    // ---------- Spánek ----------

    fun sleepFor(date: LocalDate): SleepEntry? = sleep.firstOrNull { it.date == date }

    fun setSleep(date: LocalDate, minutes: Int) {
        val updated = sleep.filter { it.date != date } + SleepEntry(date, minutes)
        sleep.clear()
        sleep.addAll(updated.sortedByDescending { it.date })
        save()
    }

    fun removeSleep(date: LocalDate) {
        sleep.removeAll { it.date == date }
        save()
    }

    // ---------- Gym ----------

    fun addGym(date: LocalDate, exercise: String, setsReps: String, kg: String) {
        val id = (gym.maxOfOrNull { it.id } ?: 0L) + 1L
        gym.add(GymEntry(id, date, exercise, setsReps, kg))
        hidden.remove(exercise.lowercase())
        save()
    }

    fun removeGym(id: Long) {
        gym.removeAll { it.id == id }
        save()
    }

    /** Našeptávané názvy = jen cviky, které existují v záznamech a nejsou skryté. */
    fun exerciseNames(): List<String> =
        gym.sortedByDescending { it.id }
            .map { it.exercise.trim() }
            .filter { it.isNotEmpty() && it.lowercase() !in hidden }
            .distinctBy { it.lowercase() }

    fun hideSuggestion(name: String) {
        val key = name.trim().lowercase()
        if (key !in hidden) hidden.add(key)
        save()
    }

    /** Poslední záznam daného cviku před zvoleným dnem. */
    fun lastBefore(exercise: String, date: LocalDate): GymEntry? =
        gym.filter { it.exercise.trim().equals(exercise.trim(), ignoreCase = true) && it.date.isBefore(date) }
            .maxWithOrNull(compareBy<GymEntry> { it.date }.thenBy { it.id })

    // ---------- Split ----------

    fun setSplit(day: Int, text: String) {
        if (text.isBlank()) split.remove(day) else split[day] = text
        save()
    }
}

/** (aktuální streak, nejdelší streak) dní s >= 8 h spánku. */
fun sleepStreaks(entries: List<SleepEntry>): Pair<Int, Int> {
    val good = entries.filter { it.minutes >= SLEEP_GOAL_MIN }.map { it.date }.toSet()

    // aktuální: počítá se od dneška, a pokud dnešek ještě není zapsaný, od včerejška
    var d = LocalDate.now()
    if (d !in good) d = d.minusDays(1)
    var current = 0
    while (d in good) {
        current++
        d = d.minusDays(1)
    }

    var best = 0
    var run = 0
    var prev: LocalDate? = null
    for (x in good.sorted()) {
        run = if (prev != null && prev.plusDays(1) == x) run + 1 else 1
        if (run > best) best = run
        prev = x
    }
    return current to best
}
