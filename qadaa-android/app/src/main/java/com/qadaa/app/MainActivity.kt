package com.qadaa.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qadaa.app.data.QadaaData
import com.qadaa.app.domain.Cities
import com.qadaa.app.domain.DayPlan
import com.qadaa.app.domain.HijriRules
import com.qadaa.app.domain.Planner
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val Forest = Color(0xFF174B3A)
private val Gold = Color(0xFFC5A45B)
private val Cream = Color(0xFFF7F4EC)
private val Warn = Color(0xFF9B3D2E)
private val Ar = Locale("ar")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { QadaaApp() }
        }
    }
}

@Composable
fun QadaaApp(vm: QadaaViewModel = viewModel()) {
    val data by vm.data.collectAsState()
    MaterialTheme(colorScheme = lightColorScheme(primary = Forest, secondary = Gold, background = Cream, surface = Color.White)) {
        val loaded = data
        if (loaded == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Forest) }
        } else {
            QadaaScreen(loaded, vm)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QadaaScreen(data: QadaaData, vm: QadaaViewModel) {
    val today = LocalDate.now()
    var owedText by rememberSaveable { mutableStateOf(if (data.owed == 0) "" else data.owed.toString()) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showInfo by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var cityMenu by remember { mutableStateOf(false) }

    val city = Cities.byId(data.cityId)
    val deadline = data.deadline ?: HijriRules.defaultDeadline(today)
    val remaining = (data.owed - data.completed.size).coerceAtLeast(0)
    val plan = remember(today, deadline, remaining, data.completed, data.unavailable, city) {
        Planner.plan(today, deadline, remaining, data.completed, data.unavailable, city)
    }
    val rankOf = remember(plan) { plan.ranked.withIndex().associate { it.value.date to it.index + 1 } }
    val df = DateTimeFormatter.ofPattern("d MMMM yyyy", Ar)

    Scaffold(
        containerColor = Cream,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("قضاء", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Forest) },
                actions = { IconButton(onClick = { showInfo = true }) { Icon(Icons.Default.Info, contentDescription = "معلومات") } }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                listOf("الرئيسية", "خطة الأيام", "السجل").forEachIndexed { i, label ->
                    NavigationBarItem(selected = tab == i, onClick = { tab = i },
                        icon = { Icon(listOf(Icons.Default.Home, Icons.Default.CalendarMonth, Icons.Default.CheckCircle)[i], contentDescription = label) },
                        label = { Text(label) })
                }
            }
        }
    ) { padding ->
        when (tab) {
            0 -> LazyColumn(Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Forest), shape = RoundedCornerShape(22.dp)) {
                        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("خطة قضاء الصيام", color = Color.White.copy(alpha = .8f))
                            Text("عليك $remaining يوم", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                            Text("أتممت ${data.completed.size} من ${data.owed} يوم", color = Color.White)
                            LinearProgressIndicator(
                                progress = { if (data.owed == 0) 0f else (data.completed.size.toFloat() / data.owed).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth(), color = Gold, trackColor = Color.White.copy(alpha = .2f))
                        }
                    }
                }
                item {
                    Card(shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("إعداد خطة القضاء", fontWeight = FontWeight.Bold, color = Forest, fontSize = 18.sp)
                            OutlinedTextField(owedText, { v ->
                                owedText = v.filter { it.isDigit() }.take(3)
                                vm.setOwed((owedText.toIntOrNull() ?: 0).coerceIn(0, 500))
                            }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
                                modifier = Modifier.fillMaxWidth(), label = { Text("إجمالي أيام القضاء المطلوبة") },
                                supportingText = { Text("الأيام التي تسجّلها كمكتملة تُخصم تلقائيًا") })
                            Text("آخر موعد للقضاء", fontWeight = FontWeight.Medium)
                            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Event, contentDescription = null); Spacer(Modifier.width(6.dp))
                                Text(deadline.format(df))
                            }
                            if (data.deadline == null) Text("افتراضيًا: اليوم السابق لرمضان القادم (تاريخ هجري تقديري).", color = Color.Gray, fontSize = 12.sp)
                            Text("المدينة", fontWeight = FontWeight.Medium)
                            Box {
                                OutlinedButton(onClick = { cityMenu = true }, modifier = Modifier.fillMaxWidth()) {
                                    Icon(Icons.Default.Place, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("${city.nameAr}، مصر")
                                }
                                DropdownMenu(expanded = cityMenu, onDismissRequest = { cityMenu = false }) {
                                    Cities.all.forEach { c ->
                                        DropdownMenuItem(text = { Text(c.nameAr) }, onClick = { vm.setCity(c.id); cityMenu = false })
                                    }
                                }
                            }
                            Text("سيتم اختيار أقصر $remaining يوم متاح تلقائيًا.", color = Forest, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                item {
                    Text("أقرب يوم في الخطة", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Forest)
                    Spacer(Modifier.height(8.dp))
                    val next = plan.chosen.firstOrNull()
                    if (next != null) DayCard(next, rankOf[next.date] ?: 0, true, today, df,
                        onDone = { vm.markFasted(next.date) }, onExclude = { vm.exclude(next.date) })
                    else Text(if (remaining == 0) "لا توجد أيام متبقية عليك." else "لا توجد أيام متاحة ضمن الموعد المحدد.")
                }
                item {
                    Text("ملخص الخطة", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Forest)
                    Text("الأيام المتاحة: ${plan.ranked.size} • المختارة: ${plan.chosen.size} • البدائل: ${plan.backups.size}", color = Color.DarkGray)
                    if (plan.blockedByCalendar.isNotEmpty()) Text(
                        "استُبعدت أيام لا يجوز صيامها: " + plan.blockedByCalendar.entries.joinToString("، ") { "${it.key.labelAr} (${it.value})" },
                        color = Color.Gray, fontSize = 12.sp)
                    if (plan.shortage > 0) Text("تنبيه: الأيام المتاحة أقل من المطلوب بـ ${plan.shortage} يوم. وسّع الموعد أو راجع الأيام المستبعدة.", color = Warn)
                }
            }
            1 -> LazyColumn(Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Text("أقصر الأيام أولًا", fontWeight = FontWeight.Bold, fontSize = 21.sp, color = Forest) }
                item { Text("المدة من الفجر إلى المغرب لمدينة ${city.nameAr}. المواقيت تقديرية (حساب فلكي) وليست مواقيت رسمية؛ راجع إمساكية بلدك.", color = Color.Gray, fontSize = 12.sp) }
                item { Text("الأيام المختارة (بترتيب التاريخ)", fontWeight = FontWeight.Bold, color = Forest) }
                if (plan.chosen.isEmpty()) item { Text("لا توجد أيام مختارة.", color = Color.Gray) }
                items(plan.chosen, key = { "c" + it.date }) { day ->
                    DayCard(day, rankOf[day.date] ?: 0, true, today, df,
                        onDone = { vm.markFasted(day.date) }, onExclude = { vm.exclude(day.date) })
                }
                item { Text("البدائل التالية", fontWeight = FontWeight.Bold, color = Forest) }
                items(plan.backups, key = { "b" + it.date }) { day ->
                    DayCard(day, rankOf[day.date] ?: 0, false, today, df, onDone = null, onExclude = { vm.exclude(day.date) })
                }
                val excluded = data.unavailable.filter { !it.isBefore(today) }.sorted()
                if (excluded.isNotEmpty()) {
                    item { Text("أيام استبعدتها (${excluded.size})", fontWeight = FontWeight.Bold, color = Forest) }
                    items(excluded, key = { "x$it" }) { d ->
                        Card { Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(d.format(df))
                            TextButton(onClick = { vm.exclude(d, false) }) { Text("إرجاع") }
                        } }
                    }
                }
            }
            else -> LazyColumn(Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Text("سجل الصيام", fontWeight = FontWeight.Bold, fontSize = 21.sp, color = Forest) }
                item { Text("الأيام المكتملة: ${data.completed.size} • المتبقية: $remaining") }
                if (today !in data.completed && HijriRules.blockReason(today) == null) item {
                    Button(onClick = { vm.markFasted(today) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Forest)) {
                        Icon(Icons.Default.Check, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("سجّل صيام اليوم")
                    }
                }
                if (data.completed.isEmpty()) item { Text("لم تسجّل أي يوم بعد.", color = Color.Gray) }
                items(data.completed.sortedDescending(), key = { "d$it" }) { d ->
                    Card { Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("✓  ${d.format(df)}")
                        TextButton(onClick = { vm.markFasted(d, false) }) { Text("تراجع") }
                    } }
                }
                item { Text("البيانات محفوظة على هذا الجهاز وتبقى بعد إغلاق التطبيق.", color = Color.Gray, fontSize = 12.sp) }
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = deadline.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = {
                state.selectedDateMillis?.let { vm.setDeadline(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                showDatePicker = false
            }) { Text("حفظ") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("إلغاء") } }) { DatePicker(state) }
    }
    if (showInfo) AlertDialog(onDismissRequest = { showInfo = false }, confirmButton = { TextButton(onClick = { showInfo = false }) { Text("حسنًا") } },
        title = { Text("تنبيه مهم") },
        text = { Text("مواقيت الفجر والمغرب تقديرية: محسوبة فلكيًا بطريقة الهيئة المصرية العامة للمساحة (الفجر عند 19.5°) مع التوقيت الصيفي حسب ساعة الجهاز، وليست مواقيت رسمية، وقد تختلف دقيقة أو أكثر عن الإمساكية.\n\nالتاريخ الهجري (رمضان والعيدين) محسوب بتقويم أم القرى وقد يختلف يومًا عن إعلان دار الإفتاء.\n\nلا يتضمن التطبيق أحكام الفدية أو الكفارة؛ راجع دار الإفتاء المصرية في حالتك.") })
}

@Composable
fun DayCard(day: DayPlan, rank: Int, recommended: Boolean, today: LocalDate, df: DateTimeFormatter,
            onDone: (() -> Unit)?, onExclude: () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (recommended) Color(0xFFEAF2EC) else Color.White)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("${day.date.format(df)}  •  ${day.date.dayOfWeek.getDisplayName(TextStyle.FULL, Ar)}", fontWeight = FontWeight.Bold, color = Forest)
                    Text("الترتيب رقم $rank من حيث القِصَر", color = Color.Gray, fontSize = 12.sp)
                }
                Text("${day.minutes / 60} س ${day.minutes % 60} د", fontWeight = FontWeight.Bold, color = Forest, fontSize = 18.sp)
            }
            Text("الفجر ${day.times.fajr} • المغرب ${day.times.maghrib} (تقديري)", fontSize = 13.sp, color = Color.DarkGray)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                if (onDone != null && !day.date.isAfter(today)) Button(onClick = onDone, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Forest)) {
                    Icon(Icons.Default.Check, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("صمت هذا اليوم")
                }
                OutlinedButton(onClick = onExclude, modifier = Modifier.weight(1f)) { Text("لا أستطيع هذا اليوم") }
            }
        }
    }
}
