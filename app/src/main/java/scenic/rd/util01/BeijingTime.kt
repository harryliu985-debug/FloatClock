package scenic.rd.util01

import java.util.Calendar
import java.util.TimeZone

/** 北京时间（Asia/Shanghai）格式化，精确到十分之一秒，如 16:39:29.4 */
object BeijingTime {

    private val beijing: TimeZone = TimeZone.getTimeZone("Asia/Shanghai")

    fun format(epochMillis: Long): String {
        val cal = Calendar.getInstance(beijing)
        cal.timeInMillis = epochMillis
        val tenth = cal.get(Calendar.MILLISECOND) / 100
        return "%02d:%02d:%02d.%d".format(
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            cal.get(Calendar.SECOND),
            tenth
        )
    }
}
