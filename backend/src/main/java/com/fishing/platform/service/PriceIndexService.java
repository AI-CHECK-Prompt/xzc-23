package com.fishing.platform.service;

import com.fishing.platform.entity.PriceIndex;
import com.fishing.platform.entity.PurchaseRecord;
import com.fishing.platform.entity.Vessel;
import com.fishing.platform.repository.PriceIndexRepository;
import com.fishing.platform.repository.PurchaseRecordRepository;
import com.fishing.platform.repository.VesselRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 渔获价格指数计算服务
 * <p>
 * 维度：海区 + 品种 + 规格 + 季节 + 周期
 * 异常剔除：IQR 方法（Q1-1.5×IQR, Q3+1.5×IQR），小样本回退 P5/P95
 * 输出：中位数 P50、P25、P75、P5/P95、样本数、被剔除数
 */
@Service
public class PriceIndexService {

    private static final Logger log = LoggerFactory.getLogger(PriceIndexService.class);

    @Autowired private PurchaseRecordRepository purchaseRepo;
    @Autowired private VesselRepository vesselRepo;
    @Autowired private PriceIndexRepository indexRepo;

    private static final int SCALE = 2;

    /**
     * 对单个周期（DAY/WEEK/MONTH 的某个 key）触发计算。
     * 已存在记录会被覆盖（保证幂等）。
     */
    public List<PriceIndex> calculate(String periodType, String periodKey) {
        // 1) 解析周期边界
        LocalDateTime[] range = resolvePeriodRange(periodType, periodKey);
        if (range == null) {
            log.warn("【价格指数】无法解析周期 periodType={} periodKey={}", periodType, periodKey);
            return Collections.emptyList();
        }
        LocalDateTime from = range[0];
        LocalDateTime to = range[1];
        log.info("【价格指数-计算】periodType={} periodKey={} 时间窗 [{} -> {}]", periodType, periodKey, from, to);

        // 2) 拉取周期内采购数据
        List<PurchaseRecord> records = purchaseRepo.findAll().stream()
                .filter(r -> r.getPurchaseTime() != null)
                .filter(r -> !r.getPurchaseTime().isBefore(from) && r.getPurchaseTime().isBefore(to))
                .collect(Collectors.toList());
        log.info("【价格指数-计算】周期内采购记录数={}", records.size());

        if (records.isEmpty()) {
            return Collections.emptyList();
        }

        // 3) 准备 vesselId -> seaArea 映射
        Map<String, String> seaAreaMap = vesselRepo.findAll().stream()
                .collect(Collectors.toMap(Vessel::getId, Vessel::getSeaAreaName, (a, b) -> a));

        // 4) 按维度分组
        Map<DimensionKey, List<BigDecimal>> grouped = new HashMap<>();
        for (PurchaseRecord r : records) {
            if (r.getPrice() == null || r.getPrice().signum() <= 0) continue;
            String seaArea = seaAreaMap.getOrDefault(r.getVesselId(), "未知海区");
            String spec = deriveSpecification(r.getWeight());
            String season = deriveSeason(r.getPurchaseTime());
            DimensionKey k = new DimensionKey(seaArea, r.getSpecies(), spec, season);
            grouped.computeIfAbsent(k, x -> new ArrayList<>()).add(r.getPrice());
        }
        log.info("【价格指数-计算】分组数={}", grouped.size());

        // 5) 删除该周期旧记录，保证幂等
        indexRepo.deleteByPeriod(periodType, periodKey);

        // 6) 计算并保存
        List<PriceIndex> results = new ArrayList<>();
        for (Map.Entry<DimensionKey, List<BigDecimal>> e : grouped.entrySet()) {
            Stats stats = calcStats(e.getValue());
            PriceIndex idx = new PriceIndex();
            idx.setId(UUID.randomUUID().toString().replace("-", ""));
            idx.setSeaArea(e.getKey().seaArea);
            idx.setSpecies(e.getKey().species);
            idx.setSpecification(e.getKey().specification);
            idx.setSeason(e.getKey().season);
            idx.setPeriodType(periodType);
            idx.setPeriodKey(periodKey);
            idx.setMedian(stats.median);
            idx.setP25(stats.p25);
            idx.setP75(stats.p75);
            idx.setP5(stats.p5);
            idx.setP95(stats.p95);
            idx.setSampleSize(stats.total);
            idx.setAnomalyFiltered(stats.filtered);
            idx.setCalculatedAt(LocalDateTime.now());
            results.add(idx);
        }
        indexRepo.saveAll(results);
        log.info("【价格指数-计算】写入 {} 条指数记录", results.size());
        return results;
    }

