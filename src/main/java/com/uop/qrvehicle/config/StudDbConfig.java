package com.uop.qrvehicle.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Multi-Database Configuration
 * 
 * Because we define a secondary DataSource bean, Spring Boot's auto-configuration
 * backs off from creating the primary one. So we must explicitly define BOTH.
 * 
 * Primary: vehicle_qr_db on localhost (used by JPA/Hibernate)
 * Secondary: studdb on dbwala.pdn.ac.lk (accessed via JdbcTemplate only)
 */
@Configuration
public class StudDbConfig {

    // ========== PRIMARY DATASOURCE (vehicle_qr_db) ==========

    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties primaryDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean
    public DataSource dataSource(DataSourceProperties primaryDataSourceProperties) {
        return primaryDataSourceProperties.initializeDataSourceBuilder().build();
    }

    // ========== SECONDARY DATASOURCE (studdb) ==========

    @Bean
    @ConfigurationProperties("app.datasource.studdb")
    public DataSourceProperties studDbDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "studDbDataSource")
    public DataSource studDbDataSource(
            @Qualifier("studDbDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean(name = "studDbJdbcTemplate")
    public JdbcTemplate studDbJdbcTemplate(@Qualifier("studDbDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
