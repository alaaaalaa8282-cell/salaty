package com.mohamedabdelazeim.zekr.ui.prayer

import android.content.Context
import android.location.Geocoder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batoulapps.adhan.*
import com.batoulapps.adhan.data.DateComponents
import com.mohamedabdelazeim.zekr.data.local.PrayerTimesDao
import com.mohamedabdelazeim.zekr.data.local.PrayerTimesEntity
import com.mohamedabdelazeim.zekr.data.repository.LocationRepository
import com.mohamedabdelazeim.zekr.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class PrayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationRepository: LocationRepository,
    private val settingsRepository: SettingsRepository,
    private val prayerTimesDao: PrayerTimesDao
) : ViewModel() {

    private val _prayerState = MutableStateFlow(PrayerState())
    val prayerState: StateFlow<PrayerState> = _prayerState.asStateFlow()

    private val _locationState = MutableStateFlow(LocationState())
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()

    private val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val hijriDateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("ar"))
    private val gregorianDateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("ar"))

    init {
        loadSavedLocation()
    }

    private fun loadSavedLocation() {
        viewModelScope.launch {
            locationRepository.getSavedLocation().collect { location ->
                if (location != null) {
                    _locationState.value = LocationState(
                        locationName = location.locationName,
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                }
            }
        }
    }

    fun loadPrayerTimes() {
        viewModelScope.launch {
            _prayerState.value = _prayerState.value.copy(isLoading = true, error = null)

            try {
                val location = locationRepository.getCurrentLocation()
                val locationName = getLocationName(location.latitude, location.longitude)

                locationRepository.saveLocation(location, locationName)

                _locationState.value = LocationState(
                    locationName = locationName,
                    latitude = location.latitude,
                    longitude = location.longitude
                )

                val prayers = calculatePrayerTimes(location.latitude, location.longitude)
                val hijriDate = getHijriDate()
                val gregorianDate = gregorianDateFormat.format(Date())

                val nextPrayer = findNextPrayer(prayers)

                _prayerState.value = PrayerState(
                    prayers = prayers,
                    nextPrayer = nextPrayer,
                    hijriDate = hijriDate,
                    gregorianDate = gregorianDate,
                    isLoading = false
                )

                savePrayerTimesToDatabase(prayers, hijriDate, gregorianDate)

            } catch (e: Exception) {
                _prayerState.value = _prayerState.value.copy(
                    isLoading = false,
                    error = "تعذر تحميل مواقيت الصلاة: ${e.message}"
                )
            }
        }
    }

    fun refreshPrayerTimes() {
        loadPrayerTimes()
    }

    private suspend fun calculatePrayerTimes(latitude: Double, longitude: Double): List<PrayerItem> {
        val coordinates = Coordinates(latitude, longitude)
        val date = DateComponents.from(Date())
        val calculationMethod = settingsRepository.getCalculationMethod()

        val params = CalculationMethod.valueOf(calculationMethod).params
        params.madhab = Madhab.SHAFI

        val prayerTimes = PrayerTimes(coordinates, date, params)

        return listOf(
            PrayerItem(
                name = "الفجر",
                time = dateFormat.format(prayerTimes.fajr)
            ),
            PrayerItem(
                name = "الشروق",
                time = dateFormat.format(prayerTimes.sunrise)
            ),
            PrayerItem(
                name = "الظهر",
                time = dateFormat.format(prayerTimes.dhuhr)
            ),
            PrayerItem(
                name = "العصر",
                time = dateFormat.format(prayerTimes.asr)
            ),
            PrayerItem(
                name = "المغرب",
                time = dateFormat.format(prayerTimes.maghrib)
            ),
            PrayerItem(
                name = "العشاء",
                time = dateFormat.format(prayerTimes.isha)
            )
        )
    }

    private fun findNextPrayer(prayers: List<PrayerItem>): PrayerItem? {
        val now = Date()
        val currentTime = dateFormat.format(now)

        return prayers.firstOrNull { prayer ->
            prayer.time > currentTime
        } ?: prayers.firstOrNull()?.copy(name = "${prayers.first().name} (غداً)")
    }

    private fun getLocationName(latitude: Double, longitude: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale("ar"))
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                addresses[0]?.locality ?: addresses[0]?.adminArea ?: "موقع غير معروف"
            } else {
                "موقع غير معروف"
            }
        } catch (e: Exception) {
            "موقع غير معروف"
        }
    }

    private fun getHijriDate(): String {
        return try {
            val calendar = Calendar.getInstance()
            val hijriCalendar = com.github.msarhan.ummalqura.calendar.UmmalquraCalendar()
            hijriCalendar.time = calendar.time

            val day = hijriCalendar.get(Calendar.DAY_OF_MONTH)
            val month = hijriCalendar.get(Calendar.MONTH) + 1
            val year = hijriCalendar.get(Calendar.YEAR)

            val monthNames = arrayOf(
                "محرم", "صفر", "ربيع الأول", "ربيع الثاني",
                "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
                "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
            )

            "$day ${monthNames[month - 1]} $year"
        } catch (e: Exception) {
            "التاريخ الهجري غير متاح"
        }
    }

    private suspend fun savePrayerTimesToDatabase(
        prayers: List<PrayerItem>,
        hijriDate: String,
        gregorianDate: String
    ) {
        val entity = PrayerTimesEntity(
            date = System.currentTimeMillis(),
            fajr = prayers[0].time,
            sunrise = prayers[1].time,
            dhuhr = prayers[2].time,
            asr = prayers[3].time,
            maghrib = prayers[4].time,
            isha = prayers[5].time,
            hijriDate = hijriDate,
            gregorianDate = gregorianDate
        )
        prayerTimesDao.insertPrayerTimes(entity)
    }
}
