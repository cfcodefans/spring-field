package com.thenetcircle.commons

import org.apache.commons.lang3.time.DateFormatUtils
import org.apache.commons.lang3.time.DateUtils
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*

object DateTimeHelper {
    const val ZONE_ID_OF_BERLIN: String = "Europe/Berlin"
    const val ZONE_ID_OF_SHANGHAI: String = "Asia/Shanghai"
    const val ZONE_ID_OF_ETC_GMT: String = "Etc/GMT"

    val ZONE_ID_ETC_GMT: ZoneId = ZoneId.of(ZONE_ID_OF_ETC_GMT)

    val DEFAULT_APPLE_DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss VV")

    const val EURO_DATE_TIME_FORMAT: String = "dd.MM.yyyy HH:mm:ss"
    const val DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss"

    val EURO_DATE_TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern(EURO_DATE_TIME_FORMAT)

    private val DEFAULT_DATETIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT)
    val ETC_GMT_DATETIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT).withZone(ZONE_ID_ETC_GMT)
    private val SHANGHAI_DATETIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT).withZone(ZoneId.of(ZONE_ID_OF_SHANGHAI))
    private val BERLIN_DATETIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_FORMAT).withZone(ZoneId.of(ZONE_ID_OF_BERLIN))

    const val DEFAULT_SOLR_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    val DEFAULT_SOLR_DATETIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern(DEFAULT_SOLR_DATE_TIME_FORMAT)

    fun LocalDateTime.toDate(): Date = Date.from(this.atZone(getDefaultTimeZoneId()).toInstant())

    fun LocalDateTime.addDuration(duration: Duration): LocalDateTime = this
        .atZone(getDefaultTimeZoneId())
        .toInstant()
        .plus(duration)
        .let { LocalDateTime.ofInstant(it, getDefaultTimeZoneId()) }

    fun LocalDateTime.addPeriod(period: Period): LocalDateTime = this
        .atZone(getDefaultTimeZoneId())
        .toInstant()
        .plus(period)
        .let { LocalDateTime.ofInstant(it, getDefaultTimeZoneId()) }

    fun getDefaultTimeZoneId(): ZoneId = ZoneId.systemDefault()

    fun yesterday(): LocalDateTime = currentLocalDateTime().minusDays(1)

    fun currentTimeMillis(): Long = Instant.now().toEpochMilli()

    fun currentLocalDateTime(): LocalDateTime = LocalDateTime.now(getDefaultTimeZoneId())

    fun currentLocalDateTime(zone: ZoneId?): LocalDateTime? = LocalDateTime.now(zone)

    fun endOfTodayLocalDateTime(): LocalDateTime? = LocalDate.now(getDefaultTimeZoneId()).atTime(LocalTime.MAX)

    fun convertGMTDateTime2LocalDateTime(gmtDateTime: String?): LocalDateTime? {
        return if (gmtDateTime.isNullOrBlank())
            null
        else
            LocalDateTime.parse(gmtDateTime, DEFAULT_APPLE_DATE_FMT)
                .atZone(ZONE_ID_ETC_GMT)
                .withZoneSameInstant(getDefaultTimeZoneId())
                .toLocalDateTime()
    }

    fun localDateTimeToDate(localDateTime: LocalDateTime?, zoneId: String = ZoneId.systemDefault().toString()): Date? {
        return localDateTime?.let { Date.from(it.toInstant(ZoneId.of(zoneId).rules.getOffset(Instant.now()))) }
    }

    /**
     * LocalDateTime -> String ("yyyy-MM-dd HH:mm:ss")
     */
    fun getDefaultFormattedDateTime(localDateTime: LocalDateTime?): String {
        return DEFAULT_DATETIME_FMT.format(localDateTime)
    }

    /**
     * LocalDateTime -> String ("yyyy-MM-dd HH:mm:ss Europe/Berlin")
     * LocalDateTime -> String ("yyyy-MM-dd HH:mm:ss Etc/GMT")
     * LocalDateTime -> String ("yyyy-MM-dd HH:mm:ss Asia/Shanghai")
     */
    fun getDefaultFormattedDateTimeWithZoneId(localDateTime: LocalDateTime?): String? {
        return localDateTime?.let { "${DEFAULT_DATETIME_FMT.format(it)} ${getDefaultTimeZoneId()}" }
    }

    /**
     * String -> LocalDateTime by format: yyyy-MM-dd HH:mm:ss
     * return null for empty or blank string
     *
     * @param dateString: String
     * @return LocalDateTime
     */
    fun convert2LocalDateTime(dateString: String?): LocalDateTime? {
        return if (dateString.isNullOrBlank())
            null
        else when {
            dateString.endsWith(ZONE_ID_OF_ETC_GMT) -> LocalDateTime.parse(dateString.removeSuffix(ZONE_ID_OF_ETC_GMT).trim(), ETC_GMT_DATETIME_FMT)
            dateString.endsWith(ZONE_ID_OF_SHANGHAI) -> LocalDateTime.parse(dateString.removeSuffix(ZONE_ID_OF_SHANGHAI).trim(), SHANGHAI_DATETIME_FMT)
            dateString.endsWith(ZONE_ID_OF_BERLIN) -> LocalDateTime.parse(dateString.removeSuffix(ZONE_ID_OF_BERLIN).trim(), BERLIN_DATETIME_FMT)
            else -> LocalDateTime.parse(dateString, ETC_GMT_DATETIME_FMT)
        }
    }

    /**
     * String -> LocalDateTime
     */
    fun convert2LocalDateTime(dateString: String?, dateTimeFmt: DateTimeFormatter?): LocalDateTime? {
        return if (dateString.isNullOrBlank()) null else LocalDateTime.parse(dateString, dateTimeFmt)
    }

    /**
     * String ("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") -> LocalDateTime
     */
    fun convertSolrDateString2LocalDateTime(dateString: String?): LocalDateTime? {
        return convert2LocalDateTime(dateString, DEFAULT_SOLR_DATETIME_FMT)
    }

    /**
     * millis -> string (getDefaultFormattedDateTime == getBerlinFormattedDateTime without Europe/Berlin)
     */
    fun getDefaultFormattedDateTime(millisecond: Long): String? {
        return getDefaultFormattedDateTime(localDateTime = getLocalDateTimeFromTimeMillis(millisecond))
    }

    /**
     * millis -> string (ZoneId)
     */
    private fun millsToLocalDateTimeStringByZoneId(millis: Long, zoneId: String): String {
        return getDefaultFormattedDateTime(millsToLocalDateTime(millis, ZoneId.of(zoneId)))
    }

    // millis -> LocalDateTime
    fun getLocalDateTimeFromTimeMillis(milliseconds: Long): LocalDateTime {
        return millsToLocalDateTime(milliseconds, getDefaultTimeZoneId())
    }

    //millis -> LocalDateTime (ZoneId)
    fun millsToLocalDateTime(millis: Long, zoneId: ZoneId?): LocalDateTime {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), zoneId)
    }

    fun getBerlinTime(millis: Long): String {
        return millsToLocalDateTimeStringByZoneId(millis, ZONE_ID_OF_BERLIN)
    }

    // millis -> string ("yyyy-MM-dd HH:mm:ss Europe/Berlin")
    fun getBerlinFormattedDateTime(millis: Long): String? {
        return "${getBerlinTime(millis)} $ZONE_ID_OF_BERLIN"
    }

    fun getEtcGmtTime(millis: Long): String {
        return millsToLocalDateTimeStringByZoneId(millis, ZONE_ID_OF_ETC_GMT)
    }

    // millis -> string ("yyyy-MM-dd HH:mm:ss Etc/GMT")
    fun getEtcGMTFormattedDateTime(millis: Long): String? {
        return "${getEtcGmtTime(millis)} $ZONE_ID_OF_ETC_GMT"
    }

    // LocalDateTime -> millis
    fun localDateTime2TimeMillis(localDateTime: LocalDateTime?, zoneId: ZoneId?): Long? {
        return localDateTime?.atZone(zoneId)?.toInstant()?.toEpochMilli()
    }

    fun localDateTime2TimeMillis(localDateTime: LocalDateTime?): Long? {
        return localDateTime?.atZone(getDefaultTimeZoneId())?.toInstant()?.toEpochMilli()
    }

    // string -> millis
    // length = 20, 2020-09-29T06:35:49Z
    // length = 22, 2020-09-29T06:35:49.7Z
    // length = 23, 2020-09-29T17:19:52.63Z
    // length = 24, 2020-09-29T17:21:51.153Z
    fun convertTZDateTimeString2TimeMillis(dateTimeStr: String?, zoneId: ZoneId?): Long? {
        if (dateTimeStr.isNullOrBlank()) return null
        if (!(dateTimeStr.indexOf("T") > -1 && dateTimeStr.indexOf("Z") > -1)) return null

        return try {
            when (dateTimeStr.length) {
                20 -> convert2LocalDateTime(dateTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"))
                22 -> convert2LocalDateTime(dateTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.S'Z'"))
                else -> convertSolrDateString2LocalDateTime(dateTimeStr)
            }.let { localDateTime2TimeMillis(it, zoneId)!! }
        } catch (e: DateTimeParseException) {
            null
        }
    }

    fun getDaysBefore(daysBefore: Int): LocalDateTime {
        return currentLocalDateTime().minusDays(daysBefore.toLong())
    }

    private const val DATE_FORMAT_24_H = "yyyy-MM-dd HH:mm:ss"

    private const val TIME_ZONE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

    // convert date str from other pattern(like dd.MM.yyyy HH:mm:ss) to yyyy-MM-dd HH:mm:ss
    fun convertDate24HStr(dateStr: String?, parsePattern: String): String? {
        if (dateStr.isNullOrBlank() || parsePattern.isNullOrBlank()) return null
        return getDate24HStr(DateUtils.parseDate(dateStr, parsePattern))
    }

    private fun getDate24HStr(data: Date?, dateFormat: String = DATE_FORMAT_24_H): String? {
        return data?.let { DateFormatUtils.format(it, dateFormat) }
    }

    fun LocalDateTime?.to24HourStr(): String? = this?.let { DEFAULT_DATETIME_FMT.format(it) }

    fun Date.info(): String = getDate24HStr(this)!!

    fun parse(dateStr: String?, dateFormat: String): Date? {
        if (dateStr.isNullOrBlank()) return null
        return kotlin.runCatching { DateUtils.parseDate(dateStr, dateFormat) }.getOrNull()
    }

    fun LocalDateTime.info(): String = getDefaultFormattedDateTimeWithZoneId(this)!!

    fun Date.toLocalDateTime(zid: ZoneId = ZoneId.systemDefault()): LocalDateTime = LocalDateTime.ofInstant(this.toInstant(), zid)
}

