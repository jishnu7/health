package xyz.jishnu.health.data.model

enum class FastStatus { Goal, Short, Ongoing }

/**
 * `null` means "no badge" — used for today's weight-only row before any fast
 * has been logged. A non-null status is one of the three concrete states.
 */
fun DayEntry.status(goalHours: Int): FastStatus? = when {
    isOngoing -> FastStatus.Ongoing
    isPreFastToday -> null
    fastHours >= goalHours -> FastStatus.Goal
    sessions.isNotEmpty() -> FastStatus.Short
    else -> null
}
