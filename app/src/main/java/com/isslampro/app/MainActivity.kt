package com.isslampro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

private val Deep = Color(0xFF082423)
private val Green = Color(0xFF176B55)
private val Gold = Color(0xFFC7A45B)
private val Cream = Color(0xFFF5F3EB)
private val Ink = Color(0xFF18352D)
private const val DATA = "https://raw.githubusercontent.com/gpldroid/isslampro/main/data/"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(8, 36, 35)
        window.navigationBarColor = android.graphics.Color.rgb(8, 36, 35)
        setContent {
            val player = remember { ExoPlayer.Builder(this).build() }
            DisposableEffect(Unit) { onDispose { player.release() } }
            IslamProApp(player)
        }
    }
}

private suspend fun fetchText(path: String): String = withContext(Dispatchers.IO) {
    URL(DATA + path).openConnection().apply {
        connectTimeout = 12000
        readTimeout = 12000
        setRequestProperty("User-Agent", "IslamPro-Android")
    }.getInputStream().bufferedReader(Charsets.UTF_8).use { it.readText() }
}

private data class Radio(val name: String, val url: String)
private data class Reciter(val id: Int, val name: String)
private data class Recitation(val name: String, val source: String, val chapters: List<Int>)
private data class Chapter(val number: Int, val name: String)

