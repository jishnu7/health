package xyz.jishnu.health.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import xyz.jishnu.health.data.model.Sex
import xyz.jishnu.health.data.model.Units
import xyz.jishnu.health.ui.theme.IntermTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SexSegmented(
    sex: Sex?,
    onSelect: (Sex) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = IntermTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.border2)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SexButton(label = "Male", active = sex == Sex.Male, onClick = { onSelect(Sex.Male) }, modifier = Modifier.weight(1f))
        SexButton(label = "Female", active = sex == Sex.Female, onClick = { onSelect(Sex.Female) }, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SexButton(label: String, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = IntermTheme.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) c.primarySoft else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = IntermTheme.typography.body.copy(
                fontSize = 14.sp,
                fontWeight = if (active) FontWeight.W600 else FontWeight.W500,
            ),
            color = if (active) c.primary else c.ink2,
        )
    }
}

/**
 * Modal that lets the user pick their height by swiping the entire figure
 * area up or down. The human silhouette grows / shrinks with the value, the
 * current height stays pinned at the top, and faint horizontal lines in the
 * background hint that the surface is scrollable.
 *
 * [sex] picks the male or female silhouette; defaults to male when unknown.
 */
@Composable
fun HeightDialog(
    currentCm: Double?,
    units: Units,
    sex: Sex? = null,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
) {
    val c = IntermTheme.colors
    val initialCm = (currentCm ?: 170.0).coerceIn(MIN_HEIGHT_CM.toDouble(), MAX_HEIGHT_CM.toDouble())
    var selectedCm by remember { mutableDoubleStateOf(initialCm) }
    val effectiveSex = sex ?: Sex.Male

    // pointerInput(Unit) attaches its handler once — without rememberUpdatedState
    // the lambda would keep using the initial selectedCm forever and the drag
    // would look frozen.
    val latestSelected by rememberUpdatedState(selectedCm)
    val latestSetSelected by rememberUpdatedState({ cm: Double ->
        selectedCm = cm.coerceIn(MIN_HEIGHT_CM.toDouble(), MAX_HEIGHT_CM.toDouble())
    })

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(c.card)
                .padding(20.dp),
        ) {
            Text("Height", style = IntermTheme.typography.headerTitle, color = c.ink)
            Spacer(Modifier.height(4.dp))
            Text("Swipe the figure to adjust.", style = IntermTheme.typography.caption, color = c.muted)

            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(c.border2)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { change, dragAmount ->
                            change.consume()
                            // The whole pad is interpreted as ~VISIBLE_CM of vertical
                            // range; bigger drags = bigger cm changes.
                            val pxPerCm = size.height / VISIBLE_CM
                            val deltaCm = -dragAmount / pxPerCm
                            latestSetSelected(latestSelected + deltaCm)
                        }
                    },
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Background scale lines — faint horizontal ticks across the
                    // pad, used as a scrollability cue rather than a precise
                    // measurement.
                    val rows = 14
                    for (i in 1 until rows) {
                        val y = size.height * (i / rows.toFloat())
                        drawLine(
                            color = c.subtle.copy(alpha = 0.40f),
                            start = Offset(size.width * 0.08f, y),
                            end = Offset(size.width * 0.92f, y),
                            strokeWidth = 1f,
                        )
                    }
                    // Then the human figure on top.
                    drawHumanFigure(selectedCm, effectiveSex, c.ink, c.border)
                }

                // Height value pinned to the top centre.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 18.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.card.copy(alpha = 0.92f))
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                ) {
                    Text(
                        formatHeightForDisplay(selectedCm, units),
                        style = IntermTheme.typography.hDisplay.copy(fontSize = 32.sp),
                        color = c.primary,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IntermButton(onClick = onDismiss, variant = IntermButtonVariant.Ghost, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                IntermButton(
                    onClick = { onConfirm(selectedCm.roundToInt().toDouble()) },
                    variant = IntermButtonVariant.Primary,
                    modifier = Modifier.weight(1f),
                ) { Text("Save") }
            }
        }
    }
}

// Lowest (Chandra Bahadur Dangi, ~54.6 cm) and tallest (Robert Wadlow, ~272 cm)
// medically-recorded human heights — generously rounded so the picker covers
// the full range and any user height in between.
private const val MIN_HEIGHT_CM = 55
private const val MAX_HEIGHT_CM = 272

// How many cm map to one full vertical swipe across the pad. Tuned to feel
// responsive without making 1cm changes hard to land on.
private const val VISIBLE_CM = 80f

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHumanFigure(
    heightCm: Double,
    sex: Sex,
    ink: Color,
    groundColor: Color,
) {
    val w = size.width
    val h = size.height
    val groundY = h - 18f
    // The figure needs clearance for the height pill at the top — keep its
    // crown below ~90 px down from the top edge so the readout stays legible.
    val maxFigureH = groundY - 90f
    val ratio = ((heightCm - MIN_HEIGHT_CM) / (MAX_HEIGHT_CM - MIN_HEIGHT_CM))
        .toFloat()
        .coerceIn(0f, 1f)
    val figureH = maxFigureH * (0.12f + 0.88f * ratio)
    val figureTop = groundY - figureH
    val cx = w / 2f

    when (sex) {
        Sex.Male -> drawMaleSilhouette(cx, figureTop, figureH, groundY, ink)
        Sex.Female -> drawFemaleSilhouette(cx, figureTop, figureH, groundY, ink)
    }

    // Ground line.
    drawLine(
        color = groundColor,
        start = Offset(w * 0.12f, groundY + 4f),
        end = Offset(w * 0.88f, groundY + 4f),
        strokeWidth = 1.4f,
        cap = StrokeCap.Round,
    )
}