    /**
     * 触发最近一期：昨日 / 上周 / 上月
     */
    public Map<String, Integer> calculateLatest() {
        Map<String, Integer> result = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();

        // 昨日
        String dayKey = today.minusDays(1).toString();
        result.put("DAY-" + dayKey, calculate("DAY", dayKey).size());

        // 上周
        LocalDate lastWeekMon = today.minusWeeks(1).with(DayOfWeek.MONDAY);
        String weekKey = formatWeek(lastWeekMon);
        result.put("WEEK-" + weekKey, calculate("WEEK", weekKey).size());

        // 上月
        LocalDate lastMonth = today.minusMonths(1).withDayOfMonth(1);
        String monthKey = String.format("%04d-%02d", lastMonth.getYear(), lastMonth.getMonthValue());
        result.put("MONTH-" + monthKey, calculate("MONTH", monthKey).size());

        log.info("【价格指数-触发】最新一期结果：{}", result);
        return result;
    }

    /**
     * 重建历史：对 [from, to) 区间内所有 DAY 周期重算，
     * 再据此聚合出 WEEK 和 MONTH。
     */
    public Map<String, Integer> rebuild(LocalDate from, LocalDate to) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (from == null || to == null || !from.isBefore(to)) {
            log.warn("【价格指数-重建】日期范围无效 from={} to={}", from, to);
            return result;
        }
        log.info("【价格指数-重建】日期范围 [{} -> {})", from, to);

