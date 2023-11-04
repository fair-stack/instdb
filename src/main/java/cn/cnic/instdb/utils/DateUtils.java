package cn.cnic.instdb.utils;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.quartz.CronExpression;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Slf4j
public class DateUtils {

    /**
     * datetime format
     */
    static final String ISO_DATE_FORMAT = "yyyy-MM-dd";
    static final String ISO_DATE_FORMAT1 = "yyyy/MM/dd";
    static final String ISO_DATE_YEAR = "yyyy";
    static final String CUSTOM_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    static final String DATE_PATTERN_yyyyMMddHHMMss = "yyyyMMddHHmmss";
    static final String DATE_PATTERN_ISO8601 = "yyyy-MM-dd'T'HH:mm:ssXXX";


    /**
     * Convert timestamp to string
     *
     * @param time
     * @return
     */
    public static String getDateToString(long time) {
        Date d = new Date(time);
        SimpleDateFormat formatter = new SimpleDateFormat(ISO_DATE_FORMAT);
        return formatter.format(d);
    }



    /**
     * Obtain the current year yyyy
     */
    public static String getCurrentYear() {
        return String.valueOf(LocalDate.now().getYear());
    }

    /**
     * Get the current date string,yyyy-MM-dd HH:mm:ss
     */
    public static String getCurrentDateTimeString() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(CUSTOM_DATETIME_FORMAT));
    }

    /**
     * Date formatting,yyyy-MM-dd HH:mm:ss
     */
    public static String getDateTimeString(LocalDateTime time) {
        return time.format(DateTimeFormatter.ofPattern(CUSTOM_DATETIME_FORMAT));
    }

    /**
     * Date formatting,yyyy-MM-dd
     */
    public static String getDateTimeString2(LocalDateTime time) {
        return time.format(DateTimeFormatter.ofPattern(ISO_DATE_FORMAT));
    }

    /**
     * Date formatting,yyyy
     */
    public static String getDateTimeString3(LocalDateTime time) {
        return time.format(DateTimeFormatter.ofPattern(ISO_DATE_YEAR));
    }

    /**
     * String yyyy-MM-dd HH:mm:ss turn LocalDateTime
     * @param time
     * @return
     */
    public static LocalDateTime getLocalDateTimeByString(String time) {
        DateTimeFormatter df = DateTimeFormatter.ofPattern(CUSTOM_DATETIME_FORMAT);
        return LocalDateTime.parse(time, df);
    }


    /**
     * String yyyy-MM-dd turn LocalDateTime
     * @param time
     * @return
     */
    public static LocalDateTime getLocalDateTimeByString2(String time) {
        DateTimeFormatter df = DateTimeFormatter.ofPattern(ISO_DATE_FORMAT);
        return LocalDate.parse(time, df).atStartOfDay();
    }

    public static String getDateTimeString(Date date) {
        return DateFormatUtils.format(date, CUSTOM_DATETIME_FORMAT);
    }

    /**
     * take Date take LocalDate
     *
     * @param date
     * @return java.time.LocalDate;
     */
    public static LocalDate dateToLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }



    /**
     * Get the current date string,yyyy-MM-dd
     */
    public static String getCurrentDateString() {
        return getDateString(LocalDate.now());
    }

    /**
     * Date formatting,yyyy-MM-dd
     */
    public static String getDateString(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern(ISO_DATE_FORMAT));
    }

    public static String getDateString(Date date) {
        return DateFormatUtils.format(date, ISO_DATE_FORMAT);
    }

    public static String getDateString1(Date date) {
        return DateFormatUtils.format(date, ISO_DATE_FORMAT1);
    }

    /**
     * Convert date string to date format data
     */
    public static Date parseDate(String date, String pattern) throws ParseException {
        return org.apache.commons.lang3.time.DateUtils.parseDate(date, pattern);
    }

    /**
     * LocalDateTime -> Date
     * @param localDateTime
     * @return
     */
    public static Date LocalDateTimeasDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * LocalDateTime -> ISO8601
     *
     * @param localDateTime
     * @return
     */
    public static String LocalDateTimeasISO8601(Date localDateTime) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN_ISO8601);
        return sdf.format(localDateTime);
    }

    /**
     * ISO8601 -> time
     *
     * @param ISO8601
     * @return
     */
    public static String LocalDateTimeasISO8601(String ISO8601) throws ParseException {
        DateFormat df = new SimpleDateFormat(DATE_PATTERN_ISO8601);
        Date  date = df.parse(ISO8601);
        DateFormat df2 = new SimpleDateFormat(CUSTOM_DATETIME_FORMAT);
        return df2.format(date);
    }

    /**
     * Date -> LocalDateTime
     * @param date
     * @return
     */
    public static LocalDateTime DateasLocalDateTime(Date date) {
        Instant instant = date.toInstant();
        ZoneId zoneId = ZoneId.systemDefault();
        return instant.atZone(zoneId).toLocalDateTime();
    }

    /**
     * @param beginTime start time
     * @param endTime   End time
     * @return trueDuring the time period，falseDuring the time period
     */
    public static boolean isBetween(LocalDateTime beginTime, LocalDateTime endTime) {
        //Get the current time
        LocalDateTime now = LocalDateTime.now();
        boolean flag = false;
        if (now.isAfter(beginTime) && now.isBefore(endTime)) {
            flag = true;
        }
        return flag;
    }


    /**
     * Convert short time format time to string yyyyMMddHHmmss
     *
     * @param dateDate
     * @return
     */
    public static String dateToStr_yyyyMMddHHMMss(Date dateDate) {
        SimpleDateFormat formatter = new SimpleDateFormat(DATE_PATTERN_yyyyMMddHHMMss);
        String dateString = formatter.format(dateDate);
        return dateString;
    }

    /**
     * Determine if the current time is on[startTime, endTime]Determine if the current time is on，Determine if the current time is on
     *
     * @param nowTime current time LocalDateTime
     * @param startTime start time
     * @return
     * @author jqlin
     */
    public static boolean isEffectiveDate(Date nowTime, String startTime) {
        try {
            Date dateStartTime = parseDate(startTime, ISO_DATE_FORMAT);
            if (nowTime.getTime() == dateStartTime.getTime()) {
                return true;
            }
            Calendar date = Calendar.getInstance();
            date.setTime(nowTime);
            Calendar begin = Calendar.getInstance();
            begin.setTime(dateStartTime);
            if (date.after(begin)) {
                return true;
            } else {
                return false;
            }
        } catch (ParseException e) {
            log.error("context",e);
        }
        return false;
    }

    /**
     * Determine if the start time is less than the end time
     * @param beginTime
     * @param endTime
     * @return
     */
    public static boolean belongCalendar(String beginTime, String endTime) {
        try {
            Date dateStartTime = parseDate(beginTime, ISO_DATE_FORMAT);
            Date dateendTime = parseDate(endTime, ISO_DATE_FORMAT);
            Calendar begin = Calendar.getInstance();
            begin.setTime(dateStartTime);

            Calendar end = Calendar.getInstance();
            end.setTime(dateendTime);

            if (dateStartTime.getTime() == dateendTime.getTime()) {
                return true;
            }
            if (end.after(begin)) {
                return true;
            } else {
                return false;

            }
        } catch (ParseException e) {
            log.error("context",e);
        }
        return false;
    }

    /**
     * Determine if it has exceeded how many hours
     * @param tableTime timeyyyy-M-d HH:mm:ss
     * @param hour  hour
     * @return
     */
    @SneakyThrows
    public static boolean judgmentDate(String tableTime, Integer hour){
        String currentTime = getCurrentDateTimeString();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-M-d HH:mm:ss");

        Date start = sdf.parse(tableTime);//Business time

        Date end = sdf.parse(currentTime);//current time 

        long cha = end.getTime() - start.getTime();

        if (cha < 0) {
            return false;
        }
        double result = cha * 1.0 / (1000 * 60 * 60);

        if (result <= hour) {
            return true;//Is less than or equal to hour Is less than or equal to
        } else {
            return false;
        }
    }

    public static long yesterday( int num){
        //Get the current time24Get the current time
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.set(Calendar.HOUR_OF_DAY,c.get(Calendar.HOUR_OF_DAY) - num);
        Date time= c.getTime();
        return time.getTime();
    }

    public static void main(String[] args) throws ParseException {

        String oldDateStr = "2022-05-26T23:26:04+08:00"    ;
        DateFormat df = new SimpleDateFormat(DATE_PATTERN_ISO8601);  //yyyy-MM-dd'T'HH:mm:ss.SSSZ
        Date  date = df.parse(oldDateStr);
        DateFormat df2 = new SimpleDateFormat("yyyy-MM-dd");
        String format = df2.format(date);
        System.out.println(format);


        System.out.println(belongCalendar(format, "2022-05-25"));

    }

    public static Date tomorrowdayByDate(int num,Date date){
        //Get the current time24Get the current time
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.HOUR_OF_DAY,c.get(Calendar.HOUR_OF_DAY) + num);
        Date time= c.getTime();
        return time;
    }

    public static Date tomorrowday(int num){
        //Get the current time24Get the current time
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.set(Calendar.HOUR_OF_DAY,c.get(Calendar.HOUR_OF_DAY) + num);
        Date time= c.getTime();
        return time;
    }

    /**
     * Get Time Range  Get Time Range  Get Time Range
     * @param date
     * @param num
     * @return
     */
    public static List<Date> getTimeInterval(Date date, int num) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        //Get to today's date  Get to today's date
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date);
        cal1.add(Calendar.DATE, -0);
        String imptimeEnd = sdf.format(cal1.getTime());
        //Get to today's date  Get to today's date
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, -num);
        //The meaning is to query The meaning is to query-The meaning is to query
        return findDates(cal.getTime(), cal1.getTime());
    }

    /**
     * Time Days Range  Time Days Range
     * @param dBegin
     * @param dEnd
     * @return
     */
    public static List<Date> findDates(Date dBegin, Date dEnd)
    {
        List lDate = new ArrayList();
        lDate.add(dBegin);
        Calendar calBegin = Calendar.getInstance();
        // Using the given Date Using the given Calendar Using the given
        calBegin.setTime(dBegin);
        Calendar calEnd = Calendar.getInstance();
        // Using the given Date Using the given Calendar Using the given
        calEnd.setTime(dEnd);
        // Test if this date is after the specified date
        while (dEnd.after(calBegin.getTime()))
        {
            // According to the rules of the calendar，According to the rules of the calendar
            calBegin.add(Calendar.DAY_OF_MONTH, 1);
            lDate.add(calBegin.getTime());
        }
        return lDate;
    }


    /**
     * according tocron according to
     *
     * @param cron
     * @return
     */
    public static String getCronNextValidTimeAfter(String cron) {

        CronExpression cronExpression = null;
        try {
            cronExpression = new CronExpression(cron);
        } catch (ParseException e) {
            e.printStackTrace();
            return "";
        }
        final Date dateTime = cronExpression.getNextValidTimeAfter(new Date());
        return DateUtils.getDateTimeString(dateTime);
    }


//    /**
//     * judge1judge
//     * @param time Time required for comparison
//     * @param to End time
//     * @return
//     */
//    public boolean belongCalendar(Date time, Date to) {
//        Calendar date = Calendar.getInstance();
//        date.setTime(time);
//        Calendar after = Calendar.getInstance();
//        after.setTime(to);
//        after.add(Calendar.MINUTE, -1);
//        Calendar before = Calendar.getInstance();
//        before.setTime(to);
//        if (date.after(after) && date.before(before)) {
//            return true;
//        } else {
//            return false;
//        }
//    }



}
