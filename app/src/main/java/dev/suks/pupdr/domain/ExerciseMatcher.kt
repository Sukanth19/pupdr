package dev.suks.pupdr.domain

import dev.suks.pupdr.data.Exercise
import kotlin.math.abs

/** Text normalisation shared by matching and alias lookup. */
internal object Norm {
    private val abbreviations = mapOf(
        "db" to "dumbbell", "dbs" to "dumbbell", "bb" to "barbell", "kb" to "kettlebell", "kbs" to "kettlebell",
        "bw" to "bodyweight", "ohp" to "overhead press", "rdl" to "romanian deadlift", "rdls" to "romanian deadlift",
        "sldl" to "stiff legged deadlift", "dl" to "deadlift", "dls" to "deadlift", "ext" to "extension",
        "exts" to "extension", "tri" to "tricep", "tris" to "tricep", "bi" to "bicep", "bis" to "bicep",
        "inc" to "incline", "dec" to "decline", "sq" to "squat", "bp" to "bench press", "ghr" to "glute ham raise",
        "bss" to "bulgarian split squat", "flye" to "fly", "flyes" to "fly", "flys" to "fly", "flies" to "fly",
    )
    private val stop = setOf("the", "a", "an", "with", "on", "of", "and", "to", "in", "for", "using", "exercise", "exercises", "movement")

    private fun stem(t: String): String = when {
        t.length > 4 && t.endsWith("ies") -> t.dropLast(3) + "y"
        t.length > 4 && t.endsWith("es") && (t.dropLast(2).endsWith("ss") || t.dropLast(2).endsWith("sh") || t.dropLast(2).endsWith("ch") || t.dropLast(2).endsWith("x")) -> t.dropLast(2)
        t.length > 2 && t.endsWith("s") && !t.endsWith("ss") && !t.endsWith("us") -> t.dropLast(1)
        else -> t
    }

    fun tokens(s: String): List<String> =
        s.lowercase().replace("&", " and ").replace("'", "").replace("’", "")
            .split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }
            .flatMap { (abbreviations[it] ?: it).split(' ') }
            .map { abbreviations[stem(it)] ?: stem(it) }
            .filter { it !in stop }

    fun squash(s: String) = tokens(s).joinToString("")
}

/** Common gym names → the library exercise most people mean. */
internal object Aliases {
    private val raw = mapOf(
        "Barbell Bench Press - Medium Grip" to "bench press,bench,flat bench,flat bench press,barbell bench,barbell bench press,bp",
        "Barbell Incline Bench Press - Medium Grip" to "incline bench,incline bench press,incline barbell press,incline barbell bench press",
        "Incline Dumbbell Press" to "incline dumbbell press,incline dumbbell bench press",
        "Dumbbell Bench Press" to "dumbbell press,dumbbell bench,dumbbell bench press",
        "Close-Grip Barbell Bench Press" to "close grip bench,close grip bench press,cgbp",
        "Barbell Squat" to "squat,back squat,barbell back squat",
        "Front Barbell Squat" to "front squat",
        "Goblet Squat" to "goblet squat",
        "Hack Squat" to "hack squat",
        "Split Squat with Dumbbells" to "split squat,bulgarian split squat,bulgarian",
        "Barbell Deadlift" to "deadlift,conventional deadlift",
        "Sumo Deadlift" to "sumo deadlift",
        "Romanian Deadlift" to "romanian deadlift,stiff leg deadlift,stiff legged deadlift",
        "Standing Military Press" to "overhead press,shoulder press,military press,barbell overhead press,strict press,barbell shoulder press",
        "Dumbbell Shoulder Press" to "dumbbell shoulder press,seated dumbbell press,dumbbell overhead press",
        "Arnold Dumbbell Press" to "arnold press",
        "Push Press" to "push press",
        "Pullups" to "pull up,pullup,wide grip pull up,pull-up",
        "Chin-Up" to "chin up,chinup",
        "Pushups" to "push up,pushup,press up",
        "Wide-Grip Lat Pulldown" to "lat pulldown,pulldown,lat pull down,lat pulldowns,wide grip lat pulldown",
        "Bent Over Barbell Row" to "barbell row,bent over row,bent over barbell row,pendlay row,row",
        "One-Arm Dumbbell Row" to "dumbbell row,one arm dumbbell row,single arm row,single arm dumbbell row",
        "Seated Cable Rows" to "cable row,seated row,seated cable row,low row",
        "T-Bar Row with Handle" to "t bar row,tbar row",
        "Upright Barbell Row" to "upright row",
        "Face Pull" to "face pull",
        "Barbell Curl" to "curl,bicep curl,barbell curl,barbell bicep curl",
        "Dumbbell Bicep Curl" to "dumbbell curl,dumbbell bicep curl",
        "Hammer Curls" to "hammer curl",
        "Preacher Curl" to "preacher curl",
        "Triceps Pushdown" to "tricep pushdown,pushdown,cable pushdown,rope pushdown,tricep extension,cable tricep extension",
        "EZ-Bar Skullcrusher" to "skull crusher,skullcrusher,lying tricep extension",
        "Parallel Bar Dip" to "dip,parallel bar dip,chest dip",
        "Bench Dips" to "tricep dip,bench dip",
        "Side Lateral Raise" to "lateral raise,side raise,side lateral raise,lat raise,dumbbell lateral raise",
        "Reverse Flyes" to "rear delt fly,reverse fly,rear delt raise",
        "Dumbbell Flyes" to "chest fly,dumbbell fly,pec fly,flat fly",
        "Butterfly" to "pec deck,machine fly,pec dec",
        "Cable Crossover" to "cable fly,cable crossover",
        "Leg Press" to "leg press",
        "Leg Extensions" to "leg extension,quad extension",
        "Lying Leg Curls" to "leg curl,hamstring curl,lying leg curl",
        "Seated Leg Curl" to "seated leg curl",
        "Standing Calf Raises" to "calf raise,standing calf raise,calve,calf",
        "Seated Calf Raise" to "seated calf raise",
        "Barbell Hip Thrust" to "hip thrust,barbell hip thrust",
        "Barbell Glute Bridge" to "glute bridge",
        "Dumbbell Lunges" to "lunge,dumbbell lunge",
        "Bodyweight Walking Lunge" to "walking lunge",
        "Dumbbell Step Ups" to "step up",
        "Crunches" to "crunch",
        "Cable Crunch" to "cable crunch",
        "Plank" to "plank",
        "Hanging Leg Raise" to "hanging leg raise,leg raise",
        "Russian Twist" to "russian twist",
        "Ab Roller" to "ab wheel,ab rollout,ab roller",
        "Barbell Shrug" to "shrug,barbell shrug",
        "Dumbbell Shrug" to "dumbbell shrug",
        "Good Morning" to "good morning",
        "Hyperextensions (Back Extensions)" to "back extension,hyperextension",
        "One-Arm Kettlebell Swings" to "kettlebell swing",
        "Box Jump (Multiple Response)" to "box jump",
        "Rope Jumping" to "jump rope,skipping,skip rope",
        "Power Clean" to "power clean,clean",
        "Mountain Climbers" to "mountain climber",
    )

