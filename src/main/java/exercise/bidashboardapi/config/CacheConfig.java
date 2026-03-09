package exercise.bidashboardapi.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
@Slf4j
public class CacheConfig {
    // Spring Boot will autoconfigure simple cache
    // We defined cache names in application.yml

    @PostConstruct
    public void init() {
        log.info("Caching enabled with Spring simple cache");
        log.info("Configured cache names: analytics, kpis, salesTrends, topProducts, customerSegments, geographicSales");
    }
}