/** Male silhouette: wider shoulders, V-shaped torso. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMaleSilhouette(
    cx: Float, top: Float, h: Float, groundY: Float, ink: Color,
) {
    val headR = h * 0.072f
    val headCy = top + headR
    val shoulderHalf = h * 0.22f
    val waistHalf = h * 0.15f
    val hipHalf = h * 0.18f
    val shoulderY = headCy + headR + h * 0.025f
    val waistY = top + h * 0.42f
    val hipY = top + h * 0.61f
    val crotchY = top + h * 0.65f
    val legOuter = hipHalf * 0.88f
    val legGap = (h * 0.022f).coerceAtLeast(2f)
    val footY = groundY

    // Head + neck.
    drawCircle(ink, headR, Offset(cx, headCy))
    drawRect(
        ink,
        topLeft = Offset(cx - headR * 0.40f, headCy + headR * 0.62f),
        size = Size(headR * 0.80f, shoulderY - (headCy + headR * 0.62f) + 1f),
    )

    // Torso + legs as one path, curved at shoulder→waist and waist→hip.
    val body = Path().apply {
        moveTo(cx - shoulderHalf, shoulderY)
        cubicTo(
            cx - shoulderHalf, shoulderY + (waistY - shoulderY) * 0.45f,
            cx - waistHalf * 1.08f, waistY - (waistY - shoulderY) * 0.20f,
            cx - waistHalf, waistY,
        )
        cubicTo(
            cx - waistHalf * 1.05f, waistY + (hipY - waistY) * 0.30f,
            cx - hipHalf * 1.02f, hipY - (hipY - waistY) * 0.30f,
            cx - hipHalf, hipY,
        )
        lineTo(cx - legOuter, footY)
        lineTo(cx - legGap, footY)
        lineTo(cx - legGap, crotchY)
        lineTo(cx, crotchY - h * 0.006f)
        lineTo(cx + legGap, crotchY)
        lineTo(cx + legGap, footY)
        lineTo(cx + legOuter, footY)
        lineTo(cx + hipHalf, hipY)
        cubicTo(
            cx + hipHalf * 1.02f, hipY - (hipY - waistY) * 0.30f,
            cx + waistHalf * 1.05f, waistY + (hipY - waistY) * 0.30f,
            cx + waistHalf, waistY,
        )
        cubicTo(
            cx + waistHalf * 1.08f, waistY - (waistY - shoulderY) * 0.20f,
            cx + shoulderHalf, shoulderY + (waistY - shoulderY) * 0.45f,
            cx + shoulderHalf, shoulderY,
        )
        close()
    }
    drawPath(body, ink)

    // Arms — sweeping silhouette from shoulder to mid-thigh.
    val armElbowY = top + h * 0.40f
    val armWristY = top + h * 0.58f
    val armWristHalf = waistHalf * 1.12f
    val armWidth = h * 0.052f
    drawArm(cx, shoulderHalf, armElbowY, armWristY, armWristHalf, armWidth, top, h, shoulderY, ink, mirror = false)
    drawArm(cx, shoulderHalf, armElbowY, armWristY, armWristHalf, armWidth, top, h, shoulderY, ink, mirror = true)
}

/** Female silhouette: narrower shoulders, defined waist, wider hips, shoulder-length hair. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFemaleSilhouette(
    cx: Float, top: Float, h: Float, groundY: Float, ink: Color,
) {
    val headR = h * 0.068f
    val headCy = top + headR + h * 0.005f
    val shoulderHalf = h * 0.17f
    val waistHalf = h * 0.10f
    val hipHalf = h * 0.235f
    val shoulderY = headCy + headR + h * 0.025f
    val waistY = top + h * 0.43f
    val hipY = top + h * 0.62f
    val crotchY = top + h * 0.66f
    val legOuter = hipHalf * 0.62f
    val legGap = (h * 0.018f).coerceAtLeast(2f)
    val footY = groundY

    // Hair drawn first so it sits behind the head and neck.
    val hair = Path().apply {
        moveTo(cx - headR * 1.08f, headCy)
        cubicTo(
            cx - headR * 1.30f, headCy + headR * 0.60f,
            cx - headR * 1.55f, shoulderY - h * 0.015f,
            cx - headR * 1.20f, shoulderY + h * 0.06f,
        )
        lineTo(cx + headR * 1.20f, shoulderY + h * 0.06f)
        cubicTo(
            cx + headR * 1.55f, shoulderY - h * 0.015f,
            cx + headR * 1.30f, headCy + headR * 0.60f,
            cx + headR * 1.08f, headCy,
        )
        cubicTo(
            cx + headR * 1.05f, headCy - headR * 1.15f,
            cx - headR * 1.05f, headCy - headR * 1.15f,
            cx - headR * 1.08f, headCy,
        )
        close()
    }
    drawPath(hair, ink)

    // Head + neck on top.
    drawCircle(ink, headR, Offset(cx, headCy))
    drawRect(
        ink,
        topLeft = Offset(cx - headR * 0.35f, headCy + headR * 0.65f),
        size = Size(headR * 0.70f, shoulderY - (headCy + headR * 0.65f) + 1f),
    )

    // Body — pronounced hourglass curve.
    val body = Path().apply {
        moveTo(cx - shoulderHalf, shoulderY)
        cubicTo(
            cx - shoulderHalf * 0.88f, shoulderY + (waistY - shoulderY) * 0.30f,
            cx - waistHalf * 0.65f, waistY - (waistY - shoulderY) * 0.40f,
            cx - waistHalf, waistY,
        )
        cubicTo(
            cx - waistHalf * 0.85f, waistY + (hipY - waistY) * 0.28f,
            cx - hipHalf * 0.94f, hipY - (hipY - waistY) * 0.36f,
            cx - hipHalf, hipY,
        )
        lineTo(cx - legOuter, footY)
        lineTo(cx - legGap, footY)
        lineTo(cx - legGap, crotchY)
        lineTo(cx, crotchY - h * 0.006f)
        lineTo(cx + legGap, crotchY)
        lineTo(cx + legGap, footY)
        lineTo(cx + legOuter, footY)
        lineTo(cx + hipHalf, hipY)
        cubicTo(
            cx + hipHalf * 0.94f, hipY - (hipY - waistY) * 0.36f,
            cx + waistHalf * 0.85f, waistY + (hipY - waistY) * 0.28f,
            cx + waistHalf, waistY,
        )
        cubicTo(
            cx + waistHalf * 0.65f, waistY - (waistY - shoulderY) * 0.40f,
            cx + shoulderHalf * 0.88f, shoulderY + (waistY - shoulderY) * 0.30f,
            cx + shoulderHalf, shoulderY,
        )
        close()
    }
    drawPath(body, ink)

    // Arms — thinner, hanging slightly outside the narrow waist.
    val armElbowY = top + h * 0.40f
    val armWristY = top + h * 0.58f
    val armWristHalf = waistHalf * 1.65f
    val armWidth = h * 0.040f
    drawArm(cx, shoulderHalf, armElbowY, armWristY, armWristHalf, armWidth, top, h, shoulderY, ink, mirror = false)
    drawArm(cx, shoulderHalf, armElbowY, armWristY, armWristHalf, armWidth, top, h, shoulderY, ink, mirror = true)
}

/** Helper that draws one arm path (or its mirror) from shoulder to wrist. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArm(
    cx: Float,
    shoulderHalf: Float,
    elbowY: Float,
    wristY: Float,
    wristHalf: Float,
    armWidth: Float,
    figureTop: Float,
    figureH: Float,
    shoulderY: Float,
    ink: Color,
    mirror: Boolean,
) {
    val s = if (mirror) 1f else -1f
    val outerShoulderX = cx + s * shoulderHalf
    val outerWristX = cx + s * (wristHalf + armWidth * 0.5f)
    val innerWristX = cx + s * (wristHalf - armWidth * 0.5f)
    val innerShoulderX = cx + s * (shoulderHalf - armWidth * 0.45f)
    val controlOuter1Y = figureTop + figureH * 0.30f
    val controlInner1Y = figureTop + figureH * 0.30f
    val controlMidY = (elbowY + figureTop + figureH * 0.30f) / 2f

    val arm = Path().apply {
        moveTo(outerShoulderX, shoulderY + figureH * 0.005f)
        cubicTo(
            outerShoulderX + s * figureH * 0.005f, controlOuter1Y,
            outerWristX + s * armWidth * 0.10f, controlMidY,
            outerWristX, wristY,
        )
        lineTo(innerWristX, wristY)
        cubicTo(
            innerWristX + s * armWidth * 0.10f, controlMidY,
            innerShoulderX + s * armWidth * 0.20f, controlInner1Y,
            innerShoulderX, shoulderY + figureH * 0.04f,
        )
        close()
    }
    drawPath(arm, ink)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateOfBirthDialog(
    currentIso: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val initialMillis = currentIso?.let { runCatching { LocalDate.parse(it).atStartOfDay(zone).toInstant().toEpochMilli() }.getOrNull() }
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { ms ->
                    val date = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
                    onConfirm(date.toString())
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = state)
    }
}

fun formatHeightForDisplay(cm: Double, units: Units): String = when (units) {
    Units.Metric -> "${cm.toInt()} cm"
    Units.Imperial -> {
        val totalIn = cm / 2.54
        val feet = (totalIn / 12).toInt()
        val inches = kotlin.math.round(totalIn - feet * 12).toInt()
        "$feet'${inches}\""
    }
}

fun formatDateOfBirth(iso: String): String = runCatching {
    LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))
}.getOrDefault(iso)
