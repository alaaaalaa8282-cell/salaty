package com.mohamedabdelazeim.zekr.data

import android.content.Context
import org.json.JSONObject

data class ZekrItem(
    val id: Int,
    val name: String,
    val audioRes: Int?,
    val text: String = ""
)

data class AdhkarItem(
    val id: String,
    val title: String,
    val text: String,
    val repeat: Int,
    val benefit: String
)

object ZekrData {
    val zekrList = listOf(
        ZekrItem(1, "الصلاة على النبي ﷺ", com.mohamedabdelazeim.zekr.R.raw.nozaker_salt_ala_habib, "اللهم صلِّ وسلِّم على نبينا محمد"),
        ZekrItem(2, "آية الأحزاب", com.mohamedabdelazeim.zekr.R.raw.ayah_elahzab, "إِنَّ اللَّهَ وَمَلَائِكَتَهُ يُصَلُّونَ عَلَى النَّبِيِّ"),
        ZekrItem(3, "الحمد لله", com.mohamedabdelazeim.zekr.R.raw.alhamdo_lelah, "الحمد لله"),
        ZekrItem(4, "اللهم لك الحمد", com.mohamedabdelazeim.zekr.R.raw.allahom_lk_alhamd, "اللهم لك الحمد"),
        ZekrItem(5, "لا حول ولا قوة إلا بالله", com.mohamedabdelazeim.zekr.R.raw.lahawla_wlaqowat, "لا حول ولا قوة إلا بالله"),
        ZekrItem(6, "سبحان الله وبحمده", com.mohamedabdelazeim.zekr.R.raw.sobhanallah_wabehamdeh, "سبحان الله وبحمده"),
        ZekrItem(7, "ربي اغفر لي ولوالدي", com.mohamedabdelazeim.zekr.R.raw.rbna_ighfer_li, "ربي اغفر لي ولوالديَّ")
    )

    fun loadAdhkar(context: Context, isMorning: Boolean): List<AdhkarItem> {
        return try {
            val fileName = if (isMorning) "morning_adhkar.json" else "evening_adhkar.json"
            val json = context.assets.open(fileName).bufferedReader().use { it.readText() }
            val root = JSONObject(json)
            val arr = root.getJSONArray("adhkar")
            val list = mutableListOf<AdhkarItem>()
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                list.add(
                    AdhkarItem(
                        id = item.optString("id"),
                        title = item.optString("title"),
                        text = item.optString("text"),
                        repeat = item.optInt("repeat", 1),
                        benefit = item.optString("benefit")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
}
