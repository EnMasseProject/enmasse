/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.jdbc.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLClient;

@JsonInclude(value = Include.NON_NULL)
public class JdbcProperties {

    private static final Logger log = LoggerFactory.getLogger(JdbcProperties.class);

    private String url;
    private String driverClass;
    private String username;
    private String password;
    private Integer maximumPoolSize;

    private String tableName;

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setDriverClass(String driverClassName) {
        this.driverClass = driverClassName;
    }

    public String getDriverClass() {
        return driverClass;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public void setMaximumPoolSize(Integer maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    }

    public Integer getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public static SQLClient dataSource(final Vertx vertx, final JdbcProperties dataSourceProperties) {

        /*
         * In the following lines we explicitly set the "provider_class" and **must not** use
         * the existing constant for that.
         *
         * The reason for this is, that the downstream version changes the value of the constant to
         * use Agroal as the connection pool. As C3P0 is the upstream default for Vert.x and Agroal
         * isn't even mentioned upstream, we explicitly set C3P0 here. Without using the (not so) constant.
         */

        final JsonObject config = new JsonObject()
                // set default explicitly: see comment above
                .put("provider_class", "io.vertx.ext.jdbc.spi.impl.C3P0DataSourceProvider")
                .put("url", dataSourceProperties.getUrl())
                .put("user", dataSourceProperties.getUsername())
                .put("password", dataSourceProperties.getPassword());

        if (dataSourceProperties.getDriverClass() != null) {
            config.put("driver_class", dataSourceProperties.getDriverClass());
        }
        if (dataSourceProperties.getMaximumPoolSize() != null) {
            config.put("max_pool_size", dataSourceProperties.getMaximumPoolSize());
        }

        if (log.isInfoEnabled()) {
            var logConfig = config.copy();
            logConfig.remove("password");
            log.info("Creating new SQL client: {} - table: {}", logConfig, dataSourceProperties.getTableName());
        }


        return JDBCClient.createShared(vertx, config);

    }

}
