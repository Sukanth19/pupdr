package dev.suks.pupdr

import dev.suks.pupdr.data.ExerciseLibrary
import dev.suks.pupdr.domain.Aliases
import dev.suks.pupdr.domain.ExerciseMatcher
import dev.suks.pupdr.domain.Norm
import dev.suks.pupdr.domain.RoutineCodec
import dev.suks.pupdr.domain.RoutineDto
import dev.suks.pupdr.domain.RoutineItemDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RoutineCodecTest {
    private val library = ExerciseLibrary.parse(File("src/main/assets/exercises.json").readText())
    private val matcher = ExerciseMatcher(library)
    private fun decode(t: String) = RoutineCodec.decode(t.trimIndent(), matcher::isKnown)
    private fun item(name: String, sets: Int = 3, reps: String = "10") = RoutineItemDto(name = name, sets = sets, reps = reps)
    private fun matched(q: String) = matcher.match(q)?.exercise?.name

    @Test fun jsonRoundTrip() {
        val r = listOf(RoutineDto("Push", "notes", listOf(RoutineItemDto("bench", "Barbell Bench Press - Medium Grip", 4, "6-8"))))
        assertEquals(r, RoutineCodec.decode(RoutineCodec.encode(r)))
    }

    @Test fun simpleText() {
        val rs = decode(
            """
            # Push Day
            Bench Press 4x8
            - Overhead press, 3 x 10-12
            Dips

            Legs:
            1. Squat 5×5
            """
        )
        assertEquals(listOf("Push Day", "Legs"), rs.map { it.name })
        assertEquals(item("Bench Press", 4, "8"), rs[0].exercises[0])
        assertEquals(item("Overhead press", 3, "10-12"), rs[0].exercises[1])
        assertEquals(item("Dips"), rs[0].exercises[2])
        assertEquals(item("Squat", 5, "5"), rs[1].exercises[0])
    }

    @Test fun manyWaysOfWritingSetsAndReps() {
        val e = decode(
            """
            Bench press - 4 sets of 8 reps
            Squats: 3 sets x 10 reps @ 100kg
            Deadlift 8 reps x 3 sets
            Barbell row 4 sets 8-10 reps (rest 90s)
            Lat pulldown 3 X 12 RPE 8
            Plank 3x30s
            Treadmill 20 min
            Pull ups 3 x AMRAP
            Squat 5/5/5
            Curls 12-10-8
            Leg press — 4 × 10 to 12
            A1. Incline DB press 3x8
            B2) Cable fly 3 sets, 15 reps
            * **Face pulls** 3x15
            ✅ Lateral raises 4x15
            [ ] Calf raises 4 sets of 20
            Hip thrust 3 sets 10 reps 60 kg
            Skull crushers 3x10 tempo 3-1-1
            Hammer curls 3x12 each arm
            """
        ).single().exercises
        val want = listOf(
            item("Bench press", 4, "8"),
            item("Squats", 3, "10"),
            item("Deadlift", 3, "8"),
            item("Barbell row", 4, "8-10"),
            item("Lat pulldown", 3, "12"),
            item("Plank", 3, "30s"),
            item("Treadmill", 1, "20min"),
            item("Pull ups", 3, "AMRAP"),
            item("Squat", 3, "5"),
            item("Curls", 3, "12/10/8"),
            item("Leg press", 4, "10-12"),
            item("Incline DB press", 3, "8"),
            item("Cable fly", 3, "15"),
            item("Face pulls", 3, "15"),
            item("Lateral raises", 4, "15"),
            item("Calf raises", 4, "20"),
            item("Hip thrust", 3, "10"),
            item("Skull crushers", 3, "10"),
            item("Hammer curls", 3, "12"),
        )
        want.zip(e).forEach { (w, g) -> assertEquals(w, g) }
        assertEquals(want.size, e.size)
    }

    @Test fun headersOfAllKinds() {
        val rs = decode(
            """
            Day 1 - Chest & Triceps
            Bench 4x8
            Monday
            Squat 5x5
            PULL
            Rows 4x10
            **Upper body**
            OHP 3x5
            Workout B:
            RDL 3x8
            Back and biceps
            Chin ups 3x8
            Warm-up:
            Jumping jacks 2 min
            """
        )
        assertEquals(listOf("Day 1 - Chest & Triceps", "Monday", "Pull", "Upper body", "Workout B", "Back and biceps"), rs.map { it.name })
        assertEquals(2, rs.last().exercises.size) // warm-up stays inside the current routine
    }

    @Test fun inlineHeaderAndMultipleExercisesPerLine() {
        val rs = decode("Day 1: Bench 3x8, Squat 5x5; Rows 3x10\nDay 2: Deadlift 1x5 + Pull ups 3x8")
        assertEquals(listOf("Day 1", "Day 2"), rs.map { it.name })
        assertEquals(listOf("Bench", "Squat", "Rows"), rs[0].exercises.map { it.name })
        assertEquals(listOf("Deadlift", "Pull ups"), rs[1].exercises.map { it.name })
        // a plain comma list of known exercises
        assertEquals(listOf("Squats", "Lunges", "Deadlifts"), decode("Squats, lunges, deadlifts").single().exercises.map { it.name })
        // but a comma before the scheme is not a list
        assertEquals(listOf(item("Bench Press", 3, "10")), decode("Bench Press, 3x10").single().exercises)
    }

    @Test fun verticalFormat() {
        val e = decode(
            """
            Bench Press
            Sets: 4
            Reps: 6-8
            Rest: 2 min

            Incline dumbbell press
            3 x 12
            """
        ).single().exercises
        assertEquals(listOf(item("Bench Press", 4, "6-8"), item("Incline dumbbell press", 3, "12")), e)
    }

    @Test fun tables() {
        val md = decode(
            """
            ## Push
            | Exercise | Sets | Reps |
            |---|---|---|
            | Bench press | 4 | 6-8 |
            | Dips | 3 | AMRAP |
            """
        ).single()
        assertEquals("Push", md.name)
        assertEquals(listOf(item("Bench press", 4, "6-8"), item("Dips", 3, "AMRAP")), md.exercises)

        val csv = decode("Day,Exercise,Sets,Reps\nPush,Bench Press,4,8\nPush,\"Dips, weighted\",3,10\nPull,Rows,4,10")
        assertEquals(listOf("Push", "Pull"), csv.map { it.name })
        assertEquals(item("Dips, weighted", 3, "10"), csv[0].exercises[1])

        val tsv = decode("Exercise\tSets x Reps\nSquat\t5x5\nLeg curl\t3x12").single().exercises
        assertEquals(listOf(item("Squat", 5, "5"), item("Leg curl", 3, "12")), tsv)

        val headerless = decode("Bench Press | 4 | 8\nRows, 3, 10").single().exercises
        assertEquals(listOf(item("Bench Press", 4, "8"), item("Rows", 3, "10")), headerless)
    }

    @Test fun foreignJson() {
        val a = RoutineCodec.decode("""{"workouts":[{"title":"Leg day","exercises":[{"exercise":{"name":"Squat"},"sets":[{"reps":5,"weight":100},{"reps":5},{"reps":5}]},{"exercise_name":"Leg press","sets":"4","reps":"10-12"}]}]}""")
        assertEquals("Leg day", a.single().name)
        assertEquals(listOf(item("Squat", 3, "5"), item("Leg press", 4, "10-12")), a.single().exercises)

        val b = RoutineCodec.decode("""{"Push": ["Bench 4x8", "Dips 3x10"], "Pull": [{"name": "Rows", "sets": 3, "reps": 12}]}""")
        assertEquals(listOf("Push", "Pull"), b.map { it.name })
        assertEquals(item("Bench", 4, "8"), b[0].exercises[0])

        val c = RoutineCodec.decode("""[{"name":"Squat","sets":5,"reps":5},{"name":"Bench 3x8"}]""")
        assertEquals(listOf(item("Squat", 5, "5"), item("Bench", 3, "8")), c.single().exercises)
    }

    @Test fun junkIsIgnored() {
        val rs = decode(
            """
            My program (from https://example.com/plan)
            Week 1
            Notes:
            Rest 2 min between sets
            Rest day
            Squat 5x5
            """
        )
        assertEquals(listOf(item("Squat", 5, "5")), rs.single().exercises)
        assertTrue(decode("   \n\n").isEmpty())
    }

    @Test fun aliasTargetsExist() {
        val names = library.map { Norm.squash(it.name) }.toSet()
        Aliases.targets.forEach { assertTrue("missing alias target $it", Norm.squash(it) in names) }
    }

    @Test fun matchingSlang() {
        assertEquals("Barbell Bench Press - Medium Grip", matched("bench press"))
        assertEquals("Barbell Bench Press - Medium Grip", matched("Bench"))
        assertEquals("Standing Military Press", matched("OHP"))
        assertEquals("Romanian Deadlift", matched("RDLs"))
        assertEquals("Pullups", matched("Pull-ups"))
        assertEquals("Pullups", matched("pull ups"))
        assertEquals("Chin-Up", matched("chin ups"))
        assertEquals("Pushups", matched("push-ups"))
        assertEquals("Wide-Grip Lat Pulldown", matched("lat pull down"))
        assertEquals("Incline Dumbbell Press", matched("Incline DB press"))
        assertEquals("Side Lateral Raise", matched("lateral raises"))
        assertEquals("Triceps Pushdown", matched("tricep pushdowns"))
        assertEquals("EZ-Bar Skullcrusher", matched("Skull crushers"))
        assertEquals("Hammer Curls", matched("hammer curl"))
        assertEquals("Barbell Squat", matched("Squats"))
        assertEquals("Leg Press", matched("Leg press"))
        assertEquals("Face Pull", matched("Face pulls"))
        assertEquals("One-Arm Dumbbell Row", matched("db row"))
        assertNotNull(matched("Seated cable row"))
        assertNotNull(matched("Incline Cable Flyes"))
        assertNull(matched("Zercher carry"))
        assertNull(matched("My Weird Move"))
        assertEquals("Standing Military Press", matcher.match("whatever", "Standing_Military_Press")?.exercise?.name)
    }
}
