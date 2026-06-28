package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.HomeworkViewModel
import com.example.ui.theme.InfinityOrange
import com.example.ui.theme.InfinityBlue
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.OnSlateDark
import com.example.ui.theme.OnSlateSurface
import com.example.ui.theme.OnSlateCard

@Composable
fun AboutScreen(
    viewModel: HomeworkViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Helper function to launch URL/Intents safely
    fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlateSurface)
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("about_screen_container")
    ) {
        // App Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(InfinityOrange.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "About Info",
                    tint = InfinityOrange,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "About",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = OnSlateDark
                )
                Text(
                    text = "Developer & Publisher Profile",
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
            }
        }

        // Developer Profile Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("developer_profile_card"),
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Modern styled initials avatar
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(InfinityOrange, InfinityBlue)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "PA",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Prince AR Abdur Rahman",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = OnSlateCard
                        )
                        Text(
                            text = "Independent App Developer",
                            fontSize = 13.sp,
                            color = InfinityOrange,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Independent App Developer passionate about building modern Android applications, productivity tools, AI-powered experiences, media players, educational apps, and next-generation digital products.",
                    fontSize = 13.sp,
                    color = Color.DarkGray,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))
                
                Divider(color = Color(0x1F000000), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "CONNECT WITH DEVELOPER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = InfinityBlue,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // WhatsApp 1
                ContactRow(
                    icon = Icons.Default.Phone,
                    title = "WhatsApp: 01707424006",
                    onClick = { openUrl("https://wa.me/8801707424006") }
                )

                // WhatsApp 2
                ContactRow(
                    icon = Icons.Default.Phone,
                    title = "WhatsApp: 01796951709",
                    onClick = { openUrl("https://wa.me/8801796951709") }
                )

                // Facebook
                ContactRow(
                    icon = Icons.Default.Share,
                    title = "Facebook Profile",
                    onClick = { openUrl("https://www.facebook.com/share/1BNn32qoJo/") }
                )

                // Instagram
                ContactRow(
                    icon = Icons.Default.AccountCircle,
                    title = "Instagram Profile",
                    onClick = { openUrl("https://www.instagram.com/ur___abdur____rahman__2008") }
                )
            }
        }

        // About Company Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("company_profile_card"),
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(InfinityBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = "Company Info",
                            tint = InfinityBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "NexVora Lab's Ofc",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = OnSlateCard
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "NexVora Lab's Ofc focuses on creating innovative Android applications designed to improve productivity, entertainment, learning, and digital experiences.",
                    fontSize = 13.sp,
                    color = Color.DarkGray,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color(0x1F000000), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Row {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(InfinityOrange.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Mission",
                            tint = InfinityOrange,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "MISSION",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = InfinityOrange,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Build fast, beautiful, privacy-friendly, and user-focused applications accessible to everyone.",
                            fontSize = 12.sp,
                            color = Color.DarkGray,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // Technical Info, Credits, & Copyright Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .testTag("credits_info_card"),
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "TECHNICAL INFORMATION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                TechnicalInfoRow(label = "Application Version", value = "1.0.0")

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color(0x1F000000), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "CREDITS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                TechnicalInfoRow(label = "Developed by", value = "Prince AR Abdur Rahman")
                TechnicalInfoRow(label = "Published by", value = "NexVora Lab's Ofc")

                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "© 2026 NexVora Lab's Ofc. All Rights Reserved.",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ContactRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = InfinityOrange,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            color = OnSlateCard,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = "Go",
            tint = Color.Gray,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun TechnicalInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.DarkGray
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = OnSlateCard
        )
    }
}
