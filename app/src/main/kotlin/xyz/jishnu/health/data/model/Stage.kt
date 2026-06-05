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
    /**
     * Present-tense status line shown on the metabolic stage focus card
     * while this stage is active — e.g. "Burning through stored carbs".
     */
    val message: String,
)
