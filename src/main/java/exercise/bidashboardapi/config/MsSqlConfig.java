package exercise.bidashboardapi.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "exercise.bidashboardapi.repository",
        entityManagerFactoryRef = "mssqlEntityManagerFactory",
        transactionManagerRef = "mssqlTransactionManager"
)
@Slf4j
public class MsSqlConfig {

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Primary
    @Bean(name = "mssqlDataSource")
    public DataSource mssqlDataSource() {
        log.info("Initializing SQL Server DataSource: url={}, username={}, maxPoolSize=10, minIdle=2",
                maskUrl(url), username);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);

        HikariDataSource dataSource = new HikariDataSource(config);
        log.info("SQL Server DataSource initialized successfully");
        return dataSource;
    }

    @Primary
    @Bean(name = "mssqlEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean mssqlEntityManagerFactory() {
        log.info("Initializing SQL Server EntityManagerFactory with packages: exercise.bidashboardapi.entity");

        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(mssqlDataSource());
        em.setPackagesToScan("exercise.bidashboardapi.entity");
        em.setPersistenceUnitName("mssql");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setShowSql(true);
        vendorAdapter.setGenerateDdl(true);
        em.setJpaVendorAdapter(vendorAdapter);

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", "update");
        properties.put("hibernate.dialect", "org.hibernate.dialect.SQLServerDialect");
        properties.put("hibernate.format_sql", "true");
        em.setJpaPropertyMap(properties);

        log.info("SQL Server EntityManagerFactory configured with Hibernate dialect: SQLServerDialect");
        return em;
    }

    @Primary
    @Bean(name = "mssqlTransactionManager")
    public PlatformTransactionManager mssqlTransactionManager(EntityManagerFactory entityManagerFactory) {
        log.info("Initializing SQL Server TransactionManager");
        return new JpaTransactionManager(entityManagerFactory);
    }

    private String maskUrl(String jdbcUrl) {
        // Mask sensitive parts of URL (password if present)
        if (jdbcUrl == null) return "null";
        // Extract just the server and database name for logging
        if (jdbcUrl.contains("jdbc:sqlserver://")) {
            String[] parts = jdbcUrl.split(";");
            return parts.length > 0 ? parts[0] : jdbcUrl;
        }
        return jdbcUrl;
    }
}