        // DAY：逐日计算
        Set<String> weekKeys = new TreeSet<>();
        Set<String> monthKeys = new TreeSet<>();
        LocalDate cursor = from;
        while (cursor.isBefore(to)) {
            String dayKey = cursor.toString();
            int n = calculate("DAY", dayKey).size();
            result.put("DAY-" + dayKey, n);
            weekKeys.add(formatWeek(cursor));
            monthKeys.add(String.format("%04d-%02d", cursor.getYear(), cursor.getMonthValue()));
            cursor = cursor.plusDays(1);
        }
        // WEEK：按所在周聚合
        for (String wk : weekKeys) {
            int n = calculate("WEEK", wk).size();
            result.put("WEEK-" + wk, n);
        }
        // MONTH
        for (String mk : monthKeys) {
            int n = calculate("MONTH", mk).size();
            result.put("MONTH-" + mk, n);
        }
        log.info("【价格指数-重建】完成，共 {} 个周期", result.size());
        return result;
    }

    public List<PriceIndex> query(String seaArea, String species, String specification, String periodType) {
        return indexRepo.query(seaArea, species, specification, periodType);
    }

    public List<PriceIndex> trend(String seaArea, String species, String specification, String season, String periodType) {
        return indexRepo.trend(seaArea, species, specification, season, periodType);
    }

    // ---------------- 内部工具 ----------------

    /** 规格派生：小<1kg、中 1-5kg、大>5kg */
    public static String deriveSpecification(BigDecimal weight) {
        if (weight == null) return "未分类";
        BigDecimal w = weight;
        if (w.compareTo(new BigDecimal("1")) < 0) return "小";
        if (w.compareTo(new BigDecimal("5")) <= 0) return "中";
        return "大";
    }

    /** 季节派生：3-5春、6-8夏、9-11秋、12-2冬 */
    public static String deriveSeason(LocalDateTime t) {
        int m = t.getMonthValue();
        if (m >= 3 && m <= 5) return "春";
        if (m >= 6 && m <= 8) return "夏";
        if (m >= 9 && m <= 11) return "秋";
        return "冬";
    }

    public static String formatWeek(LocalDate date) {
        WeekFields wf = WeekFields.ISO;
        int week = date.get(wf.weekOfWeekBasedYear());
        int year = date.get(wf.weekBasedYear());
        return String.format("%04d-W%02d", year, week);
    }

    /** 解析周期边界：[from, to) */
    public static LocalDateTime[] resolvePeriodRange(String periodType, String periodKey) {
        try {
            switch (periodType) {
                case "DAY": {
                    LocalDate d = LocalDate.parse(periodKey);
                    return new LocalDateTime[]{d.atStartOfDay(), d.plusDays(1).atStartOfDay()};
                }
                case "WEEK": {
                    // periodKey 形如 2026-W29
                    String[] parts = periodKey.split("-W");
                    int year = Integer.parseInt(parts[0]);
                    int week = Integer.parseInt(parts[1]);
                    WeekFields wf = WeekFields.ISO;
                    LocalDate monday = LocalDate.now()
                            .with(wf.weekBasedYear(), year)
                            .with(wf.weekOfWeekBasedYear(), week)
                            .with(DayOfWeek.MONDAY);
                    return new LocalDateTime[]{monday.atStartOfDay(), monday.plusDays(7).atStartOfDay()};
                }
                case "MONTH": {
                    String[] parts = periodKey.split("-");
                    int year = Integer.parseInt(parts[0]);
                    int month = Integer.parseInt(parts[1]);
                    LocalDate first = LocalDate.of(year, month, 1);
                    return new LocalDateTime[]{first.atStartOfDay(), first.plusMonths(1).atStartOfDay()};
                }
                default:
                    return null;
            }
        } catch (Exception ex) {
            log.error("【价格指数】解析周期失败 periodType={} periodKey={}", periodType, periodKey, ex);
            return null;
        }
    }

    /** 统计结果：包含异常剔除前后的样本数与分位数 */
    public static class Stats {
        public BigDecimal median, p25, p75, p5, p95;
        public int total;
        public int filtered;
    }

    /**
     * 异常剔除 + 分位数计算
     * - N >= 10: IQR 方法
     * - N < 10: 保留全部样本（不剔除，避免过度清洗）
     */
    public static Stats calcStats(List<BigDecimal> prices) {
        Stats s = new Stats();
        s.total = prices.size();
        if (prices.isEmpty()) return s;

        List<BigDecimal> sorted = prices.stream()
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());

        List<BigDecimal> cleaned = sorted;
        if (sorted.size() >= 10) {
            BigDecimal q1 = percentile(sorted, 25);
            BigDecimal q3 = percentile(sorted, 75);
            BigDecimal iqr = q3.subtract(q1);
            BigDecimal lower = q1.subtract(iqr.multiply(new BigDecimal("1.5")));
            BigDecimal upper = q3.add(iqr.multiply(new BigDecimal("1.5")));
            cleaned = sorted.stream()
                    .filter(v -> v.compareTo(lower) >= 0 && v.compareTo(upper) <= 0)
                    .collect(Collectors.toList());
            s.filtered = sorted.size() - cleaned.size();
        } else {
            s.filtered = 0;
        }

        s.median = percentile(cleaned, 50);
        s.p25 = percentile(cleaned, 25);
        s.p75 = percentile(cleaned, 75);
        s.p5 = percentile(cleaned, 5);
        s.p95 = percentile(cleaned, 95);
        return s;
    }

    /**
     * 线性插值分位数（与 numpy 默认 quantile 行为一致，Type 7）。
     * 输入必须已排序。
     */
    public static BigDecimal percentile(List<BigDecimal> sorted, double p) {
        if (sorted.isEmpty()) return BigDecimal.ZERO;
        if (sorted.size() == 1) return sorted.get(0);
        double rank = p / 100.0 * (sorted.size() - 1);
        int lo = (int) Math.floor(rank);
        int hi = (int) Math.ceil(rank);
        if (lo == hi) return sorted.get(lo).setScale(SCALE, RoundingMode.HALF_UP);
        double w = rank - lo;
        BigDecimal a = sorted.get(lo);
        BigDecimal b = sorted.get(hi);
        BigDecimal v = a.add(b.subtract(a).multiply(BigDecimal.valueOf(w)));
        return v.setScale(SCALE, RoundingMode.HALF_UP);
    }

    private static class DimensionKey {
        final String seaArea, species, specification, season;
        DimensionKey(String a, String b, String c, String d) { this.seaArea = a; this.species = b; this.specification = c; this.season = d; }
        @Override public boolean equals(Object o) {
            if (!(o instanceof DimensionKey)) return false;
            DimensionKey k = (DimensionKey) o;
            return seaArea.equals(k.seaArea) && species.equals(k.species) && specification.equals(k.specification) && season.equals(k.season);
        }
        @Override public int hashCode() { return Objects.hash(seaArea, species, specification, season); }
    }
}
