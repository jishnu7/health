package xyz.jishnu.health.domain

import xyz.jishnu.health.data.constants.Stages
import xyz.jishnu.health.data.model.Stage

object StageCalculator {
    fun stageFor(elapsedHours: Double, stages: List<Stage> = Stages.all): Stage {
        var current = stages.first()
        for (s in stages) if (s.startHour <= elapsedHours) current = s
        return current
    }

    fun indexFor(elapsedHours: Double, stages: List<Stage> = Stages.all): Int {
        val stage = stageFor(elapsedHours, stages)
        return stages.indexOf(stage)
    }
}
