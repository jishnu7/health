package xyz.jishnu.health.data.model

enum class FastStatus { Goal, Short, Ongoing }

fun DayEntry.status(goalHours: Int): FastStatus = when {
    session != null && session.endMs == null -> FastStatus.Ongoing
    fastHours >= goalHours -> FastStatus.Goal
    session != null || weight != null -> FastStatus.Short
    else -> FastStatus.Short
}
