package com.fishing.platform.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PriceIndexService 单元测试
 * 覆盖：分位数计算、异常剔除、规格/季节派生、周期解析、确定性。
 * <p>
 * 作为后续回归测试用例保留（参见 CLAUDE.md 中"测试用例保留"约定）。
 */
class PriceIndexServiceTest {

    @Test
    void testPercentile_basic() {
        // [10, 20, 30, 40, 50] P50 = 30
        List<BigDecimal> sorted = Arrays.asList(
                bd("10"), bd("20"), bd("30"), bd("40"), bd("50")
        );
        assertEquals(new BigDecimal("30.00"), PriceIndexService.percentile(sorted, 50));
        // P25 = 20, P75 = 40 (linear interpolation)
        assertEquals(new BigDecimal("20.00"), PriceIndexService.percentile(sorted, 25));
        assertEquals(new BigDecimal("40.00"), PriceIndexService.percentile(sorted, 75));
    }

    @Test
    void testPercentile_interpolation() {
        // [1, 2, 3, 4] P75 → rank = 0.75 * 3 = 2.25 → 3 + 0.25*(4-3) = 3.25
        List<BigDecimal> sorted = Arrays.asList(bd("1"), bd("2"), bd("3"), bd("4"));
        assertEquals(new BigDecimal("3.25"), PriceIndexService.percentile(sorted, 75));
    }

    @Test
    void testCalcStats_filtersOutliers() {
        // 11 个样本：前 10 个在 [20, 30] 区间，1 个明显异常 1000
        List<BigDecimal> prices = Arrays.asList(
                bd("20"), bd("22"), bd("24"), bd("25"), bd("26"),
                bd("27"), bd("28"), bd("29"), bd("29.5"), bd("30"),
                bd("1000")
        );
        PriceIndexService.Stats stats = PriceIndexService.calcStats(prices);
        assertEquals(11, stats.total);
        // 1000 应被 IQR 剔除
        assertEquals(1, stats.filtered);
        assertNotNull(stats.median);
    }

    @Test
    void testCalcStats_smallSample_noFilter() {
        // 5 个样本，小样本不过滤
        List<BigDecimal> prices = Arrays.asList(bd("10"), bd("20"), bd("30"), bd("40"), bd("50"));
        PriceIndexService.Stats stats = PriceIndexService.calcStats(prices);
        assertEquals(5, stats.total);
        assertEquals(0, stats.filtered);
        assertEquals(new BigDecimal("30.00"), stats.median);
    }

    @Test
    void testCalcStats_deterministic() {
        // 同样的输入两次计算，结果完全相同
        List<BigDecimal> prices = Arrays.asList(
                bd("15"), bd("18"), bd("20"), bd("22"), bd("25"),
                bd("25"), bd("28"), bd("30"), bd("32"), bd("35"),
                bd("200")
        );
        PriceIndexService.Stats a = PriceIndexService.calcStats(prices);
        PriceIndexService.Stats b = PriceIndexService.calcStats(prices);
        assertEquals(a.median, b.median);
        assertEquals(a.p25, b.p25);
        assertEquals(a.p75, b.p75);
        assertEquals(a.p5, b.p5);
        assertEquals(a.p95, b.p95);
        assertEquals(a.filtered, b.filtered);
    }

    @Test
    void testDeriveSpecification() {
        assertEquals("小", PriceIndexService.deriveSpecification(bd("0.5")));
        assertEquals("小", PriceIndexService.deriveSpecification(bd("0.99")));
        assertEquals("中", PriceIndexService.deriveSpecification(bd("1")));
        assertEquals("中", PriceIndexService.deriveSpecification(bd("5")));
        assertEquals("大", PriceIndexService.deriveSpecification(bd("5.01")));
        assertEquals("大", PriceIndexService.deriveSpecification(bd("100")));
        assertEquals("未分类", PriceIndexService.deriveSpecification(null));
    }

    @Test
    void testDeriveSeason() {
        assertEquals("春", PriceIndexService.deriveSeason(ldt(2026, 3, 15)));
        assertEquals("春", PriceIndexService.deriveSeason(ldt(2026, 5, 31)));
        assertEquals("夏", PriceIndexService.deriveSeason(ldt(2026, 6, 1)));
        assertEquals("夏", PriceIndexService.deriveSeason(ldt(2026, 8, 31)));
        assertEquals("秋", PriceIndexService.deriveSeason(ldt(2026, 9, 1)));
        assertEquals("秋", PriceIndexService.deriveSeason(ldt(2026, 11, 30)));
        assertEquals("冬", PriceIndexService.deriveSeason(ldt(2026, 12, 1)));
        assertEquals("冬", PriceIndexService.deriveSeason(ldt(2026, 1, 15)));
        assertEquals("冬", PriceIndexService.deriveSeason(ldt(2026, 2, 28)));
    }

    @Test
    void testResolvePeriodRange_day() {
        LocalDateTime[] range = PriceIndexService.resolvePeriodRange("DAY", "2026-07-23");
        assertEquals(LocalDateTime.of(2026, 7, 23, 0, 0), range[0]);
        assertEquals(LocalDateTime.of(2026, 7, 24, 0, 0), range[1]);
    }

    @Test
    void testResolvePeriodRange_month() {
        LocalDateTime[] range = PriceIndexService.resolvePeriodRange("MONTH", "2026-07");
        assertEquals(LocalDateTime.of(2026, 7, 1, 0, 0), range[0]);
        assertEquals(LocalDateTime.of(2026, 8, 1, 0, 0), range[1]);
    }

    @Test
    void testFormatWeek() {
        // 2026-07-23 是周四，所在 ISO 周为 2026-W30
        assertEquals("2026-W30", PriceIndexService.formatWeek(LocalDate.of(2026, 7, 23)));
    }

    private static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }

    private static LocalDateTime ldt(int y, int m, int d) {
        return LocalDateTime.of(y, m, d, 10, 0);
    }
}
