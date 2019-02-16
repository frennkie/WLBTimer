package de.rhab.wlbtimer.model

import android.support.annotation.Keep
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.ChronoUnit
import kotlin.math.floor


@Keep
@IgnoreExtraProperties
data class Session(
        var objectId: String? = null,
        var category: Category? = null,
        var tsStartForward: Long = 0,  // is this of any use after switch to Firestore?!
        var tsStartReverse: Long = 0,  // is this of any use after switch to Firestore?!
        var tsStart: String? = null,
        var tsEnd: String? = null,
        var note: String? = null,
        var allDay: Boolean = false,
        var finished: Boolean = false,

        // breaks is implemented as list, but currently this should have only either 0 or 1 entry
        var breaks: MutableList<Break>? = null
) {
    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
                "objectId" to objectId,
                "category" to category,
                "tsStartForward" to fromDefaultStr(tsStart!!)!!.toEpochSecond(),
                "tsStartReverse" to 1 - fromDefaultStr(tsStart!!)!!.toEpochSecond(),
                "tsStart" to tsStart,
                "tsEnd" to tsEnd,
                "note" to note,
                "allDay" to allDay,
                "finished" to finished,
                "breaks" to breaks?.map(Break::toMap)
        )
    }

    @Exclude
    fun getDurationLong(): Long {
        if (this.tsEnd == null) {
            return -1
        }

        val startEpoch = ZonedDateTime.parse(this.tsStart)!!.toEpochSecond()
        val endEpoch = ZonedDateTime.parse(this.tsEnd)!!.toEpochSecond()
        val duration = endEpoch - startEpoch

        return when {
            duration < 0 -> -1
            duration.toInt() == 0 -> 0
            else -> duration
        }
    }

    @Exclude
    fun getDurationWeightedExcludingBreaks(): String {
        if (this.tsEnd == null) {
            return "running"
        }

        var mTotalBreakTime = 0
        if (this.breaks == null) {
            mTotalBreakTime = 0
        } else {
            this.breaks!!.forEach { mBreak ->
                mTotalBreakTime += mBreak.duration
            }
        }

        val mFactor = if (this.category != null) {
            this.category!!.factor
        } else {
            1.0
        }

        val startEpoch = ZonedDateTime.parse(this.tsStart)!!.toEpochSecond()
        val endEpoch = ZonedDateTime.parse(this.tsEnd)!!.toEpochSecond()
        val durationFull = endEpoch - startEpoch

        val duration = ((durationFull - mTotalBreakTime) * mFactor).toLong()

        return when {
            duration < 0 -> "invalid"
            duration.toInt() == 0 -> "zero"
            else -> {
                val durationMins = duration.toInt() / 60
                val durationHoursStr = "%.0f".format(floor(durationMins / 60.0)).padStart(2, '0')
                val durationMinsStr = durationMins.rem(60).toString().padStart(2, '0')

                "$durationHoursStr:$durationMinsStr"
            }
        }
    }

    @Exclude
    fun getTotalBreakTime(): String {
        var mTotalBreakTime = 0
        if (this.breaks == null) {
            mTotalBreakTime = 0
        } else {
            this.breaks!!.forEach { mBreak ->
                mTotalBreakTime += mBreak.duration
            }
        }

        return when {
            mTotalBreakTime < 0 -> "invalid"
            mTotalBreakTime == 0 -> "0:00"
            else -> {
                val totalBreakTimeMins = mTotalBreakTime / 60
                val totalBreakTimeHoursStr = "%.0f".format(floor(totalBreakTimeMins / 60.0)).padStart(2, '0')
                val totalBreakTimeMinsStr = totalBreakTimeMins.rem(60).toString().padStart(2, '0')

                "$totalBreakTimeHoursStr:$totalBreakTimeMinsStr"
            }
        }
    }


    @Exclude
    fun getDateStart(): String {
        return if (this.tsStart != null) {
            Session.toDateStr(ZonedDateTime.parse(this.tsStart))
        } else {
            return "..."
        }
    }

    @Exclude
    fun getDateStartWithWeekday(): String {
        return if (this.tsStart != null) {
            val dateString = Session.toDateStr(ZonedDateTime.parse(this.tsStart)).subSequence(5, 10)

            val zDT = ZonedDateTime.parse(this.tsStart)
            val weekday = zDT.dayOfWeek.name.substring(0, 2).toLowerCase().capitalize()

            "$dateString ($weekday)"
        } else {
            return "..."
        }
    }

    @Exclude
    fun getDateEnd(): String {
        return if (this.tsStart != null) {
            Session.toDateStr(ZonedDateTime.parse(this.tsEnd))
        } else {
            return "..."
        }
    }

    @Exclude
    fun getTimeStart(): String {
        return if (this.tsStart != null) {
            Session.toTimeStr(ZonedDateTime.parse(this.tsStart))
        } else {
            return "..."
        }
    }

    @Exclude
    fun getTimeZonedStart(): String {
        return if (this.tsStart != null) {
            Session.toTimeZonedStr(ZonedDateTime.parse(this.tsStart))
        } else {
            return "..."
        }
    }

    @Exclude
    fun getTimeEnd(): String {
        return if (this.tsEnd != null) {
            Session.toTimeStr(ZonedDateTime.parse(this.tsEnd))
        } else {
            return "..."
        }
    }

    @Exclude
    fun getTimeZonedEnd(): String {
        return if (this.tsEnd != null) {
            Session.toTimeZonedStr(ZonedDateTime.parse(this.tsEnd))
        } else {
            return "..."
        }
    }

    companion object {

        private const val TAG = "Session"

        const val FBP = "sessions"

        const val FBP_SESSION_RUNNING = "session-running"

        fun getZonedDateTimeNow(): ZonedDateTime {
            return ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS)
        }

        private const val DATE_FORMAT = "yyyy-MM-dd"
        private const val DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm"
        private const val DATE_TIME_ZONED_FORMAT = "yyyy-MM-dd HH:mm (VV)"
        private const val TIME_FORMAT = "HH:mm"
        private const val TIME_ZONED_FORMAT = "HH:mm (VV)"

        private val formatterDate: DateTimeFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT)
        private val formatterDateTime: DateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)
        private val formatterDateTimeZoned: DateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_ZONED_FORMAT)
        private val formatterTime: DateTimeFormatter = DateTimeFormatter.ofPattern(TIME_FORMAT)
        private val formatterTimeZoned: DateTimeFormatter = DateTimeFormatter.ofPattern(TIME_ZONED_FORMAT)

        fun fromDefaultStr(mString: String): ZonedDateTime? {
            return ZonedDateTime.parse(mString)
        }

        fun fromDateStr(mString: String): ZonedDateTime {
            return ZonedDateTime.parse(mString, formatterDate)
        }

        fun fromDateTimeStr(mString: String): ZonedDateTime {
            return ZonedDateTime.parse(mString, formatterDateTime)
        }

        fun fromDateTimeZonedStr(mString: String): ZonedDateTime {
            return ZonedDateTime.parse(mString, formatterDateTimeZoned)
        }

        fun fromTimeStr(mString: String): ZonedDateTime {
            return ZonedDateTime.parse(mString, formatterTime)
        }

        fun fromTimeZonedStr(mString: String): ZonedDateTime {
            return ZonedDateTime.parse(mString, formatterTimeZoned)
        }

        fun toDateStr(mZonedDateTime: ZonedDateTime): String {
            return formatterDate.format(mZonedDateTime)
        }

        fun toDateTimeStr(mZonedDateTime: ZonedDateTime): String {
            return formatterDateTime.format(mZonedDateTime)
        }

        fun toDateTimeZonedStr(mZonedDateTime: ZonedDateTime): String {
            return formatterDateTimeZoned.format(mZonedDateTime)
        }

        fun toTimeStr(mZonedDateTime: ZonedDateTime): String {
            return formatterTime.format(mZonedDateTime)
        }

        fun toTimeZonedStr(mZonedDateTime: ZonedDateTime): String {
            return formatterTimeZoned.format(mZonedDateTime)
        }

    }

}
