package cz.veprek.zdravi

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
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

/** Veškerá data appky, uložená lokálně jako JSON v interním úložišti. */
object Store {
    private var file: File? = null

    val sleep = mutableStateListOf<SleepEntry>()
    val gym = mutableStateListOf<GymEntry>()

    /** Názvy cviků (lowercase), které uživatel ručně skryl z našeptávání. */
    private val hidden = mutableStateListOf<String>()

    fun init(context: Context) {
        if (file != null) return
        val f = File(context.filesDir, "data.json")
        file = f
        if (!f.exists()) return
        try {
            val root = JSONObject(f.readText())
            root.optJSONArray("sleep")?.let { a ->
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    sleep.add(SleepEntry(LocalDate.parse(o.getString("date")), o.getInt("min")))
                }
            }
            root.optJSONArray("gym")?.let { a ->
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    gym.add(
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
            root.optJSONArray("hidden")?.let { a ->
                for (i in 0 until a.length()) hidden.add(a.getString(i))
            }
            sortSleep()
        } catch (e: Exception) {
            // Poškozený soubor – nepřepisovat, ať jde data ručně zachránit
            f.copyTo(File(context.filesDir, "data.corrupt.json"), overwrite = true)
        }
    }

    private fun save() {
        val f = file ?: return
        val root = JSONObject()
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
        root.put("sleep", s).put("gym", g).put("hidden", h)

        // atomický zápis: tmp soubor + přejmenování
        val tmp = File(f.parentFile, "data.json.tmp")
        tmp.writeText(root.toString())
        tmp.renameTo(f)
    }

    // ---------- Spánek ----------

    fun sleepFor(date: LocalDate): SleepEntry? = sleep.firstOrNull { it.date == date }

    fun setSleep(date: LocalDate, minutes: Int) {
        sleep.removeAll { it.date == date }
        sleep.add(SleepEntry(date, minutes))
        sortSleep()
        save()
    }

    private fun sortSleep() {
        val sorted = sleep.sortedByDescending { it.date }
        sleep.clear()
        sleep.addAll(sorted)
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
