package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.HomeworkViewModel
import com.example.ui.theme.InfinityBlue
import com.example.ui.theme.InfinityOrange
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.OnSlateDark
import com.example.ui.theme.OnSlateSurface
import com.example.ui.theme.OnSlateCard

@Composable
fun CalculatorScreen(
    viewModel: HomeworkViewModel,
    modifier: Modifier = Modifier
) {
    val input by viewModel.keyboardInput.collectAsState()
    val activeResult by viewModel.activeResult.collectAsState()

    // Key definition layout
    val keyboardRows = listOf(
        listOf("x", "y", "d/dx(", "^", "√"),
        listOf("(", ")", ";", "AC", "⌫"),
        listOf("7", "8", "9", "*", "+"),
        listOf("4", "5", "6", "/", "-"),
        listOf("1", "2", "3", "=", ","),
        listOf("0", ".", "sin(", "cos(", "tan(")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 8.dp)
    ) {
        // 1. Title bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Infinity Calculator",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = OnSlateDark
                )
                Text(
                    text = "Scientific Rule-Based Solver",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            IconButton(
                onClick = { viewModel.setTab(3) }, // History Tab
                colors = IconButtonDefaults.iconButtonColors(containerColor = SlateCard)
            ) {
                Icon(Icons.Outlined.History, contentDescription = "History", tint = Color.LightGray)
            }
        }

        // 2. Main content view area (shows result steps or simple placeholder instructions)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            if (activeResult != null) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        ActiveEquationHeaderCard(
                            input = input,
                            finalAnswer = activeResult!!.finalAnswer,
                            type = activeResult!!.type,
                            supportsGraph = activeResult!!.supportsGraph,
                            graphParams = activeResult!!.graphParams,
                            viewModel = viewModel
                        )
                    }

                    item {
                        Text(
                            text = "STEP-BY-STEP SOLUTION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = InfinityOrange,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    itemsIndexed(activeResult!!.steps) { index, step ->
                        StepRowCard(index = index + 1, title = step.title, formula = step.formula, explanation = step.explanation)
                    }
                }
            } else {
                EmptyStateLayout(input = input, onSolve = { viewModel.solveFromKeyboard() })
            }
        }

        // 3. Mathematical visual screen display (grows beautifully)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0x1FFFFFFF))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (input.isEmpty()) "Enter mathematical formula..." else input,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (input.isEmpty()) Color.Gray else OnSlateSurface,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("calculator_display_text")
                )

                if (input.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.clearKeyboard() },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0x1FFFFFFF))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Clear Input", tint = Color.LightGray)
                    }
                }
            }
        }

        // 4. Custom physical grid math keyboard
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SlateSurface)
                .padding(8.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                keyboardRows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row.forEach { key ->
                            val isAction = key == "AC" || key == "⌫" || key == "="
                            val isVar = key == "x" || key == "y" || key == "d/dx("
                            val buttonColor = when {
                                key == "=" -> InfinityOrange
                                isAction -> Color(0xFFEADDFF)
                                isVar -> InfinityOrange.copy(alpha = 0.12f)
                                else -> SlateCard
                            }
                            val textColor = when {
                                key == "=" -> Color.White
                                isAction -> Color(0xFF21005D)
                                isVar -> InfinityOrange
                                else -> OnSlateCard
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(buttonColor)
                                    .clickable {
                                        when (key) {
                                            "AC" -> {
                                                viewModel.clearKeyboard()
                                                viewModel.solveEquation("", false) // resets active result
                                            }
                                            "⌫" -> viewModel.backspaceKeyboard()
                                            "=" -> viewModel.solveFromKeyboard()
                                            else -> viewModel.appendToKeyboard(key)
                                        }
                                    }
                                    .testTag("keyboard_key_$key"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (key == "⌫") {
                                    Icon(
                                        imageVector = Icons.Default.Backspace,
                                        contentDescription = "Backspace",
                                        tint = textColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Text(
                                        text = key,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveEquationHeaderCard(
    input: String,
    finalAnswer: String,
    type: String,
    supportsGraph: Boolean,
    graphParams: Map<String, Double>,
    viewModel: HomeworkViewModel
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, InfinityOrange.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SlateCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(InfinityOrange.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = type.uppercase(),
                        color = InfinityOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }

                Text(
                    text = "Rule Verified",
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Formula:",
                fontSize = 11.sp,
                color = Color.DarkGray
            )
            Text(
                text = input,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = OnSlateCard
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Final Answer:",
                fontSize = 11.sp,
                color = Color.DarkGray
            )
            Text(
                text = finalAnswer,
                fontSize = 24.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                color = InfinityOrange
            )

            if (supportsGraph) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val func = if (type == "Quadratic Equation") {
                            val a = graphParams["a"] ?: 1.0
                            val b = graphParams["b"] ?: 0.0
                            val c = graphParams["c"] ?: 0.0
                            val signB = if (b >= 0) "+$b" else "$b"
                            val signC = if (c >= 0) "+$c" else "$c"
                            "${a}x^2${signB}x${signC}"
                        } else {
                            val m = graphParams["m"] ?: 1.0
                            val c = graphParams["c"] ?: 0.0
                            "${m}x${if (c >= 0) "+$c" else "$c"}"
                        }
                        viewModel.setGraphFunction(func)
                        viewModel.setTab(2) // open graph tab
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = InfinityBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Plot & Interact on Coordinate Plane", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun StepRowCard(
    index: Int,
    title: String,
    formula: String,
    explanation: String
) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circle Step Number
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(InfinityBlue.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = index.toString(),
                        color = InfinityBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = OnSlateSurface,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Math formula box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SlateCard, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = formula,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = InfinityOrange,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = explanation,
                        fontSize = 12.sp,
                        color = Color.DarkGray,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateLayout(
    input: String,
    onSolve: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(InfinityOrange.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Keyboard, contentDescription = null, tint = InfinityOrange, modifier = Modifier.size(36.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Ready to Solve",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = OnSlateDark
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Use the keyboard below to type any arithmetic, algebra, fraction, quadratic, system of equations, or calculus derivatives formula, then tap '='.",
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
            lineHeight = 18.sp
        )

        if (input.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onSolve,
                colors = ButtonDefaults.buttonColors(containerColor = InfinityOrange),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Evaluate '$input'", fontWeight = FontWeight.Bold)
            }
        }
    }
}
