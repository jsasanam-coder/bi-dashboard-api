package exercise.bidashboardapi.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import javax.sql.DataSource;

@Configuration
@Slf4j
public class SnowflakeConfig {

    @Value("${snowflake.url}")
    private String url;

    @Value("${snowflake.username}")
    private String username;

    @Value("${snowflake.password}")
    private String password;

    @Value("${snowflake.warehouse}")
    private String warehouse;

    @Value("${snowflake.database}")
    private String database;

    @Value("${snowflake.schema}")
    private String schema;

    @Value("${snowflake.jdbc-query-result-format}")
    private String jdbcQueryResultFormat;

    @Bean(name = "snowflakeDataSource")
    @Lazy
    public DataSource snowflakeDataSource() {
        log.info("Initializing Snowflake DataSource: url={}, database={}, schema={}, warehouse={}, username={}",
                url, database, schema, warehouse, username);
        log.info("Snowflake pool settings: maxPoolSize=5, minIdle=2, connectionTimeout=30s, idleTimeout=600s");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("net.snowflake.client.jdbc.SnowflakeDriver");

        // Connection pool settings
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(0);
        config.setConnectionTimeout(10000);
        config.setIdleTimeout(600000);
        config.setInitializationFailTimeout(-1);

        // Snowflake properties
        config.addDataSourceProperty("warehouse", warehouse);
        config.addDataSourceProperty("db", database);
        config.addDataSourceProperty("schema", schema);
        config.addDataSourceProperty("jdbc_query_result_format", jdbcQueryResultFormat);

        HikariDataSource dataSource = new HikariDataSource(config);
        log.info("Snowflake DataSource initialized successfully");
        return dataSource;
    }
}
