package xyz.jishnu.health.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

private fun strokeIcon(
    name: String,
    path: String,
    width: Float = 1.7f,
    cap: StrokeCap = StrokeCap.Round,
    join: StrokeJoin = StrokeJoin.Round,
    viewport: Float = 24f,
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = viewport,
    viewportHeight = viewport,
).apply {
    addPath(
        pathData = addPathNodes(path),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = width,
        strokeLineCap = cap,
        strokeLineJoin = join,
    )
}.build()

private fun strokeIconMulti(
    name: String,
    paths: List<String>,
    width: Float = 1.7f,
    cap: StrokeCap = StrokeCap.Round,
    join: StrokeJoin = StrokeJoin.Round,
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    paths.forEach { p ->
        addPath(
            pathData = addPathNodes(p),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = width,
            strokeLineCap = cap,
            strokeLineJoin = join,
        )
    }
}.build()

private fun fillIcon(name: String, path: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        addPath(
            pathData = addPathNodes(path),
            fill = SolidColor(Color.Black),
        )
    }.build()

object IntermIcons {
    val Home = strokeIcon(
        "Home",
        "M3 11l9-7 9 7v9a2 2 0 01-2 2h-4v-7h-6v7H5a2 2 0 01-2-2v-9z",
    )
    val Scale = strokeIconMulti(
        "Scale",
        listOf(
            "M3 4 H21 V20 H3 Z",
            "M8 9h8M10 14l2-3 2 3",
        ),
    )
    val Chart = strokeIcon(
        "Chart",
        "M3 20V6M3 20h18M7 16l4-5 3 3 5-7",
    )
    val History = strokeIconMulti(
        "History",
        listOf(
            "M3 12a9 9 0 109-9 9 9 0 00-7 3.3M3 4v4h4",
            "M12 7v5l3 2",
        ),
    )
    val Settings = strokeIconMulti(
        "Settings",
        listOf(
            "M12 9 A3 3 0 1 0 12 15 A3 3 0 1 0 12 9",
            "M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 11-2.83 2.83l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 11-4 0v-.09a1.65 1.65 0 00-1-1.51 1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 11-2.83-2.83l.06-.06a1.65 1.65 0 00.33-1.82 1.65 1.65 0 00-1.51-1H3a2 2 0 110-4h.09a1.65 1.65 0 001.51-1 1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 112.83-2.83l.06.06a1.65 1.65 0 001.82.33h0a1.65 1.65 0 001-1.51V3a2 2 0 114 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 112.83 2.83l-.06.06a1.65 1.65 0 00-.33 1.82v0a1.65 1.65 0 001.51 1H21a2 2 0 110 4h-.09a1.65 1.65 0 00-1.51 1z",
        ),
    )
    val Plus = strokeIcon("Plus", "M12 5v14M5 12h14", width = 2f)
    val Minus = strokeIcon("Minus", "M5 12h14", width = 2f)
    val Bell = strokeIcon(
        "Bell",
        "M6 8a6 6 0 1112 0c0 7 3 9 3 9H3s3-2 3-9M10 21a2 2 0 004 0",
    )
    val Back = strokeIcon("Back", "M15 18l-6-6 6-6")
    val Check = strokeIcon("Check", "M5 12l4.5 4.5L19 7", width = 2.2f)
    val Chevron = strokeIcon("Chevron", "M9 6l6 6-6 6", width = 1.8f)
    val Flame = strokeIcon(
        "Flame",
        "M12 2c0 4-3 5-3 9a3 3 0 006 0c0-2-1-3-1-5 2 1 4 3 4 7a6 6 0 11-12 0c0-5 6-7 6-11z",
        cap = StrokeCap.Butt,
    )
    val Drop = strokeIcon(
        "Drop",
        "M12 3l6 9a6 6 0 11-12 0l6-9z",
        cap = StrokeCap.Butt,
    )
    val Food = strokeIcon(
        "Food",
        "M4 4v8a4 4 0 008 0V4M6 4v6M10 4v6M18 4c-2 0-3 3-3 7v3h6V4c-3 0-3 0-3 0zM18 14v6",
    )
    val Stop = fillIcon("Stop", "M6 6h12v12h-12z")
    val Play = fillIcon("Play", "M7 5l12 7-12 7V5z")
    val Calendar = strokeIconMulti(
        "Calendar",
        listOf(
            "M3 5 H21 V21 H3 Z",
            "M3 10h18M8 3v4M16 3v4",
        ),
        width = 1.8f,
    )
    val ChevronDown = strokeIcon("ChevronDown", "M6 9l6 6 6-6", width = 2f)
}
