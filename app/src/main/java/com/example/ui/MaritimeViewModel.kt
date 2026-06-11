package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ShipRepository
import com.example.data.VesselFlight
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MaritimeViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = ShipRepository(database.shipDao())

    // Live sea state controls
    private val _waveHeight = MutableStateFlow(1.8) // meters
    val waveHeight: StateFlow<Double> = _waveHeight.asStateFlow()

    private val _windSpeed = MutableStateFlow(9.5) // m/s
    val windSpeed: StateFlow<Double> = _windSpeed.asStateFlow()

    // Real-time variables
    private val _tideLevel = MutableStateFlow(1.2) // meters
    val tideLevel: StateFlow<Double> = _tideLevel.asStateFlow()

    private val _seaTemperature = MutableStateFlow(25.4)
    val seaTemperature: StateFlow<Double> = _seaTemperature.asStateFlow()

    // Last updated label
    private val _lastUpdatedTime = MutableStateFlow("最新觀測預報：1分鐘前")
    val lastUpdatedTime: StateFlow<String> = _lastUpdatedTime.asStateFlow()

    private val _secondsToNextUpdate = MutableStateFlow(3600) // 1 hour countdown
    val secondsToNextUpdate: StateFlow<Int> = _secondsToNextUpdate.asStateFlow()

    // UI state for filter selections
    private val _selectedClass = MutableStateFlow("A") // "A", "B", "C"
    val selectedClass: StateFlow<String> = _selectedClass.asStateFlow()

    // Search query for flight code or vessel name
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Extreme Incident & Notification Overlay Trigger states
    private val _isExtremeAlertActive = MutableStateFlow(false)
    val isExtremeAlertActive: StateFlow<Boolean> = _isExtremeAlertActive.asStateFlow()

    private val _extremeIncidentReason = MutableStateFlow("")
    val extremeIncidentReason: StateFlow<String> = _extremeIncidentReason.asStateFlow()

    private val _isVoiceCallSimulating = MutableStateFlow(false)
    val isVoiceCallSimulating: StateFlow<Boolean> = _isVoiceCallSimulating.asStateFlow()

    private val _voiceCallStatus = MutableStateFlow("Ringing") // Ringing, Connected, Ended
    val voiceCallStatus: StateFlow<String> = _voiceCallStatus.asStateFlow()

    // Mock Notifications list for the UI simulated notification tray
    private val _notifications = MutableStateFlow<List<MockNotification>>(emptyList())
    val notifications: StateFlow<List<MockNotification>> = _notifications.asStateFlow()

    // Room DB flights
    val flights: StateFlow<List<VesselFlight>> = repository.allFlights
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            // Seed database
            repository.initializeDefaultFlights()
            // Auto-timer simulation
            startAutoTelemetryTimer()
        }
    }

    private fun startAutoTelemetryTimer() {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                if (_secondsToNextUpdate.value > 0) {
                    _secondsToNextUpdate.value -= 1
                } else {
                    refreshSeaStatePreserving()
                }
            }
        }
    }

    // Interactive adjustment methods
    fun updateWaveHeight(height: Double) {
        _waveHeight.value = height
    }

    fun updateWindSpeed(speed: Double) {
        _windSpeed.value = speed
    }

    fun updateClassSelected(clazz: String) {
        _selectedClass.value = clazz
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Refresh simulation
    fun refreshSeaStatePreserving() {
        // Mock subtle fluctuations
        val waveDelta = ((-2..2).random() / 10.0)
        val windDelta = ((-15..15).random() / 10.0)
        
        _waveHeight.value = (_waveHeight.value + waveDelta).coerceIn(0.2, 5.0)
        _windSpeed.value = (_windSpeed.value + windDelta).coerceIn(1.0, 25.0)
        _tideLevel.value = (0.5 + (0..150).random() / 100.0)
        _seaTemperature.value = (24.0 + (0..20).random() / 10.0)
        
        _secondsToNextUpdate.value = 3600
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        _lastUpdatedTime.value = "最新觀測預報已更新：${formatter.format(Date())}"
        
        // Notify user about auto updates or normal flows
        checkSeaStateTriggers()
    }

    // Quick Sea Presets
    fun setSeaPreset(preset: String) {
        when (preset) {
            "calm" -> { // Summer Safe
                _waveHeight.value = 0.6
                _windSpeed.value = 4.5
            }
            "moderate" -> { // Warning zone for some classes
                _waveHeight.value = 1.6
                _windSpeed.value = 8.5
            }
            "rough_A_yellow" -> { // Class A Yellow zone, Class B/C Red zone
                _waveHeight.value = 2.4
                _windSpeed.value = 11.2
            }
            "rough_A_red" -> { // Standard Class A waves Red
                _waveHeight.value = 3.3
                _windSpeed.value = 12.0
            }
            "wind_heavy" -> { // Average wind >= 13 m/s trigger Red
                _waveHeight.value = 2.2
                _windSpeed.value = 14.5
            }
        }
        checkSeaStateTriggers()
    }

    // Logic helper for Class Status Color matching
    fun getStatusForClass(shipClass: String, currentWave: Double, currentWind: Double): SecurityStatus {
        return when (shipClass) {
            "A" -> {
                // If average wind >= 13.0, override with RED
                if (currentWind >= 13.0) {
                    SecurityStatus.RED("強制性陣風超標 (平均風速 ≥ 13m/s)，為保障靠泊安全，強制停航")
                } else if (currentWave > 3.0) {
                    SecurityStatus.RED("預測波高超過 3.0m 已達大型客輪強制停航標準")
                } else if (currentWave > 2.0 && currentWave <= 3.0) {
                    SecurityStatus.YELLOW("波高 2.0m - 3.0m，大型客輪可準點出航但航行顛簸，實施舒適度黃燈預警")
                } else {
                    SecurityStatus.GREEN("海象平穩 (波高 ≤ 2.0m)，適合大型客輪航行")
                }
            }
            "B" -> {
                if (currentWave > 2.0) {
                    SecurityStatus.RED("預測波高超過 2.0m，中型船隻強制停航")
                } else if (currentWave > 1.0 && currentWave <= 2.0) {
                    SecurityStatus.YELLOW("波高 1.0m - 2.0m，中型遊艇達黃色警戒，建議旅客衡量安全性")
                } else {
                    SecurityStatus.GREEN("海象良好 (波高 ≤ 1.0m)，適合中型遊艇舒適開航")
                }
            }
            "C" -> {
                if (currentWave > 1.2) {
                    SecurityStatus.RED("預測波高超過 1.2m，C類無動力帆船、SUP體驗全面停航強制禁入海")
                } else if (currentWave > 0.8 && currentWave <= 1.2) {
                    SecurityStatus.YELLOW("波高 0.8m - 1.2m，小型活動限制於特定屏障水域進行")
                } else {
                    SecurityStatus.GREEN("最安全狀態 (波高 ≤ 0.8m)，最適合SUP、帆船、水上活動體驗")
                }
            }
            else -> SecurityStatus.GREEN("標準海象")
        }
    }

    private fun checkSeaStateTriggers() {
        // Automatically suggest sending target notifications in notifications stream
        val height = _waveHeight.value
        val wind = _windSpeed.value

        // If Class A is red
        val statusA = getStatusForClass("A", height, wind)
        if (statusA is SecurityStatus.RED) {
            triggerAutomatedNotification(
                title = "【⚠️ 台中港航安重要通報 - Class A 航班取消】",
                body = "您預訂的台中港出發航班，因外海預測波高 ${String.format("%.1f", height)}m 或風速超標 (${String.format("%.1f", wind)}m/s) 已達強制停航標準。本航班已確認停航。",
                canRefundOrChange = true,
                type = "RED"
            )
        } else if (statusA is SecurityStatus.YELLOW) {
            triggerAutomatedNotification(
                title = "【⚓ 台中港乘船舒適度與航安提醒】",
                body = "您預訂的航班將正常起航。目前預報波高 ${String.format("%.1f", height)}m，雖符合大船安全標準，但搖晃劇烈。建議服用暈船藥，或申請免費提前改期。",
                canRefundOrChange = true,
                type = "YELLOW"
            )
        }
    }

    private fun triggerAutomatedNotification(title: String, body: String, canRefundOrChange: Boolean, type: String) {
        viewModelScope.launch {
            val exists = _notifications.value.any { it.title == title }
            if (!exists) {
                val newNotify = MockNotification(
                    id = (0..10000).random(),
                    title = title,
                    body = body,
                    timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                    canRefundOrChange = canRefundOrChange,
                    type = type
                )
                _notifications.value = listOf(newNotify) + _notifications.value
            }
        }
    }

    // Trigger Extreme Freak Wave state! (突發巨浪 3.0m+ / 瞬間陣風9級以上)
    fun simulateExtremeFreakWaveIncident() {
        _isExtremeAlertActive.value = true
        _extremeIncidentReason.value = "【突發氣象預警】台中外海浮標在過去20分鐘內偵測到猛烈湧浪突破 3.5m（瞬間瘋狗浪），且伴隨 9 級強烈風暴陣風 (持續超標)。"
        
        // Push values to extreme
        _waveHeight.value = 3.6
        _windSpeed.value = 21.4

        // Insert notification
        val newAlert = MockNotification(
            id = (0..10000).random(),
            title = "【🚨 台中港極端海氣象特報 - 啟動人工緊急客服員】",
            body = "台中港外海在短時間內出現非預期巨浪與 9 級強烈瞬間陣風，系統已緊急暫停自動放行，即刻啟動「人工緊急客服模式」。若您已在前往碼頭途中，請注意行車安全並配合接駁指引！",
            timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
            canRefundOrChange = false,
            type = "EXTREME"
        )
        _notifications.value = listOf(newAlert) + _notifications.value

        // Launch Voice Call Simulation!
        _isVoiceCallSimulating.value = true
        _voiceCallStatus.value = "Ringing"
    }

    fun dismissExtremeAlert() {
        _isExtremeAlertActive.value = false
        _isVoiceCallSimulating.value = false
    }

    // Voice Call handlers
    fun answerSimulatedVoiceCall() {
        _voiceCallStatus.value = "Connected"
    }

    fun endSimulatedVoiceCall() {
        _voiceCallStatus.value = "Ended"
        viewModelScope.launch {
            delay(1000)
            _isVoiceCallSimulating.value = false
        }
    }

    // Interactively handle Refund / Rebook on a Flight booking!
    fun processRefundForFlight(flightId: Int) {
        viewModelScope.launch {
            repository.updateBookingStatus(flightId, "Refunded")
            
            // Log notice
            val successNotification = MockNotification(
                id = (0..10000).random(),
                title = "【✅ 退款辦理成功】",
                body = "系統已完成線上全額退款申請。手續費全免，款項將會在 3-5 個常態工作天退回您的原支付管道，請留意銀行通知。",
                timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                canRefundOrChange = false,
                type = "GREEN"
            )
            _notifications.value = listOf(successNotification) + _notifications.value
        }
    }

    fun processRebookForFlight(flightId: Int, newDate: String = "改簽：下週同班次") {
        viewModelScope.launch {
            repository.updateBookingStatus(flightId, "Rescheduled")
            
            val successNotification = MockNotification(
                id = (0..10000).random(),
                title = "【📅 航班改簽成功】",
                body = "您的船票已免費安排改簽至較平穩的下週航班 ($newDate)。免收任何改票服務費，電子憑證已同步更新。",
                timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                canRefundOrChange = false,
                type = "GREEN"
            )
            _notifications.value = listOf(successNotification) + _notifications.value
        }
    }

    fun clearAllNotifications() {
        _notifications.value = emptyList()
    }
}

// Sealed or simple model representing Sea Class light categories
sealed class SecurityStatus {
    abstract val text: String
    abstract val lightName: String

    data class GREEN(override val text: String) : SecurityStatus() {
        override val lightName = "綠燈 (安全)"
    }
    data class YELLOW(override val text: String) : SecurityStatus() {
        override val lightName = "黃燈 (警戒)"
    }
    data class RED(override val text: String) : SecurityStatus() {
        override val lightName = "紅燈 (強制停航)"
    }
}

data class MockNotification(
    val id: Int,
    val title: String,
    val body: String,
    val timestamp: String,
    val canRefundOrChange: Boolean,
    val type: String // GREEN, YELLOW, RED, EXTREME
)
