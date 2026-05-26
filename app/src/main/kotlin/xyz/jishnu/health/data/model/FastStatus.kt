package xyz.jishnu.health.data.model

enum class FastStatus { Goal, Short, Ongoing }

fun DayEntry.status(goalHours: Int): FastStatus = when {
    isOngoing -> FastStatus.Ongoing
    fastHours >= goalHours -> FastStatus.Goal
    sessions.isNotEmpty() || weight != null -> FastStatus.Short
    else -> FastStatus.Short
}
