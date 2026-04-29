package com.enterprise.ad.common.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

/**
 * 日期时间公共工具类
 * ★ 抽取自各 Controller 中重复的日期范围计算逻辑
 */
public final class DateUtil {

    private DateUtil() {}

    /**
     * LocalDate → 当天开始时间 00:00:00
     */
    public static LocalDateTime startOfDay(LocalDate date) {
        return date != null ? date.atStartOfDay() : null;
    }

    /**
     * LocalDate → 当天结束时间 23:59:59
     */
    public static LocalDateTime endOfDay(LocalDate date) {
        return date != null ? date.atTime(23, 59, 59) : null;
    }

    /**
     * 根据 period 关键字解析时间范围
     *
     * @param period  today / week / month / year
     * @param today   基准日期
     * @return [startDateTime, endDateTime]
     */
    public static LocalDateTime[] parsePeriod(String period, LocalDate today) {
        return switch (period != null ? period : "month") {
            case "today" -> new LocalDateTime[]{
                today.atStartOfDay(), today.atTime(23, 59, 59)};
            case "week" -> new LocalDateTime[]{
                today.minusDays(today.getDayOfWeek().getValue() - 1).atStartOfDay(),
                today.atTime(23, 59, 59)};
            case "year" -> new LocalDateTime[]{
                LocalDate.of(today.getYear(), 1, 1).atStartOfDay(),
                today.atTime(23, 59, 59)};
            default -> new LocalDateTime[]{ // month
                LocalDate.of(today.getYear(), today.getMonth(), 1).atStartOfDay(),
                today.atTime(23, 59, 59)};
        };
    }

    /**
     * 填充日期默认值：startDate 默认本月1号，endDate 默认今天
     */
    public static LocalDate[] fillMonthRange(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        return new LocalDate[]{
            startDate != null ? startDate : today.withDayOfMonth(1),
            endDate != null ? endDate : today
        };
    }

    /**
     * 填充日期默认值：startDate 默认近30天，endDate 默认今天
     */
    public static LocalDate[] fillRecentRange(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        return new LocalDate[]{
            startDate != null ? startDate : today.minusDays(30),
            endDate != null ? endDate : today
        };
    }

    /**
     * 填充日期默认值：startDate 默认近7天，endDate 默认今天
     */
    public static LocalDate[] fillWeekRange(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        return new LocalDate[]{
            startDate != null ? startDate : today.minusDays(7),
            endDate != null ? endDate : today
        };
    }

    /**
     * 获取本周一 00:00:00
     */
    public static LocalDateTime thisWeekStart() {
        return LocalDate.now()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .atStartOfDay();
    }

    /**
     * 获取本月1号 00:00:00
     */
    public static LocalDateTime thisMonthStart() {
        return LocalDate.now().withDayOfMonth(1).atStartOfDay();
    }

    /**
     * 获取昨天 00:00:00
     */
    public static LocalDateTime yesterdayStart() {
        return LocalDate.now().minusDays(1).atStartOfDay();
    }

    /**
     * 获取昨天 23:59:59
     */
    public static LocalDateTime yesterdayEnd() {
        return LocalDate.now().minusDays(1).atTime(23, 59, 59);
    }
}
