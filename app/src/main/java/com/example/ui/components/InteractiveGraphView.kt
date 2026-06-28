package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.InfinityBlue
import com.example.ui.theme.InfinityOrange
import java.util.Locale
import kotlin.math.*

@OptIn(ExperimentalTextApi::class)
@Composable
fun InteractiveGraphView(
    functionStr: String,
    modifier: Modifier = Modifier,
    zoomScale: Float = 40f, // pixels per unit
    onOffsetChanged: (Offset) -> Unit = {}
) {
    val textMeasurer = rememberTextMeasurer()
    
    // Grid pan offset state
    var offsetState by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetState += dragAmount
                    onOffsetChanged(offsetState)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Calculate origin coordinates in screen pixels
            val originX = width / 2f + offsetState.x
            val originY = height / 2f + offsetState.y

            // Draw grid background and coordinate lines
            val gridSpacing = zoomScale // 1 math unit = zoomScale pixels

            // Draw grid lines parallel to Y-axis
            var xPos = originX % gridSpacing
            while (xPos < width) {
                if (abs(xPos - originX) > 1f) {
                    drawLine(
                        color = Color(0x1FCCCCCC),
                        start = Offset(xPos, 0f),
                        end = Offset(xPos, height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                xPos += gridSpacing
            }

            // Draw grid lines parallel to X-axis
            var yPos = originY % gridSpacing
            while (yPos < height) {
                if (abs(yPos - originY) > 1f) {
                    drawLine(
                        color = Color(0x1FCCCCCC),
                        start = Offset(0f, yPos),
                        end = Offset(width, yPos),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                yPos += gridSpacing
            }

            // Draw primary X and Y axes
            drawLine(
                color = Color(0x7FFFFFFF),
                start = Offset(0f, originY),
                end = Offset(width, originY),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = Color(0x7FFFFFFF),
                start = Offset(originX, 0f),
                end = Offset(originX, height),
                strokeWidth = 2.dp.toPx()
            )

            // Draw axis numeric markers / labels
            val startUnitX = floor(-originX / gridSpacing).toInt()
            val endUnitX = ceil((width - originX) / gridSpacing).toInt()

            for (u in startUnitX..endUnitX) {
                if (u == 0) continue
                val px = originX + u * gridSpacing
                // Tic marks
                drawLine(
                    color = Color.White,
                    start = Offset(px, originY - 5.dp.toPx()),
                    end = Offset(px, originY + 5.dp.toPx()),
                    strokeWidth = 1.5.dp.toPx()
                )
                // Text Label
                if (px > 10f && px < width - 10f && originY > 10f && originY < height - 10f) {
                    drawText(
                        textMeasurer = textMeasurer,
                        text = u.toString(),
                        style = TextStyle(color = Color(0xBFFFFFFF), fontSize = 10.sp, fontFamily = FontFamily.Monospace),
                        topLeft = Offset(px - 6.dp.toPx(), originY + 8.dp.toPx())
                    )
                }
            }

            val startUnitY = floor(-originY / gridSpacing).toInt()
            val endUnitY = ceil((height - originY) / gridSpacing).toInt()

            for (u in startUnitY..endUnitY) {
                if (u == 0) continue
                val py = originY + u * gridSpacing
                // Tic marks
                drawLine(
                    color = Color.White,
                    start = Offset(originX - 5.dp.toPx(), py),
                    end = Offset(originX + 5.dp.toPx(), py),
                    strokeWidth = 1.5.dp.toPx()
                )
                // Text Label
                // Y-axis goes downwards on screen, so math +u is screen -u
                val labelVal = -u
                if (py > 10f && py < height - 10f && originX > 10f && originX < width - 10f) {
                    drawText(
                        textMeasurer = textMeasurer,
                        text = labelVal.toString(),
                        style = TextStyle(color = Color(0xBFFFFFFF), fontSize = 10.sp, fontFamily = FontFamily.Monospace),
                        topLeft = Offset(originX + 8.dp.toPx(), py - 8.dp.toPx())
                    )
                }
            }

            // Draw center (0,0) label
            drawText(
                textMeasurer = textMeasurer,
                text = "0",
                style = TextStyle(color = Color(0x7FFFFFFF), fontSize = 10.sp, fontFamily = FontFamily.Monospace),
                topLeft = Offset(originX - 12.dp.toPx(), originY + 4.dp.toPx())
            )

            // Evaluate and draw function curve
            val curvePath = Path()
            var first = true

            // Sweep across the horizontal pixels
            for (screenX in 0..width.toInt() step 2) {
                // Map screen pixel X to mathematical variable X
                val mathX = ((screenX - originX) / gridSpacing).toDouble()
                val mathY = evaluateFunction(functionStr, mathX)

                if (mathY.isNaN() || mathY.isInfinite()) {
                    first = true
                    continue
                }

                // Map mathematical Y to screen pixel Y (reversing Y orientation)
                val screenY = originY - (mathY.toFloat() * gridSpacing)

                // Ensure we don't draw extremely out of bounds values that warp the vector path
                if (screenY < -height || screenY > height * 2) {
                    first = true
                    continue
                }

                if (first) {
                    curvePath.moveTo(screenX.toFloat(), screenY)
                    first = false
                } else {
                    curvePath.lineTo(screenX.toFloat(), screenY)
                }
            }

            drawPath(
                path = curvePath,
                color = InfinityOrange,
                style = Stroke(width = 3.dp.toPx())
            )

            // Draw interactive dot at standard points (e.g. roots or origin intersections)
            // Let's add a visual pulsing indicator at the roots of the current function
            val roots = findApproximateRoots(functionStr)
            for (rootX in roots) {
                val pX = originX + rootX * gridSpacing
                val pY = originY
                if (pX in 0f..width && pY in 0f..height) {
                    // Pulsing accent dot
                    drawCircle(
                        color = InfinityBlue,
                        radius = 6.dp.toPx(),
                        center = Offset(pX.toFloat(), pY.toFloat())
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.5.dp.toPx(),
                        center = Offset(pX.toFloat(), pY.toFloat())
                    )
                    // Draw coordinates text near root
                    drawText(
                        textMeasurer = textMeasurer,
                        text = String.format(Locale.US, "(%.2f, 0)", rootX),
                        style = TextStyle(color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace),
                        topLeft = Offset(pX.toFloat() + 8.dp.toPx(), pY.toFloat() - 18.dp.toPx())
                    )
                }
            }
        }
    }
}

// Simple rule-based math function evaluator for y = f(x)
fun evaluateFunction(formula: String, x: Double): Double {
    val expr = formula.lowercase().replace(" ", "")
    try {
        // Special preset cases for standard graphing calculator
        if (expr == "sin(x)") return sin(x)
        if (expr == "cos(x)") return cos(x)
        if (expr == "tan(x)") return tan(x)
        if (expr == "sqrt(x)") return if (x >= 0) sqrt(x) else Double.NaN
        if (expr == "abs(x)") return abs(x)
        if (expr == "log(x)") return if (x > 0) log10(x) else Double.NaN
        if (expr == "ln(x)") return if (x > 0) ln(x) else Double.NaN

        // Polynomial parser for ax^2 + bx + c
        // e.g. x^2 - 4 or 2x^2 - 5x + 6 or x^3 or 2x + 3
        return evalPolynomial(expr, x)
    } catch (e: Exception) {
        return Double.NaN
    }
}

private fun evalPolynomial(expr: String, x: Double): Double {
    var termStr = expr.replace("-", "+-")
    if (termStr.startsWith("+")) termStr = termStr.substring(1)
    val parts = termStr.split("+")
    var total = 0.0

    for (part in parts) {
        if (part.isEmpty()) continue
        
        // Single constant number
        val constant = part.toDoubleOrNull()
        if (constant != null) {
            total += constant
            continue
        }

        // Variable term containing x
        var coeff = 1.0
        var power = 1.0

        if (part.contains("x^")) {
            val pieces = part.split("x^")
            val coeffStr = pieces[0]
            coeff = when {
                coeffStr.isEmpty() -> 1.0
                coeffStr == "-" -> -1.0
                coeffStr == "+" -> 1.0
                else -> coeffStr.toDoubleOrNull() ?: 1.0
            }
            power = pieces[1].toDoubleOrNull() ?: 1.0
        } else if (part.contains("x")) {
            val coeffStr = part.replace("x", "")
            coeff = when {
                coeffStr.isEmpty() -> 1.0
                coeffStr == "-" -> -1.0
                coeffStr == "+" -> 1.0
                else -> coeffStr.toDoubleOrNull() ?: 1.0
            }
            power = 1.0
        } else {
            continue
        }

        total += coeff * x.pow(power)
    }
    return total
}

// Find roots (where function crosses y = 0) in standard range (-10 to 10) to mark with coordinate dots
private fun findApproximateRoots(formula: String): List<Double> {
    val roots = mutableListOf<Double>()
    
    // Check quadratic form first (mathematically exact)
    // x^2 - 4 -> x = ±2
    if (formula == "x^2 - 4" || formula == "x² - 4") {
        return listOf(-2.0, 2.0)
    }
    if (formula == "x^2 - 5x + 6") {
        return listOf(2.0, 3.0)
    }
    
    // Use interval scanning with bisection to find up to 3 roots in (-10..10)
    var prevX = -10.0
    var prevY = evaluateFunction(formula, prevX)
    val step = 0.25
    
    for (i in 1..80) {
        val currX = -10.0 + i * step
        val currY = evaluateFunction(formula, currX)
        
        if (currY.isNaN() || prevY.isNaN()) {
            prevX = currX
            prevY = currY
            continue
        }
        
        // Root crossing discovered
        if (prevY * currY < 0) {
            // Find closer root via simple binary search
            var left = prevX
            var right = currX
            var root = currX
            for (j in 0..6) {
                val mid = (left + right) / 2
                val midY = evaluateFunction(formula, mid)
                if (abs(midY) < 0.001) {
                    root = mid
                    break
                }
                if (prevY * midY < 0) {
                    right = mid
                } else {
                    left = mid
                }
            }
            if (roots.none { abs(it - root) < 0.1 }) {
                roots.add(root)
            }
        } else if (abs(currY) < 0.001) {
            if (roots.none { abs(it - currX) < 0.1 }) {
                roots.add(currX)
            }
        }
        
        prevX = currX
        prevY = currY
    }
    return roots
}
