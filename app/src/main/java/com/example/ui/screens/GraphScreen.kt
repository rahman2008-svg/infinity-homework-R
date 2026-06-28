package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.HomeworkViewModel
import com.example.ui.components.InteractiveGraphView
import com.example.ui.theme.InfinityBlue
import com.example.ui.theme.InfinityOrange
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.OnSlateDark
import com.example.ui.theme.OnSlateSurface
import com.example.ui.theme.OnSlateCard

@Composable
fun GraphScreen(
    viewModel: HomeworkViewModel,
    modifier: Modifier = Modifier
) {
    val activeFunction by viewModel.graphFunction.collectAsState()
    var inputFunction by remember { mutableStateOf(activeFunction) }
    var zoomScale by remember { mutableStateOf(45f) } // Pixels per unit

    // Sync input field when viewmodel active function updates
    LaunchedEffect(activeFunction) {
        inputFunction = activeFunction
    }

    val presets = listOf(
        "x^2 - 4" to "Quadratic parabola",
        "sin(x)" to "Sine wave",
        "cos(x)" to "Cosine wave",
        "tan(x)" to "Tangent curve",
        "abs(x)" to "Absolute V-shape",
        "sqrt(x)" to "Square root curve"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 8.dp)
    ) {
        // 1. Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ShowChart,
                contentDescription = null,
                tint = InfinityOrange,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Interactive Grapher",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = OnSlateDark
                )
                Text(
                    text = "Drag grid to pan, use buttons to zoom",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }

        // 2. Interactive Input Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "y = ",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = InfinityOrange,
                        fontFamily = FontFamily.Monospace
                    )
                    
                    TextField(
                        value = inputFunction,
                        onValueChange = {
                            inputFunction = it
                            viewModel.setGraphFunction(it)
                        },
                        placeholder = { Text("e.g. x^2 - 4", color = Color.Gray) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("graph_function_input"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = OnSlateSurface,
                            unfocusedTextColor = OnSlateSurface
                        ),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Presets list row
                Text("QUICK FUNCTION PRESETS:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presets) { (presetFormula, desc) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (presetFormula == activeFunction) InfinityOrange.copy(alpha = 0.2f) else SlateCard)
                                .border(
                                    1.dp,
                                    if (presetFormula == activeFunction) InfinityOrange else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable {
                                    viewModel.setGraphFunction(presetFormula)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = presetFormula,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (presetFormula == activeFunction) InfinityOrange else OnSlateCard
                            )
                        }
                    }
                }
            }
        }

        // 3. Graph View Area with floating zoom controls
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF07080B))
                .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(16.dp))
        ) {
            // Live Interactive Graph Canvas
            InteractiveGraphView(
                functionStr = activeFunction,
                zoomScale = zoomScale
            )

            // Dynamic coordinates overlay panel
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .background(Color(0xCC000000), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(InfinityOrange, RoundedCornerShape(4.dp)))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "f(x) = $activeFunction",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Blue dots show approximate roots (y = 0)",
                        color = InfinityBlue,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Floating Zoom controllers
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingActionButton(
                    onClick = { if (zoomScale < 120f) zoomScale += 10f },
                    containerColor = SlateCard,
                    contentColor = InfinityOrange,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In")
                }

                FloatingActionButton(
                    onClick = { if (zoomScale > 20f) zoomScale -= 10f },
                    containerColor = SlateCard,
                    contentColor = InfinityOrange,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
                }
            }
        }
    }
}
