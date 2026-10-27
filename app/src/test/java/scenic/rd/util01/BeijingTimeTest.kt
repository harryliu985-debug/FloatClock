package scenic.rd.util01

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.TimeZone

/** 北京时间格式化：HH:mm:ss.S（十分之一秒） */
class BeijingTimeTest {

    @Test
    fun `epoch 0 是北京时间早上 8 点整`() {
        assertEquals("08:00:00.0", BeijingTime.format(0L))
    }

    @Test
    fun `毫秒截断为十分之一秒`() {
        // epoch 1234ms → 08:00:01.234 → 十分位 2
        assertEquals("08:00:01.2", BeijingTime.format(1234L))
        // 999ms → 十分位 9
        assertEquals("08:00:00.9", BeijingTime.format(999L))
    }

    @Test
    fun `用任意时区运行结果一致`() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
        assertEquals("08:00:00.0", BeijingTime.format(0L))
    }
}
