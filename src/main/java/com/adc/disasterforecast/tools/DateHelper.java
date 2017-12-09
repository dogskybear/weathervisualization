package com.adc.disasterforecast.tools;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DateHelper {

    public static String getPostponeDateByHour(int year, int month, int date, int hourOfDay, int minute, int second, int delayHour) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

        Calendar base = Calendar.getInstance();
        base.set(year, month - 1, date, hourOfDay, minute, second);
        base.add(Calendar.HOUR, delayHour);

        return simpleDateFormat.format(base.getTime());
    }

    public static String getPostponeDateByHour(String date, int delayHour) {
        return getPostponeDateByHour(Integer.valueOf(date.substring(0, 4)),
                Integer.valueOf(date.substring(4, 6)),
                Integer.valueOf(date.substring(6, 8)),
                Integer.valueOf(date.substring(8, 10)),
                Integer.valueOf(date.substring(10, 12)),
                Integer.valueOf(date.substring(12, 14)),
                delayHour);
    }

    public static String getPostponeDateByMonth(int year, int month, int date, int hourOfDay, int minute, int second, int delayMonth) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

        Calendar base = Calendar.getInstance();
        base.set(year, month - 1, date, hourOfDay, minute, second);
        base.add(Calendar.MONTH, delayMonth);

        return simpleDateFormat.format(base.getTime());
    }

    public static String getWarningDate(String date) {
        // 2017-08-04T13:15:00
        String[] parts = date.split("T");
        String[] dates = parts[0].split("-");
        String[] times = parts[1].split(":");
        return dates[1] + "/" + dates[2] + " " + times[0] + ":" + times[1];
    }

    public static String getTimeMillis(String date) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        Calendar cal = Calendar.getInstance();
        try{
            cal.setTime(simpleDateFormat.parse(date));
        }catch (ParseException e){
            return null;
        }
        return String.valueOf(cal.getTimeInMillis());
    }

    /**
    * @Description 获取时间节点对应的当前小时
    * @Author lilin
    * @Create 2017/12/6 17:58
    **/
    public static String getNowHour(String date) {
        return date.substring(0, 10) + "0000";
    }
    /**
    * @Description 获取当前时间节点的下个小时
    * @Author lilin
    * @Create 2017/12/6 17:57
    **/
    public static String getNextHour(String date) {
        if ("20150616162200".equals(date)) {
            return "20150616170000";
        }
        if ("20150616235300".equals(date)) {
            return "20150617000000";
        }
        if ("20150617020000".equals(date)) {
            return "20150617030000";
        }
        if ("20150617031200".equals(date)) {
            return "20150617040000";
        }
        if ("20150617090000".equals(date)) {
            return "20150617100000";
        }
        return "";
    }

    public static long getPostponeDateByDay(int delayDay) {
        Calendar base = Calendar.getInstance();
        base.add(Calendar.DATE, delayDay);

        return base.getTimeInMillis();
    }

    public static String getCurrentHourInString() {
        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR);
        return hour > 9 ? "" + hour : "0" + hour;
    }

    private static String getNowYear(String date) {
        return date.substring(0, 4) + "0101000000";
    }

    public static String getNowDay(String date) {
        return date.substring(0, 8) + "000000";
    }

    public static String getNowMinute(String date) {
        return date.substring(0, 12) + "00";
    }

    public static String getCurrentTimeInString(String accuracy) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        Calendar base = Calendar.getInstance();
        String date = simpleDateFormat.format(base.getTime());

        switch (accuracy) {
            case "minute": return getNowMinute(date);
            case "hour": return getNowHour(date);
            case "day": return getNowDay(date);
            case "year": return getNowYear(date);
            default: return null;
        }
    }

    public static long getPostponeDateByHourInLong(int year, int month, int date, int hourOfDay, int minute, int second, int delayHour) {
        Calendar base = Calendar.getInstance();
        base.set(year, month - 1, date, hourOfDay, minute, second);
        base.add(Calendar.HOUR, delayHour);

        return base.getTimeInMillis();
    }

    public static long getPostponeDateByHourInLong(String date,int delayHour) {
        return getPostponeDateByHourInLong(Integer.valueOf(date.substring(0, 4)),
                Integer.valueOf(date.substring(4, 6)),
                Integer.valueOf(date.substring(6, 8)),
                Integer.valueOf(date.substring(8, 10)),
                Integer.valueOf(date.substring(10, 12)),
                Integer.valueOf(date.substring(12, 14)),
                delayHour);
    }

    public static String getPostponeDateByMinute(int year, int month, int date, int hourOfDay, int minute, int second, int delayMinute) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

        Calendar base = Calendar.getInstance();
        base.set(year, month - 1, date, hourOfDay, minute, second);
        base.add(Calendar.MINUTE, delayMinute);

        return simpleDateFormat.format(base.getTime());
    }

    public static String getPostponeDateByMinute(String date, int delayMinute) {
        return getPostponeDateByMinute(Integer.valueOf(date.substring(0, 4)),
                Integer.valueOf(date.substring(4, 6)),
                Integer.valueOf(date.substring(6, 8)),
                Integer.valueOf(date.substring(8, 10)),
                Integer.valueOf(date.substring(10, 12)),
                Integer.valueOf(date.substring(12, 14)),
                delayMinute);
    }

    public static long getDateInLong(String date) {
        // 2017-08-04T13:15:00
        String[] parts = date.split("T");
        String[] dates = parts[0].split("-");
        String[] times = parts[1].split(":");

        return getPostponeDateByHourInLong(dates[0] + dates[1] + dates[2] + times[0] + times[1] + times[2], 0);
    }

    public static long getDateInLongByHour(String date) {
        // 2017-08-04T13:15:00
        String[] parts = date.split("T");
        String[] dates = parts[0].split("-");
        String[] times = parts[1].split(":");

        return getPostponeDateByHourInLong(dates[0] + dates[1] + dates[2] + times[0] + "0000", 0);
    }

    public static String getYear(String date) {
        // 2017-08-04T13:15:00
        String[] parts = date.split("T");
        String[] dates = parts[0].split("-");

        return dates[0];
    }

    public static String getMonth(String date) {
        // 2017-08-04T13:15:00
        String[] parts = date.split("T");
        String[] dates = parts[0].split("-");

        return dates[1];
    }

    public static String getHour(String date) {
        // 2017-08-04T13:15:00
        String[] parts = date.split("T");
        String[] dates = parts[1].split(":");

        return dates[0];
    }

    public static void main(String[] args) {
        System.out.println(Calendar.getInstance().get(Calendar.MONTH));
    }
}
