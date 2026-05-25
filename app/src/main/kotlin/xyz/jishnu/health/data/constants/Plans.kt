package xyz.jishnu.health.data.constants

import xyz.jishnu.health.data.model.Plan

object Plans {
    val all: List<Plan> = listOf(
        Plan(id = "14:10", label = "14:10", fastHours = 14, subtitle = "Starter — gentle"),
        Plan(id = "16:8", label = "16:8", fastHours = 16, subtitle = "Most common"),
        Plan(id = "18:6", label = "18:6", fastHours = 18, subtitle = "Intermediate"),
        Plan(id = "20:4", label = "20:4", fastHours = 20, subtitle = "Warrior"),
        Plan(id = "23:1", label = "23:1", fastHours = 23, subtitle = "OMAD"),
    )

    val default: Plan = all.first { it.id == "16:8" }

    fun byId(id: String): Plan = all.firstOrNull { it.id == id } ?: default
}
