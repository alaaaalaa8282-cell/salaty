package com.mohamedabdelazeim.zekr.worker

import android.content.Context
import android.location.Geocoder
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.batoulapps.adhan.*
import com.batoulapps.adhan.data.DateComponents
import com.mohamedabdelazeim.zekr.data.local.PrayerTimesDao
import com.mohamedabdelazeim.zekr.data.local.PrayerTimesEntity
import com.mohamedabdelazeim.zekr.data.repository.LocationRepository
import com.mohamedabdelazeim.zekr.data.repository.SettingsRepository
import com.mohamedabdelazeim.zekr.service.alarm.PrayerAlarmScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@HiltWorker
class DailyPrayerWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val locationRepository: LocationRepository,
    private val settingsRepository: SettingsRepository,
    private val prayerTimesDao: PrayerTimesDao,
    private val prayerAlarmScheduler: PrayerAlarmScheduler
) : CoroutineWorker(context, params) {

    private val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val hijriDateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("ar"))
    private val gregorianDateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("ar"))

    override suspend fun doWork(): Result {
        return try {
            val location = locationRepository.getCurrentLocation()
            val locationName = getLocationName(location.latitude, location.longitude)

            locationRepository.saveLocation(location, locationName)

            val prayers = calculatePrayerTimes(location.latitude, location.longitude)
            val hijriDate = getHijriDate()
            val gregorianDate = gregorianDateFormat.format(Date())

            savePrayerTimesToDatabase(prayers, hijriDate, gregorianDate)

            prayerAlarmScheduler.scheduleAllPrayerAlarms(prayers)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private suspend fun calculatePrayerTimes(latitude: Double, longitude: Double): List<PrayerTimeData> {
        val coordinates = Coordinates(latitude, longitude)
        val date = DateComponents.from(Date())
        val calculationMethod = settingsRepository.getCalculationMethod()

        val params = CalculationMethod.valueOf(calculationMethod).params
        params.madhab = Madhab.SHAFI

        val prayerTimes = PrayerTimes(coordinates, date, params)

        return listOf(
            PrayerTimeData("الفجر", prayerTimes.fajr),
            PrayerTimeData("الشروق", prayerTimes.sunrise),
            PrayerTimeData("الظهر", prayerTimes.dhuhr),
            PrayerTimeData("العصر", prayerTimes.asr),
            PrayerTimeData("المغرب", prayerTimes.maghrib),
            PrayerTimeData("العشاء", prayerTimes.isha)
        )
    }

    private fun getLocationName(latitude: Double, longitude: Double): String {
        return try {
            val geocoder = Geocoder(applicationContext, Locale("ar"))
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
        prayers: List<PrayerTimeData>,
        hijriDate: String,
        gregorianDate: String
    ) {
        val entity = PrayerTimesEntity(
            date = System.currentTimeMillis(),
            fajr = dateFormat.format(prayers[0].time),
            sunrise = dateFormat.format(prayers[1].time),
            dhuhr = dateFormat.format(prayers[2].time),
            asr = dateFormat.format(prayers[3].time),
            maghrib = dateFormat.format(prayers[4].time),
            isha = dateFormat.format(prayers[5].time),
            hijriDate = hijriDate,
            gregorianDate = gregorianDate
        )
        prayerTimesDao.insertPrayerTimes(entity)
    }

    data class PrayerTimeData(
        val name: String,
        val time: Date
    )
}
