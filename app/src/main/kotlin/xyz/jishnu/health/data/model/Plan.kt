package xyz.jishnu.health.data.model

data class Plan(
    val id: String,
    val label: String,
    val fastHours: Int,
    val subtitle: String,
) {
    val eatHours: Int get() = 24 - fastHours
}
