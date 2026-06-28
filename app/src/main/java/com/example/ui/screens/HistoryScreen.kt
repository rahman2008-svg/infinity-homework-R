package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
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
import com.example.data.SavedEquation
import com.example.ui.HomeworkViewModel
import com.example.ui.theme.AccentGold
import com.example.ui.theme.InfinityOrange
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.OnSlateDark
import com.example.ui.theme.OnSlateSurface
import com.example.ui.theme.OnSlateCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    viewModel: HomeworkViewModel,
    modifier: Modifier = Modifier
) {
    val history by viewModel.historyState.collectAsState()
    val favorites by viewModel.favoritesState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(0) } // 0: All, 1: Starred
    var showClearConfirm by remember { mutableStateOf(false) }

    val activeList = if (selectedCategory == 0) history else favorites
    val filteredList = activeList.filter {
        it.equation.contains(searchQuery, ignoreCase = true) ||
        it.result.contains(searchQuery, ignoreCase = true)
    }

    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US) }

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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "My Saved Solutions",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = OnSlateDark
                )
                Text(
                    text = "Offline database repository",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            if (history.isNotEmpty()) {
                TextButton(
                    onClick = { showClearConfirm = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red),
                    modifier = Modifier.testTag("clear_history_button")
                ) {
                    Icon(Icons.Outlined.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 2. Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search equations or results...", color = Color.Gray, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .testTag("history_search_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SlateSurface,
                unfocusedContainerColor = SlateSurface,
                focusedBorderColor = InfinityOrange,
                unfocusedBorderColor = Color(0x1F000000),
                focusedTextColor = OnSlateSurface,
                unfocusedTextColor = OnSlateSurface
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        // 3. Category Filter Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterTabButton(
                text = "All History (${history.size})",
                selected = selectedCategory == 0,
                onClick = { selectedCategory = 0 },
                modifier = Modifier.weight(1f)
            )

            FilterTabButton(
                text = "Favorites (${favorites.size})",
                selected = selectedCategory == 1,
                onClick = { selectedCategory = 1 },
                modifier = Modifier.weight(1f)
            )
        }

        // 4. Solutions List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (filteredList.isEmpty()) {
                HistoryEmptyState(
                    queryActive = searchQuery.isNotEmpty(),
                    categoryActive = selectedCategory == 1
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        EquationHistoryItemCard(
                            item = item,
                            dateStr = dateFormatter.format(Date(item.timestamp)),
                            onSelect = {
                                viewModel.onKeyboardInputChanged(item.equation)
                                viewModel.selectHistoryItem(item)
                                viewModel.setTab(1) // switch to edit details tab
                            },
                            onToggleFav = { viewModel.toggleFavorite(item.id, !item.isFavorite) },
                            onDelete = { viewModel.deleteEquation(item.id) },
                            modifier = Modifier.animateItemPlacement()
                        )
                    }
                }
            }
        }
    }

    // Confirmation Popup for clearing all history
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear Entire History?", fontWeight = FontWeight.Bold, color = OnSlateSurface) },
            text = { Text("Are you absolutely sure? This will remove all previously solved mathematical equations and starred favorites offline database. This operation is irreversible.", color = Color.DarkGray) },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirm = false
                        viewModel.clearAllHistory()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel", color = InfinityOrange)
                }
            },
            containerColor = SlateSurface
        )
    }
}

@Composable
fun FilterTabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) InfinityOrange else SlateSurface)
            .border(
                1.dp,
                if (selected) Color.Transparent else Color(0x1F000000),
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color.DarkGray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun EquationHistoryItemCard(
    item: SavedEquation,
    dateStr: String,
    onSelect: () -> Unit,
    onToggleFav: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x0FFFFFFF), RoundedCornerShape(14.dp))
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left equation type indicator badge
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(InfinityOrange.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (item.type) {
                        "arithmetic" -> Icons.Default.PlusOne
                        "fractions" -> Icons.Default.VerticalSplit
                        "linear equation" -> Icons.Default.LinearScale
                        "quadratic equation" -> Icons.Default.Functions
                        "system of equations" -> Icons.Default.Layers
                        "calculus" -> Icons.Default.AutoMode
                        else -> Icons.Default.Calculate
                    },
                    contentDescription = null,
                    tint = InfinityOrange,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text detail column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.equation,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = OnSlateSurface
                )
                
                Spacer(modifier = Modifier.height(3.dp))
                
                Text(
                    text = "= " + item.result,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = dateStr,
                    fontSize = 9.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action row (Favorite Star + Trash delete)
            IconButton(onClick = onToggleFav) {
                Icon(
                    imageVector = if (item.isFavorite) Icons.Outlined.Star else Icons.Outlined.StarOutline,
                    contentDescription = "Star",
                    tint = if (item.isFavorite) AccentGold else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.Gray.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun HistoryEmptyState(
    queryActive: Boolean,
    categoryActive: Boolean
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(InfinityOrange.copy(alpha = 0.05f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (categoryActive) Icons.Outlined.StarOutline else Icons.Default.History,
                contentDescription = null,
                tint = InfinityOrange.copy(alpha = 0.7f),
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = when {
                queryActive -> "No matching solutions"
                categoryActive -> "No starred equations"
                else -> "History is empty"
            },
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = OnSlateDark
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = when {
                queryActive -> "Verify spelling or try entering a simpler term."
                categoryActive -> "Star formulas during calculations to save them here for offline homework study."
                else -> "Scan or enter a formula to see your step-by-step history logs."
            },
            fontSize = 11.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
            lineHeight = 16.sp
        )
    }
}
