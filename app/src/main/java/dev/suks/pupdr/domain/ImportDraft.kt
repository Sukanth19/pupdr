package dev.suks.pupdr.domain

/** An imported exercise awaiting review. exerciseId == null means "create as a custom exercise". */
data class DraftItem(
    val key: Long,
    val source: String,
    val exerciseId: String?,
    val exerciseName: String,
    val confidence: Double,
    val sets: Int,
    val reps: String,
) {
    val isNew get() = exerciseId == null
    val isUnsure get() = exerciseId != null && confidence < 0.9
}

data class DraftRoutine(val key: Long, val name: String, val items: List<DraftItem>)