@Composable
fun IslamProApp(player: ExoPlayer) {
    var page by remember { mutableStateOf("الرئيسية") }
    var chapters by remember { mutableStateOf<List<Chapter>>(emptyList()) }
    var radios by remember { mutableStateOf<List<Radio>>(emptyList()) }
    var reciters by remember { mutableStateOf<List<Reciter>>(emptyList()) }
    var recitations by remember { mutableStateOf<List<Recitation>>(emptyList()) }
    var selectedReciter by remember { mutableStateOf<Reciter?>(null) }
    var selectedRadio by remember { mutableStateOf<Radio?>(null) }
    var playing by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var ayat by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedChapter by remember { mutableStateOf<Chapter?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var athkarCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        try {
            val meta = JSONObject(fetchText("meta.json"))
            val names = meta.getJSONArray("chapters")
            chapters = (0 until names.length()).map { Chapter(it + 1, names.getString(it)) }
        } catch (_: Exception) { error = "تعذر تحميل فهرس السور. تحقق من اتصال الإنترنت." }
        try {
            val arr = JSONArray(fetchText("radios.json"))
            radios = (0 until arr.length()).map { val a = arr.getJSONArray(it); Radio(a.getString(0), a.getString(1)) }
        } catch (_: Exception) { }
        try {
            val arr = JSONArray(fetchText("reciters.json"))
            reciters = (0 until arr.length()).map { val a = arr.getJSONArray(it); Reciter(a.getInt(0), a.getString(1)) }
        } catch (_: Exception) { }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = lightColorScheme(primary = Green, secondary = Gold, background = Cream, surface = Color.White)) {
            Scaffold(
                containerColor = Cream,
                bottomBar = {
                    NavigationBar(containerColor = Deep, contentColor = Color.White) {
                        listOf("الرئيسية", "القرآن", "الأذكار", "الصلاة", "المزيد").forEach { label ->
                            NavigationBarItem(
                                selected = page == label,
                                onClick = { page = label; selectedChapter = null },
                                icon = { Icon(when(label) {
                                    "الرئيسية" -> Icons.Default.Home
                                    "القرآن" -> Icons.Default.MenuBook
                                    "الأذكار" -> Icons.Default.Favorite
                                    "الصلاة" -> Icons.Default.AccessTime
                                    else -> Icons.Default.GridView
                                }, null) },
                                label = { Text(label, fontSize = 10.sp, maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = Gold, selectedTextColor = Gold, indicatorColor = Green, unselectedIconColor = Color.White.copy(.7f), unselectedTextColor = Color.White.copy(.7f))
                            )
                        }
                    }
                }
            ) { padding ->
                when (page) {
                    "القرآن" -> {
                        if (selectedChapter == null) {
                            Column(Modifier.fillMaxSize().padding(padding)) {
                                Header("القرآن الكريم", "فهرس سور القرآن الكريم")
                                SearchField(search, { search = it }, "ابحث عن سورة")
                                if (chapters.isEmpty()) LoadingOrError(loading, error)
                                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(chapters.filter { it.name.contains(search, true) || it.number.toString() == search }) { chapter ->
                                        Surface(color = Color.White, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().clickable {
                                            selectedChapter = chapter; loading = true; error = null; ayat = emptyList()
                                        }) {
                                            Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Text(chapter.number.toString(), color = Gold, fontWeight = FontWeight.Bold)
                                                Spacer(Modifier.width(14.dp))
                                                Text(chapter.name, modifier = Modifier.weight(1f), color = Ink, fontWeight = FontWeight.SemiBold)
                                                Text("فتح", color = Green, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            LaunchedEffect(selectedChapter) {
                                val ch = selectedChapter ?: return@LaunchedEffect
                                loading = true
                                try {
                                    val json = withContext(Dispatchers.IO) {
                                        JSONObject(URL("https://api.alquran.cloud/v1/surah/${ch.number}").readText())
                                    }
                                    val list = json.getJSONObject("data").getJSONArray("ayahs")
                                    ayat = (0 until list.length()).map { list.getJSONObject(it).getString("text") }
                                } catch (_: Exception) { error = "تعذر تحميل آيات السورة. حاول مرة أخرى." }
                                loading = false
                            }
                            Column(Modifier.fillMaxSize().padding(padding)) {
                                Row(Modifier.fillMaxWidth().background(Deep).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { selectedChapter = null; error = null }) { Icon(Icons.Default.ArrowForward, null, tint = Color.White) }
                                    Column {
                                        Text(selectedChapter!!.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                        Text("سورة رقم ${selectedChapter!!.number}", color = Gold, fontSize = 12.sp)
                                    }
                                }
                                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Gold)
                                if (error != null) Text(error!!, color = Color.Red, modifier = Modifier.padding(16.dp))
                                LazyColumn(contentPadding = PaddingValues(20.dp)) {
                                    item { Text("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp), textAlign = TextAlign.Center, color = Green, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                                    items(ayat.chunked(1).flatten()) { verse ->
                                        Text(verse + " ۞", modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp), textAlign = TextAlign.Center, color = Ink, fontSize = 24.sp, lineHeight = 48.sp)
                                    }
                                }
                            }
                        }
                    }
                    "القراء والتلاوات" -> {
                        Column(Modifier.fillMaxSize().padding(padding)) {
                            Header(if (selectedReciter == null) "القراء والتلاوات" else selectedReciter!!.name, "اختر القارئ والرواية ثم السورة")
                            if (selectedReciter == null) {
                                SearchField(search, { search = it }, "ابحث عن قارئ")
                                LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(reciters.filter { it.name.contains(search, true) }) { reciter ->
                                        Surface(color = Color.White, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().clickable { selectedReciter = reciter; loading = true; error = null }) {
                                            Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.RecordVoiceOver, null, tint = Green)
                                                Spacer(Modifier.width(12.dp))
                                                Text(reciter.name, color = Ink, modifier = Modifier.weight(1f))
                                                Icon(Icons.Default.ChevronLeft, null, tint = Gold)
                                            }
                                        }
                                    }
                                }
                            } else {
                                LaunchedEffect(selectedReciter) {
                                    val r = selectedReciter ?: return@LaunchedEffect
                                    loading = true
                                    try {
                                        val obj = JSONObject(fetchText("r/${r.id}.json"))
                                        val arr = obj.getJSONArray("recs")
                                        recitations = (0 until arr.length()).map { i ->
                                            val item = arr.getJSONObject(i)
                                            val ch = item.getJSONArray("ch")
                                            Recitation(item.getString("name"), item.getString("src"), (0 until ch.length()).map { ch.getInt(it) })
                                        }
                                    } catch (_: Exception) { error = "تعذر تحميل روايات القارئ." }
                                    loading = false
                                }
                                TextButton(onClick = { selectedReciter = null; recitations = emptyList() }) { Text("رجوع إلى القراء") }
                                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Gold)
                                if (error != null) Text(error!!, color = Color.Red, modifier = Modifier.padding(12.dp))
                                LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(recitations) { recitation ->
                                        Text(recitation.name, color = Green, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                                        recitation.chapters.forEach { number ->
                                            val chapter = chapters.firstOrNull { it.number == number }
                                            if (chapter != null) {
                                                Surface(color = Color.White, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable {
                                                    val url = when {
                                                        recitation.source.contains("%03d") -> recitation.source.replace("%03d", number.toString().padStart(3, '0'))
                                                        recitation.source.contains("%d") -> recitation.source.replace("%d", number.toString())
                                                        else -> recitation.source.trimEnd('/') + "/" + number.toString().padStart(3, '0') + ".mp3"
                                                    }
                                                    player.setMediaItem(MediaItem.fromUri(url)); player.prepare(); player.play()
                                                    selectedRadio = Radio("${selectedReciter!!.name} — ${chapter.name}", url)
                                                    playing = true
                                                }) {
                                                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.PlayCircle, null, tint = Gold)
                                                        Spacer(Modifier.width(10.dp))
                                                        Text(chapter.name, color = Ink, modifier = Modifier.weight(1f))
                                                        Text("استماع", color = Green, fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "الأذكار" -> {
                        Column(Modifier.fillMaxSize().padding(padding)) {
                            Header("الأذكار", "ذكر الله طمأنينة للقلب")
                            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(listOf("سبحان الله وبحمده","أستغفر الله وأتوب إليه","لا إله إلا الله وحده لا شريك له، له الملك وله الحمد وهو على كل شيء قدير","اللهم صل وسلم على نبينا محمد","سبحان الله، والحمد لله، ولا إله إلا الله، والله أكبر")) { dhikr ->
                                    Surface(color = Color.White, shape = RoundedCornerShape(18.dp)) {
                                        Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(dhikr, color = Ink, fontSize = 21.sp, textAlign = TextAlign.Center, lineHeight = 36.sp)
                                            Spacer(Modifier.height(12.dp))
                                            Button(onClick = { athkarCount++ }, colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("تسبيح  •  $athkarCount") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "الصلاة" -> {
                        var city by remember { mutableStateOf("الدار البيضاء") }
                        var latText by remember { mutableStateOf("33.5731") }
                        var lngText by remember { mutableStateOf("-7.5898") }
                        var methodKey by remember { mutableStateOf("morocco") }
                        var methodMenu by remember { mutableStateOf(false) }
                        val latitude = latText.toDoubleOrNull()
                        val longitude = lngText.toDoubleOrNull()
                        val prayerTimes = remember(latitude, longitude, methodKey) {
                            if (latitude != null && longitude != null && latitude in -90.0..90.0 && longitude in -180.0..180.0)
                                runCatching { PrayerCalculator.calculate(java.time.LocalDate.now(), latitude, longitude, methodKey) }.getOrNull()
                            else null
                        }
                        val zone = java.time.ZoneId.systemDefault()
                        Column(Modifier.fillMaxSize().padding(padding)) {
                            Header("مواقيت الصلاة", "حساب فلكي محلي مع طريقة حساب قابلة للتغيير")
                            Surface(color = Color.White, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("المدينة والإحداثيات", color = Green, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                    OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("اسم المدينة") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(value = latText, onValueChange = { latText = it }, label = { Text("خط العرض") }, singleLine = true, modifier = Modifier.weight(1f))
                                        OutlinedTextField(value = lngText, onValueChange = { lngText = it }, label = { Text("خط الطول") }, singleLine = true, modifier = Modifier.weight(1f))
                                    }
                                    Box {
                                        OutlinedButton(onClick = { methodMenu = true }, modifier = Modifier.fillMaxWidth()) {
                                            Text(PrayerCalculator.methods[methodKey]?.title ?: "طريقة الحساب")
                                        }
                                        DropdownMenu(expanded = methodMenu, onDismissRequest = { methodMenu = false }) {
                                            PrayerCalculator.methods.forEach { (key, method) ->
                                                DropdownMenuItem(text = { Text(method.title) }, onClick = { methodKey = key; methodMenu = false })
                                            }
                                        }
                                    }
                                    Text("مواقيت " + city + " — " + java.time.LocalDate.now(), color = Ink, fontWeight = FontWeight.SemiBold)
                                    if (prayerTimes == null) {
                                        Text("أدخل إحداثيات صحيحة لعرض المواقيت.", color = Color(0xFFB3261E))
                                    } else {
                                        val rows = listOf(
                                            "الفجر" to prayerTimes.fajr, "الشروق" to prayerTimes.sunrise,
                                            "الظهر" to prayerTimes.dhuhr, "العصر" to prayerTimes.asr,
                                            "المغرب" to prayerTimes.maghrib, "العشاء" to prayerTimes.isha
                                        )
                                        rows.forEach { (name, instant) ->
                                            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(name, color = Ink)
                                                Text(PrayerCalculator.format(instant, zone), color = Green, fontWeight = FontWeight.Bold)
                                            }
                                            Divider(color = Cream)
                                        }
                                    }
                                    Text("الافتراضي هو إحداثيات الدار البيضاء وطريقة المغرب. عدّل المدينة والإحداثيات حسب موقعك؛ الأوقات محسوبة محلياً وفق الخوارزمية الموجودة في الموقع الأصلي.", color = Color.Gray, fontSize = 11.sp, lineHeight = 18.sp)
                                }
                            }
                        }
                    }
                    "الإذاعات" -> {
                        Column(Modifier.fillMaxSize().padding(padding)) {
                            Header("إذاعات القرآن", "اختر إذاعة للاستماع المباشر")
                            if (radios.isEmpty()) LoadingOrError(false, "تعذر تحميل قائمة الإذاعات")
                            LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(radios) { radio ->
                                    Surface(color = Color.White, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().clickable {
                                        try {
                                            player.setMediaItem(MediaItem.fromUri(radio.url)); player.prepare(); player.play()
                                            selectedRadio = radio; playing = true; page = "الإذاعات"
                                        } catch (_: Exception) { }
                                    }) {
                                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Radio, null, tint = Green)
                                            Spacer(Modifier.width(12.dp))
                                            Text(radio.name, modifier = Modifier.weight(1f), color = Ink)
                                            Icon(if (selectedRadio == radio && playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Gold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "المزيد" -> {
                        Column(Modifier.fillMaxSize().padding(padding)) {
                            Header("المزيد من إسلام برو", "أقسام الموقع الأصلي")
                            listOf("إذاعات القرآن","القراء والتلاوات","التفسير","المكتبة الإسلامية","الابتهالات","الإعدادات").forEach { name ->
                                Surface(color = Color.White, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp).clickable { page = if (name == "إذاعات القرآن") "الإذاعات" else name }) {
                                    Text(name, modifier = Modifier.padding(18.dp), color = Ink, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                    else -> {
                        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            item {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text("إسلام برو", color = Green, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                                        Text("القرآن الكريم من أي مكان", color = Color.Gray, fontSize = 13.sp)
                                    }
                                    Icon(Icons.Default.MenuBook, null, tint = Gold, modifier = Modifier.size(38.dp))
                                }
                            }
                            item {
                                Surface(color = Deep, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", color = Gold, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                                        Text("اجعل يومك عامراً بالذكر والقرآن", color = Color.White, fontSize = 15.sp)
                                        Text("المصحف • التلاوات • الأذكار • الإذاعات", color = Color.White.copy(.75f), fontSize = 12.sp)
                                    }
                                }
                            }
                            item { Text("أقسام التطبيق", color = Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold) }
                            items(listOf("القرآن","الأذكار","الصلاة","الإذاعات","المزيد")) { name ->
                                Surface(color = Color.White, shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth().clickable { page = name }) {
                                    Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(when(name) { "القرآن" -> Icons.Default.MenuBook; "الأذكار" -> Icons.Default.Favorite; "الصلاة" -> Icons.Default.AccessTime; "الإذاعات" -> Icons.Default.Radio; else -> Icons.Default.GridView }, null, tint = Green)
                                        Spacer(Modifier.width(14.dp))
                                        Text(name, color = Ink, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        Icon(Icons.Default.ChevronLeft, null, tint = Gold)
                                    }
                                }
                            }
                            if (selectedRadio != null) item {
                                Surface(color = Green, shape = RoundedCornerShape(18.dp)) {
                                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text("يُبث الآن", color = Color.White.copy(.7f), fontSize = 11.sp)
                                            Text(selectedRadio!!.name, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                        IconButton(onClick = { if (playing) player.pause() else player.play(); playing = !playing }) {
                                            Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.White)
                                        }
                                    }
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
private fun Header(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().background(Deep).padding(horizontal = 20.dp, vertical = 22.dp)) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 23.sp)
        Text(subtitle, color = Gold, fontSize = 12.sp)
    }
}

@Composable
private fun SearchField(value: String, onValue: (String) -> Unit, hint: String) {
    Surface(color = Color.White, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, null, tint = Green)
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) Text(hint, color = Color.Gray, fontSize = 14.sp)
                BasicTextField(value, onValue, singleLine = true, textStyle = LocalTextStyle.current.copy(color = Ink, fontSize = 14.sp), modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun LoadingOrError(loading: Boolean, error: String?) {
    if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Gold)
    if (error != null) Text(error, color = Color.Gray, modifier = Modifier.padding(16.dp))
}
