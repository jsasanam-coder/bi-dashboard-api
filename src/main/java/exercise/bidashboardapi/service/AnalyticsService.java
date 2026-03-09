package exercise.bidashboardapi.service;

import exercise.bidashboardapi.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Date;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AnalyticsService {

    private final JdbcTemplate jdbcTemplate;

    public AnalyticsService(@Qualifier("snowflakeDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Cacheable(value = "kpis", unless = "#result == null")
    public KPIResponse getKPIs() {
        long startTime = System.currentTimeMillis();
        log.info("Fetching KPIs from Snowflake");

        String sql = """
            SELECT
                SUM(total_amount) as total_revenue,
                COUNT(*) as total_orders,
                AVG(total_amount) as average_order_value,
                COUNT(DISTINCT customer_id) as total_customers
            FROM ORDERS
        """;

        Map<String, Object> result = jdbcTemplate.queryForMap(sql);

        // Get total products count
        Long totalProducts = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM PRODUCTS",
                Long.class
        );

        KPIResponse response = KPIResponse.builder()
                .totalRevenue(toDouble(result.get("TOTAL_REVENUE")))
                .totalOrders(((Number) result.get("TOTAL_ORDERS")).longValue())
                .averageOrderValue(toDouble(result.get("AVERAGE_ORDER_VALUE")))
                .totalCustomers(((Number) result.get("TOTAL_CUSTOMERS")).longValue())
                .totalProducts(totalProducts)
                .build();

        long duration = System.currentTimeMillis() - startTime;
        log.info("Fetched KPIs in {}ms - revenue={}, orders={}, customers={}",
                duration, response.getTotalRevenue(), response.getTotalOrders(), response.getTotalCustomers());

        return response;
    }

    @Cacheable(value = "salesTrends", key = "#period", unless = "#result == null")
    public List<SalesTrendDTO> getSalesTrends(String period) {
        long startTime = System.currentTimeMillis();
        log.info("Fetching sales trends for period: {}", period);

        String dateGroup = switch (period.toLowerCase()) {
            case "daily" -> "DATE(order_date)";
            case "monthly" -> "DATE_TRUNC('MONTH', order_date)";
            case "yearly" -> "DATE_TRUNC('YEAR', order_date)";
            default -> "DATE(order_date)";
        };

        String sql = String.format("""
            SELECT
                %s as TREND_DATE,
                SUM(total_amount) as REVENUE,
                COUNT(*) as ORDER_COUNT,
                AVG(total_amount) as AVERAGE_ORDER_VALUE
            FROM ORDERS
            GROUP BY %s
            ORDER BY TREND_DATE
        """, dateGroup, dateGroup);

        List<SalesTrendDTO> trends = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Date sqlDate = rs.getDate("TREND_DATE");
            return SalesTrendDTO.builder()
                    .date(sqlDate != null ? sqlDate.toLocalDate() : null)
                    .revenue(toDouble(rs.getObject("REVENUE")))
                    .orderCount(((Number) rs.getObject("ORDER_COUNT")).longValue())
                    .averageOrderValue(toDouble(rs.getObject("AVERAGE_ORDER_VALUE")))
                    .build();
        });

        long duration = System.currentTimeMillis() - startTime;
        log.info("Fetched {} sales trends for period={} in {}ms", trends.size(), period, duration);

        return trends;
    }

    @Cacheable(value = "topProducts", key = "#category + '_' + #limit", unless = "#result == null")
    public List<TopProductDTO> getTopProducts(String category, int limit) {
        long startTime = System.currentTimeMillis();
        log.info("Fetching top {} products for category: {}", limit, category);

        String categoryFilter = (category != null && !category.isEmpty())
                ? "WHERE p.category = ?"
                : "";

        String sql = String.format("""
            SELECT
                p.name as product_name,
                p.category,
                SUM(oi.quantity) as total_units_sold,
                SUM(oi.quantity * oi.price) as total_revenue,
                p.stock as current_stock
            FROM PRODUCTS p
            JOIN ORDER_ITEMS oi ON p.product_id = oi.product_id
            %s
            GROUP BY p.name, p.category, p.stock
            ORDER BY total_revenue DESC
            LIMIT ?
        """, categoryFilter);

        List<TopProductDTO> products;
        if (category != null && !category.isEmpty()) {
            products = jdbcTemplate.query(sql, (rs, rowNum) ->
                            TopProductDTO.builder()
                                    .productName(rs.getString("PRODUCT_NAME"))
                                    .category(rs.getString("CATEGORY"))
                                    .totalUnitsSold(((Number) rs.getObject("TOTAL_UNITS_SOLD")).longValue())
                                    .totalRevenue(toDouble(rs.getObject("TOTAL_REVENUE")))
                                    .currentStock(((Number) rs.getObject("CURRENT_STOCK")).intValue())
                                    .build(),
                    category, limit
            );
        } else {
            products = jdbcTemplate.query(sql, (rs, rowNum) ->
                            TopProductDTO.builder()
                                    .productName(rs.getString("PRODUCT_NAME"))
                                    .category(rs.getString("CATEGORY"))
                                    .totalUnitsSold(((Number) rs.getObject("TOTAL_UNITS_SOLD")).longValue())
                                    .totalRevenue(toDouble(rs.getObject("TOTAL_REVENUE")))
                                    .currentStock(((Number) rs.getObject("CURRENT_STOCK")).intValue())
                                    .build(),
                    limit
            );
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Fetched {} top products in {}ms", products.size(), duration);

        return products;
    }

    @Cacheable(value = "customerSegments", unless = "#result == null")
    public List<CustomerSegmentDTO> getCustomerSegments() {
        long startTime = System.currentTimeMillis();
        log.info("Fetching customer segments");

        String sql = """
            WITH customer_totals AS (
                SELECT
                    c.customer_id,
                    COALESCE(SUM(o.total_amount), 0) as customer_total
                FROM CUSTOMERS c
                LEFT JOIN ORDERS o ON c.customer_id = o.customer_id
                GROUP BY c.customer_id
            ),
            customer_segments AS (
                SELECT
                    customer_id,
                    customer_total,
                    CASE
                        WHEN customer_total > 5000 THEN 'VIP'
                        WHEN customer_total > 1000 THEN 'REGULAR'
                        ELSE 'NEW'
                    END as SEGMENT
                FROM customer_totals
            )
            SELECT
                SEGMENT,
                COUNT(*) as CUSTOMER_COUNT,
                SUM(customer_total) as TOTAL_REVENUE,
                AVG(customer_total) as AVERAGE_SPENDING
            FROM customer_segments
            GROUP BY SEGMENT
            ORDER BY TOTAL_REVENUE DESC
        """;

        List<CustomerSegmentDTO> segments = jdbcTemplate.query(sql, (rs, rowNum) ->
                CustomerSegmentDTO.builder()
                        .segment(rs.getString("SEGMENT"))
                        .customerCount(((Number) rs.getObject("CUSTOMER_COUNT")).longValue())
                        .totalRevenue(toDouble(rs.getObject("TOTAL_REVENUE")))
                        .averageSpending(toDouble(rs.getObject("AVERAGE_SPENDING")))
                        .build()
        );

        long duration = System.currentTimeMillis() - startTime;
        log.info("Fetched {} customer segments in {}ms", segments.size(), duration);

        return segments;
    }

    @Cacheable(value = "geographicSales", unless = "#result == null")
    public List<GeographicSalesDTO> getGeographicSales() {
        long startTime = System.currentTimeMillis();
        log.info("Fetching geographic sales data");

        String sql = """
            SELECT
                c.country,
                c.city,
                COUNT(o.order_id) as order_count,
                SUM(o.total_amount) as total_revenue,
                COUNT(DISTINCT c.customer_id) as customer_count
            FROM CUSTOMERS c
            LEFT JOIN ORDERS o ON c.customer_id = o.customer_id
            GROUP BY c.country, c.city
            ORDER BY total_revenue DESC
        """;

        List<GeographicSalesDTO> salesData = jdbcTemplate.query(sql, (rs, rowNum) ->
                GeographicSalesDTO.builder()
                        .country(rs.getString("COUNTRY"))
                        .city(rs.getString("CITY"))
                        .orderCount(((Number) rs.getObject("ORDER_COUNT")).longValue())
                        .totalRevenue(toDouble(rs.getObject("TOTAL_REVENUE")))
                        .customerCount(((Number) rs.getObject("CUSTOMER_COUNT")).longValue())
                        .build()
        );

        long duration = System.currentTimeMillis() - startTime;
        log.info("Fetched geographic sales for {} locations in {}ms", salesData.size(), duration);

        return salesData;
    }

    // Methods for hybrid endpoints
    public Map<String, Object> getProductAnalytics(Integer productId) {
        long startTime = System.currentTimeMillis();
        log.info("Fetching analytics for productId={}", productId);

        String sql = """
            SELECT
                SUM(oi.quantity) as total_units_sold,
                SUM(oi.quantity * oi.price) as total_revenue,
                AVG(oi.price) as average_selling_price
            FROM ORDER_ITEMS oi
            WHERE oi.product_id = ?
        """;

        Map<String, Object> analytics = jdbcTemplate.queryForMap(sql, productId);
        long duration = System.currentTimeMillis() - startTime;
        log.info("Fetched product analytics for productId={} in {}ms", productId, duration);

        return analytics;
    }

    public Map<String, Object> getCustomerAnalytics(Integer customerId) {
        long startTime = System.currentTimeMillis();
        log.info("Fetching analytics for customerId={}", customerId);

        String sql = """
            SELECT
                COUNT(o.order_id) as lifetime_orders,
                SUM(o.total_amount) as lifetime_value,
                AVG(o.total_amount) as average_order_value,
                CASE
                    WHEN SUM(o.total_amount) > 5000 THEN 'VIP'
                    WHEN SUM(o.total_amount) > 1000 THEN 'REGULAR'
                    ELSE 'NEW'
                END as segment
            FROM ORDERS o
            WHERE o.customer_id = ?
            GROUP BY o.customer_id
        """;

        try {
            Map<String, Object> analytics = jdbcTemplate.queryForMap(sql, customerId);
            long duration = System.currentTimeMillis() - startTime;
            log.info("Fetched customer analytics for customerId={} in {}ms", customerId, duration);
            return analytics;
        } catch (Exception e) {
            log.warn("No analytics found for customerId={}, returning default values", customerId);
            return Map.of(
                    "lifetime_orders", 0L,
                    "lifetime_value", 0.0,
                    "average_order_value", 0.0,
                    "segment", "NEW"
            );
        }
    }

    // Helper method to safely convert Number types (BigDecimal, etc.) to Double
    private Double toDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }
}
