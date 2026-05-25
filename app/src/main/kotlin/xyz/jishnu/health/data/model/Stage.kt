package xyz.jishnu.health.data.model

data class Stage(
    val id: String,
    val name: String,
    val short: String,
    val range: String,
    val startHour: Int,
    val title: String,
    val body: String,
    val benefits: List<String>,
    val hue: Int,
)
