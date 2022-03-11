package io.kylin.mdx.insight.common.util;

import lombok.extern.slf4j.Slf4j;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

@Slf4j
public class DateUtils {
    private static final ThreadLocal<SimpleDateFormat> QUERY_DATE_FORMAT = ThreadLocal.withInitial(()
            -> new SimpleDateFormat("yyyyMMdd_HHmmss"));

    private static final ThreadLocal<SimpleDateFormat> CONDITION_DATE_FORMAT = ThreadLocal.withInitial(()
            -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

    public static boolean isDateString(String dateValue) {
        try {
            Date date = CONDITION_DATE_FORMAT.get().parse(dateValue);
            return dateValue.equals(CONDITION_DATE_FORMAT.get().format(date));
        } catch (Exception e) {
            return false;
        }
    }

    public static String formatCondition(Date date) {
        return CONDITION_DATE_FORMAT.get().format(date);
    }

    public static Date parseCondition(String date) throws ParseException {
        return CONDITION_DATE_FORMAT.get().parse(date);
    }


    public static String format(Date date) {
        return QUERY_DATE_FORMAT.get().format(date);
    }

    public static String formatNow() {
        Date date = new Date(System.currentTimeMillis());
        return QUERY_DATE_FORMAT.get().format(date);
    }

    public static Date parse(String dateStr) throws ParseException {
        return QUERY_DATE_FORMAT.get().parse(dateStr);
    }

    public static Date pastDays(String queryTime, int days) {
        Date date = null;
        try {
            Date queryDate = DateUtils.parse(queryTime);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(queryDate);
            calendar.add(Calendar.DATE, days);
            date = calendar.getTime();
        } catch (ParseException e) {
            log.error("error happened when parse time {}", queryTime);
        }
        return date;
    }
}
