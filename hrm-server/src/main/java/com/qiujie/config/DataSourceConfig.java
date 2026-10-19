package com.qiujie.config;

import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * 数据源配置。
 * Flowable 与业务共用 master 数据源，引擎 SQL 通过表前缀访问 hrm_flowable.ACT_*，
 * 两个库的表因此落在同一连接、同一本地事务里。
 */
@Configuration
public class DataSourceConfig {

    @Primary
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.master")
    public DataSource masterDataSource() {
        return DataSourceBuilder.create().build();
    }

    /**
     * 为主数据源显式配置事务管理器，确保 @Transactional 正确提交。
     * 多数据源场景下 Spring Boot 可能不确定用哪个 TransactionManager。
     */
    @Primary
    @Bean
    public PlatformTransactionManager transactionManager(
            @Qualifier("masterDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }


    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.kb")
    public DataSource kbDataSource() {
        return DataSourceBuilder.create().build();
    }

    /**
     * Flowable 表仍存放在 hrm_flowable：运行时 SQL 通过表前缀生成 hrm_flowable.ACT_*，
     * 与业务表共用 master 数据源和同一个本地事务。
     * MySQL 下 database-schema 不会给引擎的建表/升级脚本加前缀，故 schema-update 必须关闭，
     * hrm_flowable 的表结构由 db/mysql/hrm_flowable.sql 初始化。
     */
    @Bean
    public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> engineConfigurationConfigurer(
            @Value("${flowable.table-prefix}") String tablePrefix) {
        return config -> config.setDatabaseTablePrefix(tablePrefix);
    }
}
