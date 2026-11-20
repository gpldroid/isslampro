package com.isslampro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val Green = Color(0xFF176B55)
private val Gold = Color(0xFFC69B45)
private val Cream = Color(0xFFF6F5EF)
private val Ink = Color(0xFF18352D)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(246,245,239)
        window.navigationBarColor = android.graphics.Color.rgb(246,245,239)
        window.decorView.layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL
        setContent { IslamProApp() }
    }
}

private data class Feature(val title: String, val subtitle: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val detail: String)

@Composable
fun IslamProApp() {
    var selected by remember { mutableStateOf("الرئيسية") }
    val items = listOf("القرآن", "الأذكار", "الصلاة", "الإذاعات", "المكتبة")
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = lightColorScheme(primary = Green, secondary = Gold, background = Cream, surface = Color.White)) {
            Scaffold(
                containerColor = Cream,
                bottomBar = {
                    NavigationBar(containerColor = Color.White) {
                        listOf("الرئيسية", "القرآن", "الأذكار", "الصلاة").forEach { label ->
                            NavigationBarItem(
                                selected = selected == label,
                                onClick = { selected = label },
                                icon = { Icon(when(label) { "الرئيسية" -> Icons.Default.Home; "القرآن" -> Icons.Default.MenuBook; "الأذكار" -> Icons.Default.Favorite; else -> Icons.Default.AccessTime }, null) },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            ) { padding ->
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("إسلام برو", color = Green, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                                Text("رفيقك اليومي للعبادة والمعرفة", color = Color.Gray, fontSize = 13.sp)
                            }
                            Surface(shape = RoundedCornerShape(14.dp), color = Color.White) {
                                IconButton(onClick = { selected = "الإعدادات" }) { Icon(Icons.Default.Settings, "الإعدادات", tint = Green) }
                            }
                        }
                    }
                    item { HeroCard() }
                    item {
                        Text(if (selected == "الرئيسية") "اكتشف خدمات التطبيق" else selected, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink)
                    }
                    val features = listOf(
                        Feature("القرآن الكريم", "المصحف والتلاوات", Icons.Default.MenuBook, "اقرأ القرآن الكريم وتابع التلاوات."),
                        Feature("مواقيت الصلاة", "أوقات الصلاة والتنبيهات", Icons.Default.AccessTime, "تعرض أوقات الصلاة وفق موقعك وإعدادات الحساب."),
                        Feature("الأذكار", "أذكار الصباح والمساء", Icons.Default.Favorite, "مجموعة من الأذكار اليومية."),
                        Feature("إذاعات القرآن", "استمع إلى التلاوات", Icons.Default.Radio, "إذاعات وتلاوات قرآنية مباشرة عند توفر الاتصال."),
                        Feature("التفسير والمكتبة", "التفسير والمحتوى المرئي", Icons.Default.LibraryBooks, "مكتبة إسلامية للقراءة والتعلم.")
                    )
                    items(features.size) { i ->
                        FeatureCard(features[i]) { selected = features[i].title }
                    }
                    item {
                        Text("إصدار أولي أصلي بنظام Android", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.fillMaxWidth().padding(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCard() {
    Surface(color = Green, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("اجعل يومك عامراً بالذكر والقرآن", color = Color.White.copy(alpha = .86f), fontSize = 14.sp)
            Surface(color = Gold, shape = RoundedCornerShape(12.dp)) {
                Text("ابدأ رحلتك", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp))
            }
        }
    }
}

@Composable
private fun FeatureCard(feature: Feature, onClick: () -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(18.dp), shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = Green.copy(alpha = .09f), shape = RoundedCornerShape(14.dp)) {
                Icon(feature.icon, null, tint = Green, modifier = Modifier.padding(13.dp).size(25.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(feature.title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(feature.subtitle, color = Color.Gray, fontSize = 12.sp)
            }
            Icon(Icons.Default.ChevronLeft, null, tint = Gold)
        }
    }
}
