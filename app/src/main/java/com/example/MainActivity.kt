package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.VesselFlight
import com.example.ui.MaritimeViewModel
import com.example.ui.MockNotification
import com.example.ui.SecurityStatus
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MaritimeViewModel = viewModel()) {
    // Collect Flow States
    val waveHeight by viewModel.waveHeight.collectAsStateWithLifecycle()
    val windSpeed by viewModel.windSpeed.collectAsStateWithLifecycle()
    val tideLevel by viewModel.tideLevel.collectAsStateWithLifecycle()
    val seaTemp by viewModel.seaTemperature.collectAsStateWithLifecycle()
    val lastUpdatedLabel by viewModel.lastUpdatedTime.collectAsStateWithLifecycle()
    val countdown by viewModel.secondsToNextUpdate.collectAsStateWithLifecycle()
    
    val selectedClass by viewModel.selectedClass.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val flights by viewModel.flights.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    
    val isExtremeAlert by viewModel.isExtremeAlertActive.collectAsStateWithLifecycle()
    val alarmReason by viewModel.extremeIncidentReason.collectAsStateWithLifecycle()
    val isVoiceRinging by viewModel.isVoiceCallSimulating.collectAsStateWithLifecycle()
    val ringStatus by viewModel.voiceCallStatus.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0: Dashboard, 1: Flights, 2: Terminal & Alert

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("main_screen"),
        bottomBar = {
            Column {
                HorizontalDivider(color = Color(0xFFE7E0EC), thickness = 1.dp)
                NavigationBar(
                    containerColor = Color(0xFFF3EDF7),
                    modifier = Modifier.height(72.dp).testTag("bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Waves, contentDescription = "Sea State Dashboard") },
                        label = { Text("海象看板", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF6750A4),
                            selectedTextColor = Color(0xFF6750A4),
                            indicatorColor = Color(0xFFE8DEF8),
                            unselectedIconColor = Color(0xFF49454F),
                            unselectedTextColor = Color(0xFF49454F)
                        ),
                        modifier = Modifier.testTag("nav_tab_dashboard")
                    )
                    NavigationBarItem(
                        icon = { 
                            BadgeBox(count = flights.count { it.bookingStatus == "Active" && viewModel.getStatusForClass(it.shipClass, waveHeight, windSpeed) !is SecurityStatus.GREEN }) {
                                Icon(Icons.Default.DirectionsBoat, contentDescription = "Flights Dynamic")
                            } 
                        },
                        label = { Text("航班動態", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF6750A4),
                            selectedTextColor = Color(0xFF6750A4),
                            indicatorColor = Color(0xFFE8DEF8),
                            unselectedIconColor = Color(0xFF49454F),
                            unselectedTextColor = Color(0xFF49454F)
                        ),
                        modifier = Modifier.testTag("nav_tab_flights")
                    )
                    NavigationBarItem(
                        icon = { 
                            BadgeBox(count = notifications.size) {
                                Icon(Icons.Default.Notifications, contentDescription = "Notification Center")
                            } 
                        },
                        label = { Text("客務與定位", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF6750A4),
                            selectedTextColor = Color(0xFF6750A4),
                            indicatorColor = Color(0xFFE8DEF8),
                            unselectedIconColor = Color(0xFF49454F),
                            unselectedTextColor = Color(0xFF49454F)
                        ),
                        modifier = Modifier.testTag("nav_tab_notifications")
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(OceanDarkBackground)
                .padding(innerPadding)
        ) {
            when (activeTab) {
                0 -> DashboardTab(
                    viewModel = viewModel,
                    waveHeight = waveHeight,
                    windSpeed = windSpeed,
                    tideLevel = tideLevel,
                    seaTemp = seaTemp,
                    lastUpdatedLabel = lastUpdatedLabel,
                    countdown = countdown,
                    selectedClass = selectedClass,
                    flights = flights
                )
                1 -> FlightsTab(
                    viewModel = viewModel,
                    flights = flights,
                    waveHeight = waveHeight,
                    windSpeed = windSpeed,
                    searchQuery = searchQuery
                )
                2 -> TerminalAndNotificationsTab(
                    viewModel = viewModel,
                    notifications = notifications,
                    waveHeight = waveHeight,
                    windSpeed = windSpeed,
                    tideLevel = tideLevel,
                    isExtremeAlert = isExtremeAlert,
                    alarmReason = alarmReason
                )
            }

            // Global Voice Call Simulation Overlay
            if (isVoiceRinging) {
                SimulatedVoiceCallOverlay(
                    status = ringStatus,
                    reason = alarmReason,
                    onAnswer = { viewModel.answerSimulatedVoiceCall() },
                    onDecline = { viewModel.endSimulatedVoiceCall() }
                )
            }
        }
    }
}

@Composable
fun BadgeBox(count: Int, content: @Composable () -> Unit) {
    Box(propagateMinConstraints = true) {
        content()
        if (count > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .background(ColorRed, CircleShape)
                    .size(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = count.toString(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 0: DASHBOARD & OCEAN SIMULATOR
// -------------------------------------------------------------
@Composable
fun DashboardTab(
    viewModel: MaritimeViewModel,
    waveHeight: Double,
    windSpeed: Double,
    tideLevel: Double,
    seaTemp: Double,
    lastUpdatedLabel: String,
    countdown: Int,
    selectedClass: String,
    flights: List<VesselFlight>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Premium Header (Bento Style)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Circle Anchor Avatar
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(BentoPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Anchor,
                        contentDescription = "Anchor Anchor",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "台中港航安系統",
                        color = BentoDarkText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(
                        text = "數據已即時同步更新",
                        color = BentoMutedText,
                        fontSize = 12.sp
                    )
                }
            }
            
            // Manual/auto refresh countdown button mapped beautifully
            Button(
                onClick = { viewModel.refreshSeaStatePreserving() },
                colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.testTag("refresh_marine_data_button"),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Manual Refresh", modifier = Modifier.size(14.dp))
                    Text("${countdown / 60}m${countdown % 60}s", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Main Risk Status Card (Bento Style #f9dedc rounded-3xl)
        val currentStatus = viewModel.getStatusForClass(selectedClass, waveHeight, windSpeed)
        val cardBg: Color
        val cardBorder: Color
        val textCol: Color
        val badgeBg: Color
        val badgeText: String
        val statusTitle: String

        when (currentStatus) {
            is SecurityStatus.GREEN -> {
                cardBg = Color(0xFFE8F5E9)
                cardBorder = Color(0xFFA5D6A7)
                textCol = Color(0xFF1B5E20)
                badgeBg = Color(0xFF2E7D32)
                badgeText = "安全通行"
                statusTitle = "海象平穩開航"
            }
            is SecurityStatus.YELLOW -> {
                cardBg = Color(0xFFFFF8E1)
                cardBorder = Color(0xFFFFD54F)
                textCol = Color(0xFFB78103)
                badgeBg = Color(0xFFFFA000)
                badgeText = "舒適加強"
                statusTitle = "舒適乘客提醒"
            }
            is SecurityStatus.RED -> {
                cardBg = BentoWarningBg
                cardBorder = BentoWarningBorder
                textCol = BentoWarningText
                badgeBg = BentoWarningPrimary
                badgeText = "停航警戒"
                statusTitle = "航班停開管制"
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, cardBorder, RoundedCornerShape(28.dp)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Badge Text
                    Box(
                        modifier = Modifier
                            .background(badgeBg, RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = when (selectedClass) {
                            "A" -> "A類 大型客輪"
                            "B" -> "B類 中型遊艇"
                            else -> "C類 水上活動"
                        },
                        color = textCol,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = statusTitle,
                    color = textCol,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    lineHeight = 38.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = currentStatus.text,
                    color = textCol.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.testTag("safety_light_text")
                )
            }
        }

        // Weather Metrics side-by-side Bento Row (Wave height & Wind Speed grid elements!)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Weather Metric: Wave (Column span 2, row span 2 equivalent inside half Row weight)
            Card(
                colors = CardDefaults.cardColors(containerColor = BentoWaveBg),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, BentoWaveBorder, RoundedCornerShape(24.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(130.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(BentoPrimary, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Waves,
                            contentDescription = "Wave Icon",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "預測波高",
                            color = BentoMutedText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${String.format("%.2f", waveHeight)}m",
                            color = BentoWaveText,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val waveTrend = if (waveHeight >= 3.0) "↑ 紅燈停航" else if (waveHeight >= 2.0) "↑ 舒適警戒" else "✓ 海象平穩"
                        Text(
                            text = waveTrend,
                            color = BentoWaveText.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Weather Metric: Wind (Column span 2, row span 2 equivalent inside half Row weight)
            Card(
                colors = CardDefaults.cardColors(containerColor = BentoWindBg),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, BentoWindBorder, RoundedCornerShape(24.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(130.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(BentoSecondary, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Air,
                            contentDescription = "Wind Icon",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "平均風速",
                            color = BentoMutedText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${String.format("%.1f", windSpeed)} m/s",
                            color = BentoWindText,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val windTrend = if (windSpeed >= 13.0) "⚠️ 強風管制" else "低於管制標準"
                        Text(
                            text = windTrend,
                            color = BentoWindText.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Quick Weather Presets Switcher (styled as premium pill buttons row)
        Column {
            Text(
                text = "氣象模擬快捷 (快速測試燈號連動):",
                color = BentoDarkText,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SeaPresetChip("夏日平穩 (綠燈)", "calm") { viewModel.setSeaPreset("calm") }
                SeaPresetChip("春秋微湧 (B/C警戒)", "moderate") { viewModel.setSeaPreset("moderate") }
                SeaPresetChip("客輪黃警 (2.4m)", "rough_A_yellow") { viewModel.setSeaPreset("rough_A_yellow") }
                SeaPresetChip("客輪停航 (3.3m)", "rough_A_red") { viewModel.setSeaPreset("rough_A_red") }
                SeaPresetChip("逆風加劇 (14.5m/s)", "wind_heavy") { viewModel.setSeaPreset("wind_heavy") }
            }
        }

        // Dynamic Interactive Sliders to adjust live sea-state
        Card(
            colors = CardDefaults.cardColors(containerColor = BentoSurface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BentoBorder, RoundedCornerShape(24.dp)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("即時海氣象手動調整儀 (Evaluator Sliders)", color = BentoDarkText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))
                
                // Wave Height Control
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("示範波高: ", color = BentoMutedText, fontSize = 13.sp, modifier = Modifier.width(70.dp))
                    Text("${String.format("%.2f", waveHeight)} m", color = BentoPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.width(65.dp))
                    Slider(
                        value = waveHeight.toFloat(),
                        onValueChange = { viewModel.updateWaveHeight(it.toDouble()) },
                        valueRange = 0.2f..5.0f,
                        modifier = Modifier.weight(1f).testTag("wave_height_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = BentoPrimary,
                            activeTrackColor = BentoPrimary,
                            inactiveTrackColor = BentoBorder.copy(alpha = 0.4f)
                        )
                    )
                }
                
                // Wind Speed Control
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("平均風速: ", color = BentoMutedText, fontSize = 13.sp, modifier = Modifier.width(70.dp))
                    Text("${String.format("%.1f", windSpeed)} m/s", color = BentoSecondary, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.width(65.dp))
                    Slider(
                        value = windSpeed.toFloat(),
                        onValueChange = { viewModel.updateWindSpeed(it.toDouble()) },
                        valueRange = 1.0f..25.0f,
                        modifier = Modifier.weight(1f).testTag("wind_speed_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = BentoSecondary,
                            activeTrackColor = BentoSecondary,
                            inactiveTrackColor = BentoBorder.copy(alpha = 0.4f)
                        )
                    )
                }
            }
        }

        // Quick Action: Vessel Selection (Bento switch panel)
        Card(
            colors = CardDefaults.cardColors(containerColor = BentoSurface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BentoBorder, RoundedCornerShape(24.dp)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "船型分級耐浪警戒監控選擇",
                    color = BentoMutedText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        Triple("A", "A類大船", Icons.Default.DirectionsBoat),
                        Triple("B", "B類遊艇", Icons.Default.DirectionsBoat), // Keep simple sailing style boat
                        Triple("C", "C類水上", Icons.Default.Waves)
                    ).forEachIndexed { index, (code, name, icon) ->
                        val isSelected = selectedClass == code
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { viewModel.updateClassSelected(code) }
                                .padding(8.dp)
                                .weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        if (isSelected) BentoPrimary else Color.Transparent,
                                        CircleShape
                                    )
                                    .border(
                                        width = if (isSelected) 0.dp else 1.dp,
                                        color = if (isSelected) Color.Transparent else BentoBorder,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = name,
                                    tint = if (isSelected) Color.White else BentoMutedText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                name,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) BentoPrimary else BentoMutedText
                            )
                        }
                        if (index < 2) {
                            Spacer(
                                modifier = Modifier
                                    .height(30.dp)
                                    .width(1.dp)
                                    .background(BentoBorder.copy(alpha = 0.5f))
                            )
                        }
                    }
                }
            }
        }

        // Active Reservation Card (Upcoming Flight Bento Card template)
        val activeFlight = flights.firstOrNull { it.bookingStatus == "Active" }
        if (activeFlight != null) {
            val safetyStatus = viewModel.getStatusForClass(activeFlight.shipClass, waveHeight, windSpeed)
            Card(
                colors = CardDefaults.cardColors(containerColor = BentoSurface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BentoBorder, RoundedCornerShape(24.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "我的預約",
                            color = BentoPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "航班 ${activeFlight.flightNumber}",
                            color = BentoDarkText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Button(
                        onClick = {
                            if (safetyStatus is SecurityStatus.GREEN) {
                                // fine
                            } else {
                                viewModel.processRebookForFlight(activeFlight.id)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (safetyStatus is SecurityStatus.GREEN) BentoSecondary else BentoWarningPrimary
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (safetyStatus is SecurityStatus.GREEN) "正常起航" else "申請改簽",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Specific Safety standard detail text
        Card(
            colors = CardDefaults.cardColors(containerColor = BentoSurface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BentoBorder, RoundedCornerShape(24.dp)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Info, contentDescription = "History context", tint = BentoPrimary, modifier = Modifier.size(18.dp))
                Column {
                    val historicalText = when (selectedClass) {
                        "A" -> "【大船海陸聯動對策】此類萬噸交通輪或雙體快輪，耐浪係數經台中港24-25數據分析評估，安全航行波高放寬至 > 3.0m。冬季出航機率大幅提高，但注意波高 2.0m - 3.0m 時會觸發大船搭乘舒適度警告 (舒適客服改簽)。平均風速若 ≥ 13.0 m/s 則會觸發強制側風關閉機制，保障接駁靠泊安全。"
                        "B" -> "【中型遊艇觀光指標】此款娛樂漁船或海上遊客帆船，耐浪承受限制約為 2m。根據歷史天氣，台中港 4 至 8 月微浪率接近 100%，最適合辦理外海遊程及垂釣；冬季因強烈東北季風，常態性落入 2.0m 以上之紅燈禁止出航狀態。"
                        else -> "【小型無動力安全細則】包含獨木舟、SUP或無動力小帆船，耐浪臨界極低。僅 6 至 8 月最適合在台中港屏障區內（波高 ≤ 0.8m）舉行。超過 1.2m 強制停止航渡、封閉海域，避免外海側流帶離安全視線。"
                    }
                    Text(
                        text = historicalText,
                        color = BentoMutedText,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Live Station Buoy Telemetry visualization
        Card(
            colors = CardDefaults.cardColors(containerColor = BentoSurface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BentoBorder, RoundedCornerShape(24.dp)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("台中外海即時觀測浮標 (Live Buoy Station)", color = BentoDarkText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TelemetryItem(icon = Icons.Default.Thermostat, title = "海域水溫", value = "${String.format("%.1f", seaTemp)} °C")
                    TelemetryItem(icon = Icons.Default.Water, title = "潮位計水深", value = "${String.format("%.1f", tideLevel)} m")
                    TelemetryItem(icon = Icons.Default.Air, title = "預測突發瞬間陣風", value = "${String.format("%.0f", windSpeed * 1.25)} m/s (約${if (windSpeed * 1.25 < 13.8) "5-6" else if (windSpeed * 1.25 < 20.8) "7-8" else "9"}級)")
                }
            }
        }

        // Forecasting Waves And Wind values over 7 days in a Canvas Spline graph
        Card(
            colors = CardDefaults.cardColors(containerColor = BentoSurface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BentoBorder, RoundedCornerShape(24.dp)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("台中港未來7天預測海象趨勢圖 (Forecast Canvas)", color = BentoDarkText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("安全波高切線 (紅色虛線指標為 3.0m)", color = BentoMutedText, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(14.dp))
                
                // Canvas Graph
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(BentoBg, RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        
                        val dataPoints = listOf(1.2f, 1.8f, 2.5f, 2.9f, 3.2f, 1.9f, 1.4f)
                        val stepX = width / (dataPoints.size - 1)
                        val maxScale = 5.0f

                        // Draw Grid lines
                        drawLine(
                            color = BentoBorder,
                            start = Offset(0f, height * 0.4f), // 3.0m line (since 3.0 / 5.0 = 0.6 down, 1-0.6 = 0.4)
                            end = Offset(width, height * 0.4f),
                            strokeWidth = 2f,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                        
                        val path = Path()
                        dataPoints.forEachIndexed { i, value ->
                            val x = i * stepX
                            val y = height * (1.0f - (value / maxScale))
                            if (i == 0) {
                                path.moveTo(x, y)
                            } else {
                                path.lineTo(x, y)
                            }
                        }

                        drawPath(
                            path = path,
                            color = BentoPrimary,
                            style = Stroke(width = 6f)
                        )

                        dataPoints.forEachIndexed { i, value ->
                            val x = i * stepX
                            val y = height * (1.0f - (value / maxScale))
                            drawCircle(
                                color = if (value > 3.0f) ColorRed else BentoSecondary,
                                radius = 8f,
                                center = Offset(x, y)
                            )
                        }
                    }
                }
                
                // Graph X-labels
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val dataPoints = listOf("1.2m", "1.8m", "2.5m", "2.9m", "3.2m", "1.9m", "1.4m")
                    val days = listOf("週一", "週二", "週三", "週四", "週五", "週六", "週日")
                    days.forEachIndexed { i, dayName ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(dayName, color = BentoMutedText, fontSize = 10.sp)
                            Text(dataPoints[i], color = BentoPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SeaPresetChip(label: String, type: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(BentoSurface)
            .border(1.dp, BentoPrimary, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = BentoPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TelemetryItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp)
    ) {
        Icon(icon, contentDescription = title, tint = BentoSecondary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(title, color = BentoMutedText, fontSize = 11.sp)
        Text(value, color = BentoDarkText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

// -------------------------------------------------------------
// TAB 1: FLIGHT TRACKING & ACTION BOARD
// -------------------------------------------------------------
@Composable
fun FlightsTab(
    viewModel: MaritimeViewModel,
    flights: List<VesselFlight>,
    waveHeight: Double,
    windSpeed: Double,
    searchQuery: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text(
                text = "台中港客輪/遊艇航班監控",
                color = BentoDarkText,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "今日海象實際狀態下 (波高:${String.format("%.2f", waveHeight)}m, 風速:${String.format("%.1f", windSpeed)}m/s)，即時顯示各船隻出航基準與改退簽連動。",
                color = BentoMutedText,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }

        // Search text interface
        TextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            placeholder = { Text("輸入航班代碼 (如 TC-101) 或船型名稱進行動態過濾", fontSize = 12.sp, color = BentoMutedText) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = BentoMutedText) },
            modifier = Modifier.fillMaxWidth().testTag("flights_search_field"),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = BentoSurface,
                unfocusedContainerColor = BentoSurface,
                focusedTextColor = BentoDarkText,
                unfocusedTextColor = BentoDarkText,
                focusedLabelColor = BentoPrimary,
                unfocusedLabelColor = BentoMutedText,
                focusedIndicatorColor = BentoPrimary,
                unfocusedIndicatorColor = BentoBorder
            ),
            shape = RoundedCornerShape(12.dp)
        )

        val filteredFlights = flights.filter {
            it.flightNumber.contains(searchQuery, ignoreCase = true) ||
            it.vesselName.contains(searchQuery, ignoreCase = true)
        }

        if (filteredFlights.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.DirectionsBoat, contentDescription = "Empty list", tint = BentoBorder, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("未查找符合條件的航班", color = BentoMutedText, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).testTag("flights_lazy_column"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredFlights) { flight ->
                    val safetyStatus = viewModel.getStatusForClass(flight.shipClass, waveHeight, windSpeed)
                    
                    val safetyColor = when (safetyStatus) {
                        is SecurityStatus.GREEN -> ColorGreen
                        is SecurityStatus.YELLOW -> ColorYellow
                        is SecurityStatus.RED -> ColorRed
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = BentoSurface),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BentoBorder, RoundedCornerShape(20.dp))
                            .testTag("flight_item_${flight.flightNumber}"),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // First row: Flight detail, class indicator, and booking state
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .background(BentoPrimary.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("Class ${flight.shipClass}", color = BentoPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                    Text(
                                        text = flight.flightNumber,
                                        color = BentoDarkText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    )
                                }
                                
                                // Booking Status Indicator (Active, Refunded, Rescheduled)
                                val (statusText, statusBg, statusTextCol) = when (flight.bookingStatus) {
                                    "Active" -> Triple("票務狀態：已購票 (有效)", BentoSecondary.copy(alpha = 0.12f), BentoSecondary)
                                    "Refunded" -> Triple("已完成線上退票 (全額退款)", BentoBorder.copy(alpha = 0.3f), BentoMutedText)
                                    "Rescheduled" -> Triple("已完成搭乘改簽 (免費退改)", BentoPrimary.copy(alpha = 0.12f), BentoPrimary)
                                    else -> Triple("未知狀態", BentoBorder.copy(alpha = 0.3f), BentoMutedText)
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(statusBg)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(text = statusText, color = statusTextCol, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Vessel details and times
                            Text(text = flight.vesselName, color = BentoDarkText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("出發時刻：${flight.departureTime} (今日)", color = BentoMutedText, fontSize = 12.sp)
                                    Text("核載人數：${flight.passengerCount} 人", color = BentoMutedText, fontSize = 12.sp)
                                }
                                
                                // Current sea criteria state for this specific flight type
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(modifier = Modifier.size(10.dp).background(safetyColor, CircleShape))
                                    Text(
                                        text = when (safetyStatus) {
                                            is SecurityStatus.GREEN -> "符合綠燈出航"
                                            is SecurityStatus.YELLOW -> "達黃燈警戒"
                                            is SecurityStatus.RED -> "紅燈：取消停航"
                                        },
                                        color = safetyColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = BentoBorder.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))

                            // Active refund/rebook Actions
                            if (flight.bookingStatus == "Active") {
                                when (safetyStatus) {
                                    is SecurityStatus.RED -> {
                                        Column {
                                            Text(
                                                text = "⚠️ 航班已達紅燈停航標準。您可以一鍵辦理退換票：",
                                                color = ColorRed,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Button(
                                                    onClick = { viewModel.processRefundForFlight(flight.id) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = ColorRed),
                                                    shape = RoundedCornerShape(20.dp),
                                                    modifier = Modifier.weight(1f).height(38.dp),
                                                    contentPadding = PaddingValues(0.dp)
                                                ) {
                                                    Icon(Icons.Default.CreditCard, contentDescription = "Refund", modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("全額退票", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Button(
                                                    onClick = { viewModel.processRebookForFlight(flight.id) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary),
                                                    shape = RoundedCornerShape(20.dp),
                                                    modifier = Modifier.weight(1f).height(38.dp),
                                                    contentPadding = PaddingValues(0.dp)
                                                ) {
                                                    Icon(Icons.Default.CalendarToday, contentDescription = "Reschedule", modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("免費改簽", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                    is SecurityStatus.YELLOW -> {
                                        if (flight.shipClass == "A") { // A-Class low-comfort advisory
                                            Column {
                                                Text(
                                                    text = "🌊 目前正值顛簸黃燈警戒區間，大船正常起航，但容易暈船旅客可於出發前辦理彈性免手續費退改：",
                                                    color = ColorYellow,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                    OutlinedButton(
                                                        onClick = { viewModel.processRefundForFlight(flight.id) },
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorYellow),
                                                        border = BorderStroke(1.dp, ColorYellow),
                                                        shape = RoundedCornerShape(20.dp),
                                                        modifier = Modifier.weight(1f).height(38.dp),
                                                        contentPadding = PaddingValues(0.dp)
                                                    ) {
                                                        Text("免責全額退票", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    Button(
                                                        onClick = { viewModel.processRebookForFlight(flight.id) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = BentoSecondary),
                                                        shape = RoundedCornerShape(20.dp),
                                                        modifier = Modifier.weight(1f).height(38.dp),
                                                        contentPadding = PaddingValues(0.dp)
                                                    ) {
                                                        Icon(Icons.Default.CalendarMonth, contentDescription = "Reschedule", modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("免手續費改簽", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        } else {
                                            // Other classes yellow alert
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("⚠️ 黃色警戒提示，依耐浪分級行進。", color = ColorYellow, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                Button(
                                                    onClick = { viewModel.processRebookForFlight(flight.id) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary),
                                                    shape = RoundedCornerShape(20.dp),
                                                    modifier = Modifier.height(34.dp),
                                                    contentPadding = PaddingValues(horizontal = 14.dp)
                                                ) {
                                                    Text("預檢改簽", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                    is SecurityStatus.GREEN -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = "Safe", tint = ColorGreen, modifier = Modifier.size(16.dp))
                                                Text("海象符合安全航行標準，正常開航", color = ColorGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Text("無須提前退改", color = BentoMutedText, fontSize = 11.sp)
                                        }
                                    }
                                }
                            } else {
                                // Cancelled/Rebooked/Refunded summary
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.TaskAlt, contentDescription = "Committed", tint = ColorGreen, modifier = Modifier.size(16.dp))
                                    Text("此訂單已受理完成。系統資料庫已更新其客票實施狀態。", color = BentoMutedText, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
          // -------------------------------------------------------------
// TAB 2: PORT LAYOUT + AUTOMATE NOTIFICATIONS CENTER
// -------------------------------------------------------------
@Composable
fun TerminalAndNotificationsTab(
    viewModel: MaritimeViewModel,
    notifications: List<MockNotification>,
    waveHeight: Double,
    windSpeed: Double,
    tideLevel: Double,
    isExtremeAlert: Boolean,
    alarmReason: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Upper Port Details and Locations Info Map
        Column {
            Text(
                text = "台中港登船碼頭與航線定位",
                color = BentoDarkText,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "即時監管航務通道航情，並可點選模擬極度惡劣異常實況。",
                color = BentoMutedText,
                fontSize = 12.sp
            )
        }

        // Port map schematics drawing (Bento Card)
        Card(
            colors = CardDefaults.cardColors(containerColor = BentoSurface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.border(1.dp, BentoBorder, RoundedCornerShape(24.dp)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "台中港 19A 大型客運碼頭監視圖 (Live Buoy Radar)",
                    color = BentoDarkText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // Visual Map or Radar Screen via Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(BentoBg, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, BentoBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height

                        // Draw harbor dock shorelines
                        val dockPath = Path().apply {
                            moveTo(0f, height * 0.7f)
                            lineTo(width * 0.45f, height * 0.7f)
                            lineTo(width * 0.45f, height * 0.25f)
                            lineTo(width * 0.7f, height * 0.25f)
                            lineTo(width * 0.7f, height * 0.05f)
                        }

                        drawPath(
                            path = dockPath,
                            color = BentoBorder,
                            style = Stroke(width = 6f)
                        )

                        // Draw dynamic waving water layers below shore based on wave info
                        val waterPath = Path().apply {
                            moveTo(0f, height)
                            lineTo(width, height)
                            lineTo(width, height * 0.75f - (waveHeight.toFloat() * 8f))
                            lineTo(0f, height * 0.75f - (waveHeight.toFloat() * 8f))
                            close()
                        }
                        drawPath(
                            path = waterPath,
                            color = BentoPrimary.copy(alpha = 0.15f)
                        )

                        // Ship location marker (19A terminal)
                        drawCircle(
                            color = BentoSecondary,
                            radius = 12f,
                            center = Offset(width * 0.55f, height * 0.45f)
                        )
                    }

                    // Floating text info
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(BentoSurface.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                            .border(1.dp, BentoBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "19A 碼頭：台中-平潭航線 (麗娜輪靠泊位)",
                            color = BentoDarkText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("航線定點：台中港 ➔ 外海 10 浬觀測浮標", color = BentoMutedText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Text("航道深度：16.5m (現時潮位: +${String.format("%.1f", tideLevel)}m)", color = BentoMutedText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                    Box(
                        modifier = Modifier
                            .background(ColorGreen.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("碼頭防波堤正常", color = ColorGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // EXTREME INCIDENT SIMULATOR TRIGGERS (突發性瘋狗浪或瞬間陣風超標 2-6小時內)
        Card(
            colors = CardDefaults.cardColors(containerColor = BentoSurface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.border(
                width = if (isExtremeAlert) 2.dp else 1.dp,
                color = if (isExtremeAlert) ColorRed else BentoBorder,
                shape = RoundedCornerShape(24.dp)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🚨 突發性船難/瘋狗浪極端演練控制儀 (Emergency Trigger)",
                    color = BentoDarkText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "模擬出發前 2-6 小時，海面突然爆發巨大瘋狗浪（突發波高 > 3.0m）或側向陣風達到 9 級暴風，系統即刻發送強烈蓋版彈窗、啟用「人工手動客服」並以 AI 自動電話通知旅客機制。",
                    color = BentoMutedText,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
                
                Spacer(modifier = Modifier.height(14.dp))

                if (isExtremeAlert) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ColorRed.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .border(1.dp, ColorRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text("【系統警告：應急模式運行中】", color = ColorRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(alarmReason, color = BentoDarkText, fontSize = 12.sp, lineHeight = 17.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { viewModel.dismissExtremeAlert() },
                                colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("復原正常海象監控 (Reset)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Button(
                        onClick = { viewModel.simulateExtremeFreakWaveIncident() },
                        colors = ButtonDefaults.buttonColors(containerColor = ColorRed),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth().testTag("simulate_freak_wave_button")
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = "Trigger Red Alert", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("一鍵模擬突發外海瘋狗浪 & 9級大風 (觸發急迫語音電話)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // AUTOMATED PASSENGER NOTIFICATION LOGS (客服自動化通報機制)
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "旅客簡訊與推播模擬收件箱 (Notification Tray)",
                    color = BentoDarkText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                if (notifications.isNotEmpty()) {
                    Text(
                        text = "清除日誌",
                        color = BentoPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { viewModel.clearAllNotifications() }
                            .testTag("clear_notifications_btn")
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BentoSurface, RoundedCornerShape(24.dp))
                        .border(1.dp, BentoBorder, RoundedCornerShape(24.dp))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Sms, contentDescription = "No notifications", tint = BentoBorder, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("目前海象極佳或尚未進行海象滑動調整。", color = BentoMutedText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("（您可以在海象儀把波高拉大，會自動在此推送客服通知）", color = BentoMutedText.copy(alpha = 0.8f), fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                }
            } else {
                notifications.forEach { item ->
                    NotificationMessageCard(item, viewModel)
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
fun NotificationMessageCard(item: MockNotification, viewModel: MaritimeViewModel) {
    val borderCol = when (item.type) {
        "RED" -> ColorRed
        "YELLOW" -> ColorYellow
        "GREEN" -> ColorGreen
        else -> BentoPrimary
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = BentoSurface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderCol.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .testTag("notification_card_${item.id}"),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = if (item.type == "RED" || item.type == "EXTREME") Icons.Default.Cancel else Icons.Default.Campaign,
                        contentDescription = item.type,
                        tint = borderCol,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = item.title,
                        color = borderCol,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                }
                Text(text = item.timestamp, color = BentoMutedText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = item.body,
                color = BentoDarkText,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            if (item.canRefundOrChange) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = BentoBorder.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "👉 點擊快捷動作更新 Room 航票狀態",
                        color = BentoMutedText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.processRefundForFlight(1)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ColorRed),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(30.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("免費退票", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                viewModel.processRebookForFlight(1)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(30.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("一鍵改簽", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// GLOBAL VOICE CALL OVERLAY (突發瘋狗浪瞬間致電旅客電話模擬)
// -------------------------------------------------------------
@Composable
fun SimulatedVoiceCallOverlay(
    status: String,
    reason: String,
    onAnswer: () -> Unit,
    onDecline: () -> Unit
) {
    Dialog(onDismissRequest = { onDecline() }) {
        Card(
            colors = CardDefaults.cardColors(containerColor = BentoSurface),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, BentoBorder, RoundedCornerShape(28.dp))
                .testTag("voice_call_overlay"),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Warning Beacon icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(ColorRed.copy(alpha = 0.1f), CircleShape)
                        .border(2.dp, ColorRed, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneInTalk,
                        contentDescription = "Alert Call",
                        tint = ColorRed,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "台中港航安警告預報通話",
                    color = BentoDarkText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Text(
                    text = "發話方：航道安全預警播報中心\n通知：麗娜輪因突發惡劣瘋狗浪緊急取消開航",
                    color = BentoMutedText,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Divider(color = BentoBorder.copy(alpha = 0.5f))

                // Render voice-to-text transcript based on call action status
                when (status) {
                    "Ringing" -> {
                        Text(
                            text = "［系統正在向您的手機撥送應急語音播報...］",
                            color = BentoPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = onDecline,
                                colors = ButtonDefaults.buttonColors(containerColor = BentoMutedText),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("拒接", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onAnswer,
                                colors = ButtonDefaults.buttonColors(containerColor = ColorGreen),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("接聽預警", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    "Connected" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BentoBg, RoundedCornerShape(12.dp))
                                .border(1.dp, BentoBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Text("［即時自動語音 / 播音文本對照］：", color = ColorGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "「您好！這裡是台中港客航通行管防中心。極其遺憾地通知您，由於外海近2小時爆發巨大側向波湧，實測波高驟增至 3.5 米，持續橫掃主要航道，原定客輪班次均已強制停開。請您暂緩前往港口起渡碼頭辦理乘渡手續。相關客服網關已對接完畢，一鍵為您實施全程免費改簽或自動原路返還付退票款額...」",
                                color = BentoDarkText,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Button(
                            onClick = onDecline,
                            colors = ButtonDefaults.buttonColors(containerColor = ColorRed),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.CallEnd, contentDescription = "Hang Up")
                                Text("掛斷安全電話", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    "Ended" -> {
                        Text(
                            text = "語音播報已結束。敬請注意避風戒備。",
                            color = BentoMutedText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