    /** squashed alias → squashed target name */
    val map: Map<String, String> = buildMap {
        raw.forEach { (target, keys) -> keys.split(',').forEach { put(Norm.squash(it), Norm.squash(target)) } }
    }
    val targets: Set<String> get() = raw.keys
}

/** Fuzzy matcher from free-typed exercise names to the bundled library. */
class ExerciseMatcher(library: List<Exercise>) {
    data class Match(val exercise: Exercise, val score: Double)

    private class Entry(val ex: Exercise, val tokens: Set<String>, val squash: String)

    private val entries = library.map { e -> Norm.tokens(e.name).let { Entry(e, it.toSet(), it.joinToString("")) } }
    private val byId = library.associateBy { it.id }
    private val bySquash: Map<String, Entry> = entries.reversed().associateBy { it.squash }
    private val preferredEquipment = listOf("barbell", "dumbbell", "body only", "cable", "machine")

    fun match(name: String, id: String? = null): Match? {
        id?.let { byId[it] }?.let { return Match(it, 1.0) }
        val qt = Norm.tokens(name)
        if (qt.isEmpty()) return null
        val qs = qt.joinToString("")
        bySquash[qs]?.let { return Match(it.ex, 1.0) }
        Aliases.map[qs]?.let { t -> bySquash[t] }?.let { return Match(it.ex, 0.98) }

        val q = qt.toSet()
        var best: Entry? = null
        var bestScore = 0.0
        var bestCov = 0.0
        for (e in entries) {
            val common = q.count { it in e.tokens || (it.length >= 4 && e.squash.contains(it)) }
            if (common == 0) continue
            val cov = common.toDouble() / q.size
            val prec = e.tokens.count { it in q || qs.contains(it) }.toDouble() / e.tokens.size
            var s = 0.7 * cov + 0.3 * prec
            if (qs.length >= 5 && e.squash.contains(qs)) s = maxOf(s, 0.8 + 0.15 * qs.length / e.squash.length)
            if (cov < 0.999) s -= 0.1
            val better = s > bestScore + 1e-9 || (abs(s - bestScore) < 1e-9 && best != null && prefer(e, best))
            if (better) { best = e; bestScore = s; bestCov = cov }
        }
        return best?.takeIf { bestScore >= 0.6 && bestCov >= 0.66 }?.let { Match(it.ex, bestScore.coerceAtMost(0.95)) }
    }

    private fun prefer(a: Entry, b: Entry): Boolean {
        val ra = preferredEquipment.indexOf(a.ex.equipment).let { if (it < 0) 99 else it }
        val rb = preferredEquipment.indexOf(b.ex.equipment).let { if (it < 0) 99 else it }
        return if (ra != rb) ra < rb else a.ex.name.length < b.ex.name.length
    }

    /** True only for exact / alias hits: used to tell exercise lines from section headers. */
    fun isKnown(name: String) = (match(name)?.score ?: 0.0) >= 0.98
}
